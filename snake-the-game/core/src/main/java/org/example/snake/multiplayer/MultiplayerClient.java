package org.example.snake.multiplayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MultiplayerClient implements WebSocket.Listener {
    private final URI serverUri;
    private final String playerName;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Queue<Event> events = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final StringBuilder partial = new StringBuilder();

    private volatile WebSocket socket;

    public MultiplayerClient(String serverUri, String playerName) {
        this.serverUri = URI.create(serverUri);
        this.playerName = playerName == null || playerName.isBlank() ? "Player" : playerName.trim();
    }

    public void connect() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(serverUri, this)
                .whenComplete((ws, err) -> {
                    if (err != null) {
                        events.add(Event.error("Failed to open WebSocket", err));
                        return;
                    }
                    socket = ws;
                    events.add(Event.connected());
                    sendJoin();
                });
    }

    public void sendDirection(Direction direction) {
        WebSocket ws = socket;
        if (ws == null || direction == null) {
            return;
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "direction");
        node.put("direction", direction.name());
        sendAsync(node);
    }

    public void close() {
        WebSocket ws = socket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    public Event pollEvent() {
        return events.poll();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        partial.append(data);
        if (last) {
            String payload = partial.toString();
            partial.setLength(0);
            handleMessage(payload);
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    private void handleMessage(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            String type = Optional.ofNullable(node.path("type").asText(null)).orElse("");
            switch (type) {
                case "welcome" -> events.add(Event.welcome(mapper.treeToValue(node, Welcome.class)));
                case "state" -> events.add(Event.state(mapper.treeToValue(node, Snapshot.class)));
                case "pong" -> { /* ignore */ }
                default -> events.add(Event.info("Unknown message: " + type));
            }
        } catch (JsonProcessingException ex) {
            events.add(Event.error("Failed to parse message", ex));
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        events.add(Event.closed(statusCode, reason));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        events.add(Event.error("WebSocket error", error));
    }

    private void sendJoin() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "join");
        node.put("name", playerName);
        sendAsync(node);
    }

    private void sendAsync(ObjectNode node) {
        WebSocket ws = socket;
        if (ws == null) {
            return;
        }
        try {
            String json = mapper.writeValueAsString(node);
            ws.sendText(json, true);
        } catch (JsonProcessingException ignored) {
        }
    }

    public enum Direction {
        UP, DOWN, LEFT, RIGHT;

        public static Direction fromInput(String value) {
            if (value == null) return null;
            try {
                return Direction.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    public sealed interface Event permits Event.Connected, Event.State, Event.WelcomeEvent, Event.Error, Event.Info, Event.Closed {
        static Event connected() { return new Connected(); }
        static Event state(Snapshot snapshot) { return new State(snapshot); }
        static Event welcome(Welcome welcome) { return new WelcomeEvent(welcome); }
        static Event error(String message, Throwable throwable) { return new Error(message, throwable); }
        static Event info(String message) { return new Info(message); }
        static Event closed(int status, String reason) { return new Closed(status, reason); }

        record Connected() implements Event {}
        record State(Snapshot snapshot) implements Event {}
        record WelcomeEvent(Welcome welcome) implements Event {}
        record Error(String message, Throwable cause) implements Event {}
        record Info(String message) implements Event {}
        record Closed(int status, String reason) implements Event {}
    }

    public static final class Snapshot {
        public String type;
        public long tick;
        public java.util.List<Player> players;
        public java.util.List<Point> foods;
        public java.util.List<LeaderboardEntry> leaderboard;
    }

    public static final class Player {
        public String id;
        public String name;
        public String color;
        public boolean alive;
        public int score;
        public java.util.List<Point> segments;
        public boolean bot; // optional, default false
    }

    public static final class Point {
        public int x;
        public int y;
    }

    public static final class LeaderboardEntry {
        public String name;
        public int score;
    }

    public static final class Welcome {
        public String type;
        public String id;
        public String color;
        public int cols;
        public int rows;
        public long tickMillis;
    }
}
