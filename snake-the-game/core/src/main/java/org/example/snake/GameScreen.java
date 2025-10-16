package org.example.snake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.*;

public class GameScreen extends ScreenAdapter {
    public static final int DEFAULT_CELL_SIZE = 20;
    public static final int GRID_COLS = 30;
    public static final int GRID_ROWS = 20;

    private final MainGame game;
    private final Level level;

    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    // Dynamic layout
    private int cellSize;
    private int originX;
    private int originY;
    private int worldWidth;
    private int worldHeight;

    private Deque<Point> snake;
    private Set<Point> snakeSet;
    private Direction dir = Direction.RIGHT;
    private Direction nextDir = Direction.RIGHT;
    private Point food;
    private Random rng;

    private float accumulator = 0f;
    private boolean gameOver = false;
    private int score = 0;

    // UI overlay for game over
    private Stage uiStage;
    private Skin uiSkin;
    private Texture uiBtnUp, uiBtnOver, uiBtnDown;
    private Table gameOverTable;
    private Label scoreLabel;

    public GameScreen(MainGame game, Level level) {
        this.game = game;
        this.level = level;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        shapes = new ShapeRenderer();
        shapes.setAutoShapeType(true);
        batch = game.batch;
        font = game.font;
        rng = new Random();
        // Initialize layout based on current window
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        setupUi();
        resetGame();
    }

    private void setupUi() {
        uiStage = new Stage(new com.badlogic.gdx.utils.viewport.ScreenViewport(), batch);
        uiSkin = new Skin();
        uiSkin.add("default-font", font, BitmapFont.class);

        uiBtnUp = makeRoundRectTex(240, 48, new Color(0.2f, 0.6f, 0.85f, 1f));
        uiBtnOver = makeRoundRectTex(240, 48, new Color(0.25f, 0.75f, 1.0f, 1f));
        uiBtnDown = makeRoundRectTex(240, 48, new Color(0.15f, 0.5f, 0.7f, 1f));

        Drawable up = new TextureRegionDrawable(uiBtnUp);
        Drawable over = new TextureRegionDrawable(uiBtnOver);
        Drawable down = new TextureRegionDrawable(uiBtnDown);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, font);
        style.over = over;
        uiSkin.add("default", style);

        // Game Over panel
        gameOverTable = new Table();
        gameOverTable.setFillParent(true);
        gameOverTable.center();

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label title = new Label("Game Over", labelStyle);
        scoreLabel = new Label("Score: 0", labelStyle);

        TextButton restart = new TextButton("Restart", uiSkin);
        TextButton toMenu = new TextButton("Menu", uiSkin);

