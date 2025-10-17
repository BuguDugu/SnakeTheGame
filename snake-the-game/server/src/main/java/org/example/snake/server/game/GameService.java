package org.example.snake.server.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class GameService {
    private static final Logger log = LoggerFactory.getLogger(GameService.class);
    private static final int DEFAULT_WORLD_COLS = 120;
    private static final int DEFAULT_WORLD_ROWS = 120;
    private static final int FOOD_TARGET = 60;
    private static final int INITIAL_SNAKE_LENGTH = 6;
    private static final int RESPAWN_DELAY_TICKS = 15;
    private static final List<String> COLORS = List.of(
            "#4CAF50", "#FF7043", "#9575CD", "#26C6DA", "#EC407A",
            "#FFCA28", "#66BB6A", "#8D6E63", "#42A5F5", "#AB47BC"
    );

    private final ConcurrentMap<String, PlayerSession> sessions = new ConcurrentHashMap<>();
    private final Set<Point> foods = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper;
    private final StringRedisTemplate redisTemplate;
    private final AtomicLong tickCounter = new AtomicLong();
    private final Random random = new Random();
    private final AtomicInteger colorIdx = new AtomicInteger();

    private final int worldCols;
    private final int worldRows;
    private final long tickMillis;
    private final String leaderboardKey;

    public GameService(ObjectMapper mapper,
                       StringRedisTemplate redisTemplate,
                       @Value("${game.world.cols:" + DEFAULT_WORLD_COLS + "}") int worldCols,
                       @Value("${game.world.rows:" + DEFAULT_WORLD_ROWS + "}") int worldRows,
                       @Value("${game.tick-millis:120}") long tickMillis,
                       @Value("${game.redis.leaderboard-key:snake:leaderboard}") String leaderboardKey) {
        this.mapper = mapper;
        this.redisTemplate = redisTemplate;
        this.worldCols = Math.max(40, worldCols);
        this.worldRows = Math.max(40, worldRows);
        this.tickMillis = Math.max(80, tickMillis);
        this.leaderboardKey = leaderboardKey;
    }

    public void register(WebSocketSession session) {
        session.setTextMessageSizeLimit(64 * 1024);
        sessions.put(session.getId(), new PlayerSession(session));
    }

    public void unregister(WebSocketSession session) {
        PlayerSession removed = sessions.remove(session.getId());
        if (removed != null && removed.player() != null) {
            removeFromLeaderboard(removed.player().name());
        }
        safeClose(session);
    }

    public void handleMessage(WebSocketSession session, JsonNode payload) throws IOException {
        PlayerSession ps = sessions.get(session.getId());
        if (ps == null) {
            return;
        }
        String type = Optional.ofNullable(payload.path("type").asText(null)).orElse("");
        switch (type) {
            case "join" -> handleJoin(ps, payload);
            case "direction" -> handleDirection(ps, payload.path("direction").asText(""));
            case "ping" -> sendPong(ps);
            default -> { }
        }
    }

    private void handleJoin(PlayerSession ps, JsonNode payload) throws JsonProcessingException {
        String name = payload.path("name").asText("").trim();
        if (name.isEmpty()) {
            name = "Player";
        }
        if (ps.player() != null) {
            ps.player().name(name);
            recordScore(ps.player().name(), ps.player().score());
            return;
        }
        String id = UUID.randomUUID().toString();
        String color = COLORS.get(Math.floorMod(colorIdx.getAndIncrement(), COLORS.size()));
        PlayerState player = new PlayerState(id, name, color);
        spawnPlayer(player);
        ps.player(player);
        ps.pendingDirection(Direction.RIGHT);
        ps.lastDirection(Direction.RIGHT);
        recordScore(name, 0);
        sendWelcome(ps, player);
    }

    private void sendWelcome(PlayerSession ps, PlayerState player) throws JsonProcessingException {
        WelcomeMessage welcome = new WelcomeMessage(
                "welcome",
                player.id(),
                player.color(),
                worldCols,
                worldRows,
                tickMillis
        );
        send(ps.session(), welcome);
    }

    private void handleDirection(PlayerSession ps, String directionText) {
        Direction dir = Direction.from(directionText);
        if (dir == null) {
            return;
        }
        Direction lastDir = ps.lastDirection();
        if (lastDir != null && dir.isOpposite(lastDir)) {
            return;
        }
        ps.pendingDirection(dir);
    }

    private void sendPong(PlayerSession ps) throws JsonProcessingException {
        send(ps.session(), Map.of("type", "pong", "now", System.currentTimeMillis()));
    }

    @Scheduled(fixedRateString = "${game.tick-millis:120}")
    public void gameLoop() {
        long tick = tickCounter.incrementAndGet();
        ensureFood();
        updatePlayers(tick);
        broadcastState(tick);
    }

    private void ensureFood() {
        while (foods.size() < FOOD_TARGET) {
            Point candidate = new Point(random.nextInt(worldCols), random.nextInt(worldRows));
            if (isOccupied(candidate)) {
                continue;
            }
            foods.add(candidate);
        }
    }

    private boolean isOccupied(Point pt) {
        for (PlayerSession ps : sessions.values()) {
            PlayerState player = ps.player();
            if (player == null || !player.alive()) continue;
            if (player.body().contains(pt)) {
                return true;
            }
        }
        return foods.contains(pt);
    }

    private void updatePlayers(long tick) {
        Map<Point, PlayerState> occupied = new HashMap<>();
        for (PlayerSession ps : sessions.values()) {
            PlayerState p = ps.player();
            if (p == null) continue;
            if (!p.alive() && tick >= ps.respawnAt()) {
                spawnPlayer(p);
                p.alive(true);
                ps.pendingDirection(Direction.RIGHT);
                ps.lastDirection(Direction.RIGHT);
            }
            if (p.alive()) {
                for (Point seg : p.body()) {
                    occupied.put(seg, p);
                }
            }
        }

        for (PlayerSession ps : sessions.values()) {
            PlayerState player = ps.player();
            if (player == null || !player.alive()) {
                continue;
            }
            Direction next = Optional.ofNullable(ps.pendingDirection()).orElse(ps.lastDirection());
            if (next == null) {
                next = Direction.RIGHT;
            }
            Point head = player.body().peekFirst();
            Point newHead = head.translate(next.dx(), next.dy());
            ps.pendingDirection(null);
            if (!withinBounds(newHead) || collides(player, newHead, occupied)) {
                kill(player);
                ps.respawnAt(tick + RESPAWN_DELAY_TICKS);
                continue;
            }
            boolean grew = false;
            if (foods.remove(newHead)) {
                player.score(player.score() + 10);
                recordScore(player.name(), player.score());
                grew = true;
            }
            player.body().addFirst(newHead);
            occupied.put(newHead, player);
            if (!grew) {
                Point tail = player.body().removeLast();
                occupied.remove(tail);
            }
            ps.lastDirection(next);
        }
    }

    private boolean collides(PlayerState player, Point newHead, Map<Point, PlayerState> occupied) {
        PlayerState hit = occupied.get(newHead);
        if (hit == null) {
            return false;
        }
        if (hit == player) {
            // Allow moving into the tile that will be freed by our tail unless we are growing
            Point tail = player.body().peekLast();
            if (tail != null && tail.equals(newHead)) {
                return false;
            }
        }
        kill(hit);
        return true;
    }

    private void kill(PlayerState player) {
        player.alive(false);
        player.body().clear();
    }

    private boolean withinBounds(Point p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < worldCols && p.y() < worldRows;
    }

    private void spawnPlayer(PlayerState player) {
        Deque<Point> body = player.body();
        body.clear();
        int x = random.nextInt(worldCols - 20) + 10;
        int y = random.nextInt(worldRows - 20) + 10;
        Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
        int dx = -dir.dx();
        int dy = -dir.dy();
        for (int i = 0; i < INITIAL_SNAKE_LENGTH; i++) {
            body.addLast(new Point(x + i * dx, y + i * dy));
        }
        player.alive(true);
    }

    private void broadcastState(long tick) {
        List<PlayerPayload> players = new ArrayList<>();
        for (PlayerSession ps : sessions.values()) {
            PlayerState player = ps.player();
            if (player == null) continue;
            List<PointPayload> segments = player.alive()
                    ? player.body().stream().map(p -> new PointPayload(p.x(), p.y())).toList()
                    : List.of();
            players.add(new PlayerPayload(player.id(), player.name(), player.color(), player.alive(), player.score(), segments));
        }
        List<PointPayload> foodPayload = foods.stream().map(p -> new PointPayload(p.x(), p.y())).toList();
        List<LeaderboardEntry> leaderboard = fetchLeaderboard();
        Snapshot snapshot = new Snapshot("state", tick, players, foodPayload, leaderboard);
        sessions.values().forEach(ps -> sendSilently(ps.session(), snapshot));
    }

    private List<LeaderboardEntry> fetchLeaderboard() {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(leaderboardKey, 0, 9);
            if (tuples == null) {
                return List.of();
            }
            List<LeaderboardEntry> entries = new ArrayList<>();
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() == null || tuple.getScore() == null) continue;
                entries.add(new LeaderboardEntry(tuple.getValue(), tuple.getScore().intValue()));
            }
            return entries;
        } catch (DataAccessException ex) {
            log.warn("Failed to fetch leaderboard from Redis", ex);
            return List.of();
        }
    }

    private void recordScore(String name, double score) {
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForZSet().add(leaderboardKey, name, score);
        } catch (DataAccessException ex) {
            log.warn("Failed to record score for {}", name, ex);
        }
    }

    private void removeFromLeaderboard(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            redisTemplate.opsForZSet().remove(leaderboardKey, name);
        } catch (DataAccessException ex) {
            log.warn("Failed to remove score for {}", name, ex);
        }
    }

    private void send(WebSocketSession session, Object payload) throws JsonProcessingException {
        String json = mapper.writeValueAsString(payload);
        sendText(session, json);
    }

    private void sendSilently(WebSocketSession session, Object payload) {
        try {
            send(session, payload);
        } catch (IOException ignored) {
            unregister(session);
        }
    }

    private void sendText(WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException ex) {
                unregister(session);
            }
        }
    }

    private void safeClose(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    private record Point(int x, int y) {
        Point translate(int dx, int dy) {
            return new Point(x + dx, y + dy);
        }
    }

    private enum Direction {
        UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        int dx() { return dx; }
        int dy() { return dy; }

        boolean isOpposite(Direction other) {
            return dx + other.dx == 0 && dy + other.dy == 0;
        }

        static Direction from(String value) {
            if (value == null) return null;
            try {
                return Direction.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private static final class PlayerState {
        private final String id;
        private String name;
        private final String color;
        private final Deque<Point> body = new ArrayDeque<>();
        private boolean alive;
        private int score;

        PlayerState(String id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        String id() { return id; }
        String name() { return name; }
        void name(String v) { this.name = v; }
        String color() { return color; }
        Deque<Point> body() { return body; }
        boolean alive() { return alive; }
        void alive(boolean v) { this.alive = v; }
        int score() { return score; }
        void score(int s) { this.score = s; }
    }

    private static final class PlayerSession {
        private final WebSocketSession session;
        private PlayerState player;
        private Direction pendingDirection;
        private Direction lastDirection;
        private long respawnAt;

        PlayerSession(WebSocketSession session) {
            this.session = session;
        }

        WebSocketSession session() { return session; }
        PlayerState player() { return player; }
        void player(PlayerState p) { this.player = p; }
        Direction pendingDirection() { return pendingDirection; }
        void pendingDirection(Direction d) { this.pendingDirection = d; }
        Direction lastDirection() { return lastDirection; }
        void lastDirection(Direction d) { this.lastDirection = d; }
        long respawnAt() { return respawnAt; }
        void respawnAt(long t) { this.respawnAt = t; }
    }

    private record PlayerPayload(String id, String name, String color, boolean alive, int score,
                                 List<PointPayload> segments) {
    }

    private record PointPayload(int x, int y) {
    }

    private record LeaderboardEntry(String name, int score) {
    }

    private record Snapshot(String type, long tick, List<PlayerPayload> players,
                            List<PointPayload> foods, List<LeaderboardEntry> leaderboard) {
    }

    private record WelcomeMessage(String type, String id, String color, int cols, int rows, long tickMillis) {
    }
}
