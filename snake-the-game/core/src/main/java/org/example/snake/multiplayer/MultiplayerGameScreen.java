package org.example.snake.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.List;
import java.util.Objects;
import org.example.snake.MainGame;
import org.example.snake.multiplayer.MultiplayerClient.Event;

public class MultiplayerGameScreen extends ScreenAdapter {
    private static final int DEFAULT_COLS = 120;
    private static final int DEFAULT_ROWS = 120;

    private final MainGame game;
    private final String playerName;
    private final String serverUri;

    private MultiplayerClient client;
    private MultiplayerClient.Snapshot snapshot;

    private OrthographicCamera camera;
    private ShapeRenderer shapes;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private Stage uiStage;
    private Skin uiSkin;
    private Texture buttonUpTex;
    private Texture buttonOverTex;
    private Texture buttonDownTex;
    private Label statusLabel;
    private Label leaderboardLabel;

    private int worldCols = DEFAULT_COLS;
    private int worldRows = DEFAULT_ROWS;
    private int cellSize = 24;
    private float worldWidth;
    private float worldHeight;

    private MultiplayerClient.Direction pendingDirection = MultiplayerClient.Direction.RIGHT;
    private String playerId;
    private String statusMessage = "Connecting";

    public MultiplayerGameScreen(MainGame game, String playerName, String serverUri) {
        this.game = game;
        this.playerName = playerName;
        this.serverUri = serverUri;
        this.batch = game.batch;
        this.font = game.font;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        uiStage = new Stage(new ScreenViewport(), batch);
        uiSkin = new Skin();
        uiSkin.add("default-font", font, BitmapFont.class);
        createUi();

        client = new MultiplayerClient(serverUri, playerName);
        client.connect();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                handleKey(keycode);
                return false;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void createUi() {
        buttonUpTex = makeRoundRectTex(200, 48, new Color(0.85f, 0.2f, 0.3f, 1f));
        buttonOverTex = makeRoundRectTex(200, 48, new Color(1.0f, 0.3f, 0.4f, 1f));
        buttonDownTex = makeRoundRectTex(200, 48, new Color(0.65f, 0.1f, 0.2f, 1f));
        Drawable up = new TextureRegionDrawable(buttonUpTex);
        Drawable over = new TextureRegionDrawable(buttonOverTex);
        Drawable down = new TextureRegionDrawable(buttonDownTex);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, font);
        style.over = over;
        uiSkin.add("default", style);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        statusLabel = new Label("Connecting...", labelStyle);
        leaderboardLabel = new Label("", labelStyle);

        Table root = new Table();
        root.setFillParent(true);
        root.top().left().pad(16);
        root.add(leaderboardLabel).left().expandX().top();
        root.row();
        root.add(statusLabel).left().padTop(12);

        Table buttonTable = new Table();
        buttonTable.setFillParent(true);
        buttonTable.bottom().right().pad(16);
        TextButton leave = new TextButton("Leave", uiSkin);
        leave.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                leaveGame();
            }
        });
        buttonTable.add(leave).width(200).height(48);

        uiStage.addActor(root);
        uiStage.addActor(buttonTable);
    }

    private void leaveGame() {
        if (client != null) {
            client.close();
        }
        game.showMenu();
    }

    private void handleKey(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            leaveGame();
            return;
        }
        MultiplayerClient.Direction newDir = switch (keycode) {
            case Input.Keys.UP, Input.Keys.W -> MultiplayerClient.Direction.UP;
            case Input.Keys.DOWN, Input.Keys.S -> MultiplayerClient.Direction.DOWN;
            case Input.Keys.LEFT, Input.Keys.A -> MultiplayerClient.Direction.LEFT;
            case Input.Keys.RIGHT, Input.Keys.D -> MultiplayerClient.Direction.RIGHT;
            default -> null;
        };
        if (newDir != null && newDir != pendingDirection) {
            pendingDirection = newDir;
            if (client != null) {
                client.sendDirection(newDir);
            }
        }
    }

    @Override
    public void render(float delta) {
        pollEvents();
        updateCameraTarget();

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawWorld();
        uiStage.act(delta);
        uiStage.draw();
    }

    private void pollEvents() {
        if (client == null) {
            return;
        }
        Event event;
        boolean leaderboardChanged = false;
        while ((event = client.pollEvent()) != null) {
            if (event instanceof Event.Connected) {
                statusMessage = "Connected, joining lobby";
            } else if (event instanceof Event.WelcomeEvent welcomeEvent) {
                MultiplayerClient.Welcome welcomeMsg = welcomeEvent.welcome();
                playerId = welcomeMsg.id;
                worldCols = welcomeMsg.cols;
                worldRows = welcomeMsg.rows;
                statusMessage = "Waiting for game state";
                updateWorldSize();
            } else if (event instanceof Event.State stateEvent) {
                snapshot = stateEvent.snapshot();
                leaderboardChanged = true;
                statusMessage = "Playing as " + playerName;
            } else if (event instanceof Event.Error error) {
                statusMessage = error.message();
            } else if (event instanceof Event.Closed closedEvent) {
                statusMessage = "Connection closed" + (closedEvent.reason() != null ? (": " + closedEvent.reason()) : "");
            } else if (event instanceof Event.Info info) {
                statusMessage = info.message();
            }
        }
        statusLabel.setText(statusMessage);
        if (leaderboardChanged) {
            leaderboardLabel.setText(buildLeaderboardText());
        }
    }

    private void updateCameraTarget() {
        MultiplayerClient.Player player = findSelf();
        if (player == null || player.segments == null || player.segments.isEmpty()) {
            return;
        }
        MultiplayerClient.Point head = player.segments.get(0);
        float targetX = head.x * cellSize + cellSize / 2f;
        float targetY = head.y * cellSize + cellSize / 2f;
        float halfW = camera.viewportWidth / 2f;
        float halfH = camera.viewportHeight / 2f;
        targetX = MathUtils.clamp(targetX, halfW, worldWidth - halfW);
        targetY = MathUtils.clamp(targetY, halfH, worldHeight - halfH);
        camera.position.set(targetX, targetY, 0f);
    }

    private void drawWorld() {
        if (snapshot == null) {
            return;
        }
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawGrid();
        drawFoods();
        drawPlayers();
        shapes.end();
    }

    private void drawGrid() {
        int minCol = Math.max(0, (int) ((camera.position.x - camera.viewportWidth / 2f) / cellSize) - 1);
        int maxCol = Math.min(worldCols - 1, (int) ((camera.position.x + camera.viewportWidth / 2f) / cellSize) + 1);
        int minRow = Math.max(0, (int) ((camera.position.y - camera.viewportHeight / 2f) / cellSize) - 1);
        int maxRow = Math.min(worldRows - 1, (int) ((camera.position.y + camera.viewportHeight / 2f) / cellSize) + 1);
        Color even = new Color(0.1f, 0.12f, 0.16f, 1f);
        Color odd = new Color(0.09f, 0.1f, 0.14f, 1f);
        for (int y = minRow; y <= maxRow; y++) {
            for (int x = minCol; x <= maxCol; x++) {
                shapes.setColor(((x + y) & 1) == 0 ? even : odd);
                shapes.rect(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }
    }

    private void drawFoods() {
        if (snapshot.foods == null) return;
        shapes.setColor(Color.SCARLET);
        float radius = cellSize * 0.35f;
        for (MultiplayerClient.Point f : snapshot.foods) {
            float cx = f.x * cellSize + cellSize / 2f;
            float cy = f.y * cellSize + cellSize / 2f;
            shapes.circle(cx, cy, radius, 12);
        }
    }

    private void drawPlayers() {
        if (snapshot.players == null) return;
        for (MultiplayerClient.Player player : snapshot.players) {
            if (player.segments == null || player.segments.isEmpty()) {
                continue;
            }
            Color color = parseColor(player.color, Color.GREEN);
            boolean isSelf = Objects.equals(player.id, playerId);
            float bodyRadius = cellSize * 0.4f;
            shapes.setColor(color);
            MultiplayerClient.Point prev = null;
            for (MultiplayerClient.Point point : player.segments) {
                float cx = point.x * cellSize + cellSize / 2f;
                float cy = point.y * cellSize + cellSize / 2f;
                shapes.circle(cx, cy, bodyRadius, 20);
                if (prev != null) {
                    float px = prev.x * cellSize + cellSize / 2f;
                    float py = prev.y * cellSize + cellSize / 2f;
                    shapes.rectLine(px, py, cx, cy, bodyRadius * 2f);
                }
                prev = point;
            }
            if (isSelf) {
                shapes.setColor(Color.WHITE);
                MultiplayerClient.Point head = player.segments.get(0);
                float hx = head.x * cellSize + cellSize / 2f;
                float hy = head.y * cellSize + cellSize / 2f;
                shapes.circle(hx, hy, bodyRadius * 0.55f, 16);
            }
        }
    }

    private MultiplayerClient.Player findSelf() {
        if (snapshot == null || snapshot.players == null || playerId == null) {
            return null;
        }
        for (MultiplayerClient.Player player : snapshot.players) {
            if (Objects.equals(player.id, playerId)) {
                return player;
            }
        }
        return null;
    }

    private String buildLeaderboardText() {
        if (snapshot == null || snapshot.leaderboard == null || snapshot.leaderboard.isEmpty()) {
            return "Leaderboard\n(no scores yet)";
        }
        StringBuilder sb = new StringBuilder("Leaderboard\n");
        int rank = 1;
        for (MultiplayerClient.LeaderboardEntry entry : snapshot.leaderboard) {
            sb.append(rank++).append(". ").append(entry.name);
            sb.append(" - ").append(entry.score).append('\n');
        }
        return sb.toString();
    }

    private Color parseColor(String hex, Color fallback) {
        try {
            return hex != null ? Color.valueOf(hex) : fallback;
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    @Override
    public void resize(int width, int height) {
        updateCellSize(width, height);
        camera.setToOrtho(false, width, height);
        updateWorldSize();
        if (uiStage != null) {
            uiStage.getViewport().update(width, height, true);
        }
    }

    private void updateCellSize(int width, int height) {
        int targetColsOnScreen = 30;
        int targetRowsOnScreen = 18;
        cellSize = Math.max(12, Math.min(width / targetColsOnScreen, height / targetRowsOnScreen));
    }

    private void updateWorldSize() {
        worldWidth = worldCols * cellSize;
        worldHeight = worldRows * cellSize;
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.close();
        }
        if (shapes != null) shapes.dispose();
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
        if (buttonUpTex != null) buttonUpTex.dispose();
        if (buttonOverTex != null) buttonOverTex.dispose();
        if (buttonDownTex != null) buttonDownTex.dispose();
    }

    private Texture makeRoundRectTex(int width, int height, Color color) {
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        int r = 12;
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        pm.setColor(color);
        pm.fillRectangle(r, 0, width - 2 * r, height);
        pm.fillRectangle(0, r, width, height - 2 * r);
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy <= r * r) {
                    pm.drawPixel(r + dx, r + dy);
                    pm.drawPixel(width - r - 1 + dx, r + dy);
                    pm.drawPixel(r + dx, height - r - 1 + dy);
                    pm.drawPixel(width - r - 1 + dx, height - r - 1 + dy);
                }
            }
        }
        Texture texture = new Texture(pm);
        pm.dispose();
        return texture;
    }
}