        restart.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                resetGame();
                hideGameOver();
            }
        });
        toMenu.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                HighScores.submit(score);
                game.showMenu();
            }
        });

        gameOverTable.add(title).padBottom(10).row();
        gameOverTable.add(scoreLabel).padBottom(16).row();
        gameOverTable.add(restart).width(240).height(48).pad(6).row();
        gameOverTable.add(toMenu).width(240).height(48).pad(6).row();
        gameOverTable.setVisible(false);
        uiStage.addActor(gameOverTable);

        // Route input to stage (keyboard polling still works via Gdx.input)
        Gdx.input.setInputProcessor(uiStage);
    }

    private void resetGame() {
        snake = new ArrayDeque<>();
        snakeSet = new HashSet<>();
        int startX = GRID_COLS / 2;
        int startY = GRID_ROWS / 2;
        for (int i = 0; i < 4; i++) {
            Point p = new Point(startX - i, startY);
            snake.addLast(p);
            snakeSet.add(p);
        }
        dir = Direction.RIGHT;
        nextDir = Direction.RIGHT;
        score = 0;
        gameOver = false;
        spawnFood();
        accumulator = 0f;
    }

    private void spawnFood() {
        while (true) {
            int x = rng.nextInt(GRID_COLS);
            int y = rng.nextInt(GRID_ROWS);
            Point p = new Point(x, y);
            if (!snakeSet.contains(p)) {
                food = p;
                return;
            }
        }
    }

    @Override
    public void render(float delta) {
        handleInput();

        if (!gameOver) {
            updateGame(delta);
        } else {
            // Update UI and wait for button actions; no immediate return
        }

        clearAndSetupProjection();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawBackgroundGrid();
        drawFood();
        drawSnake();
        shapes.end();

        batch.begin();
        drawHud();
        batch.end();

        // UI overlay
        uiStage.act(delta);
        uiStage.draw();
    }

    private void updateGame(float delta) {
        accumulator += delta;
        tickAccumulator();
    }

    private boolean handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            HighScores.submit(score);
            game.showMenu();
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }
        return false;
    }

    private void tickAccumulator() {
        while (accumulator >= level.stepTime) {
            step();
            accumulator -= level.stepTime;
        }
    }

    private void clearAndSetupProjection() {
        Gdx.gl.glClearColor(0.07f, 0.07f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
    }

    private void drawBackgroundGrid() {
        shapes.setColor(new Color(0.12f, 0.12f, 0.17f, 1f));
        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = 0; x < GRID_COLS; x++) {
                if (((x + y) & 1) == 0) {
                    shapes.rect(originX + x * cellSize, originY + y * cellSize, cellSize, cellSize);
                }
            }
        }
    }

    private void drawFood() {
        if (food == null) return;
        shapes.setColor(Color.SCARLET);
        shapes.rect(originX + food.x * cellSize, originY + food.y * cellSize, cellSize, cellSize);
    }

    private void drawSnake() {
        int i = 0;
        for (Point p : snake) {
            if (i == 0) shapes.setColor(Color.LIME);
            else shapes.setColor(new Color(0.2f, 0.8f, 0.2f, 1f));

            shapes.rect(originX + p.x * cellSize, originY + p.y * cellSize, cellSize, cellSize);
            i++;
        }
    }

    private void drawHud() {
        String text = "Score: " + score + (gameOver ? "  |  Game Over! ENTER=Menu, R=Restart" : "");
        font.draw(batch, text, originX + 8, originY + worldHeight - 8);
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            if (dir != Direction.DOWN) nextDir = Direction.UP;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            if (dir != Direction.UP) nextDir = Direction.DOWN;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            if (dir != Direction.RIGHT) nextDir = Direction.LEFT;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            if (dir != Direction.LEFT) nextDir = Direction.RIGHT;
        }
    }

    private void step() {
        dir = nextDir;
        Point head = snake.peekFirst();
        int nx = head.x + dir.dx;
        int ny = head.y + dir.dy;
        if (nx < 0 || ny < 0 || nx >= GRID_COLS || ny >= GRID_ROWS) {
            triggerGameOver();
            return;
        }
        Point newHead = new Point(nx, ny);
        if (snakeSet.contains(newHead)) {
            triggerGameOver();
            return;
        }
        snake.addFirst(newHead);
        snakeSet.add(newHead);
        if (food != null && newHead.equals(food)) {
            score += 10;
            spawnFood();
        } else {
            Point tail = snake.removeLast();
            snakeSet.remove(tail);
        }
    }

    @Override
    public void resize(int width, int height) {
        // Determine cell size to fit grid completely into window without scaling
        cellSize = Math.max(1, Math.min(width / GRID_COLS, height / GRID_ROWS));
        worldWidth = cellSize * GRID_COLS;
        worldHeight = cellSize * GRID_ROWS;
        originX = (width - worldWidth) / 2;
        originY = (height - worldHeight) / 2;

        // Use window size as camera viewport to avoid post-scaling (crisp rendering)
        camera.setToOrtho(false, width, height);

        if (uiStage != null) uiStage.getViewport().update(width, height, true);
    }

    private void triggerGameOver() {
        gameOver = true;
        if (scoreLabel != null) {
            scoreLabel.setText("Score: " + score);
        }
        if (gameOverTable != null) gameOverTable.setVisible(true);
    }

    private void hideGameOver() {
        gameOver = false;
        if (gameOverTable != null) gameOverTable.setVisible(false);
    }

    @Override
    public void dispose() {
        if (shapes != null) shapes.dispose();
        if (uiStage != null) uiStage.dispose();
        if (uiSkin != null) uiSkin.dispose();
        if (uiBtnUp != null) uiBtnUp.dispose();
        if (uiBtnOver != null) uiBtnOver.dispose();
        if (uiBtnDown != null) uiBtnDown.dispose();
    }

    private Texture makeRoundRectTex(int width, int height, Color color) {
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        int r = 10;
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
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private enum Direction {
        UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1, 0);
        final int dx, dy;
        Direction(int dx, int dy) { this.dx = dx; this.dy = dy; }
    }

    private static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Point point = (Point) o; return x == point.x && y == point.y; }
        @Override public int hashCode() { return (x * 73856093) ^ (y * 19349663); }
    }
}
