package org.example.snake.game;

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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.*;

import org.example.snake.HighScores;
import org.example.snake.MainGame;

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

    private int cellSize;
    private int originX;
    private int originY;
    private int worldWidth;
    private int worldHeight;

    private Deque<Point> snake;
    private Deque<Point> prevSnake;
    private Set<Point> snakeSet;
    private Direction dir = Direction.RIGHT;
    private Direction nextDir = Direction.RIGHT;
    private Point food;
    private Random rng;

    private float accumulator = 0f;
    private float lastStepTime = Level.LEVEL_1.stepTime;
    private float elapsedTime = 0f;
    // Reusable buffers to reduce per-frame allocations
    private float[] currX, currY, prevX, prevY, tanX, tanY, ptsX, ptsY;
    private boolean gameOver = false;
    private int score = 0;

    // Reusable buffers to minimize GC during rendering
    private java.util.ArrayList<Vector2> tmpCurr = new java.util.ArrayList<>();
    private java.util.ArrayList<Vector2> tmpPrev = new java.util.ArrayList<>();
    private java.util.ArrayList<Vector2> tmpPts = new java.util.ArrayList<>();
    private Vector2[] tmpTangents;

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
        prevSnake = copySnake(snake);
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
        }

        clearAndSetupProjection();

        elapsedTime += delta;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        SnakeRenderer.drawBackgroundGrid(shapes, originX, originY, cellSize, GRID_COLS, GRID_ROWS);
        SnakeRenderer.drawFood(shapes, originX, originY, cellSize, food);
        float alpha = lastStepTime > 0f ? MathUtils.clamp(accumulator / lastStepTime, 0f, 1f) : 1f;
        SnakeRenderer.drawSnakeSausage(shapes, originX, originY, cellSize, snake, prevSnake, alpha, elapsedTime);
        shapes.end();

        batch.begin();
        SnakeRenderer.drawHud(batch, font, originX, originY + worldHeight, score, gameOver);
        batch.end();

        uiStage.act(delta);
        uiStage.draw();
    }

    private void updateGame(float delta) {
        accumulator += delta;
        float stepTime = getEffectiveStepTime();
        lastStepTime = stepTime;
        tickAccumulator(stepTime);
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

    private void tickAccumulator(float stepTime) {
        while (accumulator >= stepTime) {
            prevSnake = copySnake(snake);
            step();
            accumulator -= stepTime;
        }
    }

    private float getEffectiveStepTime() {
        float st = level.stepTime;
        if ((level == Level.LEVEL_1 || level == Level.LEVEL_2)
                && Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            st = Level.LEVEL_3.stepTime;
        }
        return st;
    }

    private void clearAndSetupProjection() {
        Gdx.gl.glClearColor(0.015f, 0.02f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
    }

    private void drawBackgroundGrid() {
        SnakeRenderer.drawBackgroundGrid(shapes, originX, originY, cellSize, GRID_COLS, GRID_ROWS);
    }

    private void drawFood() {
        if (food == null) return;
        shapes.setColor(Color.SCARLET);
        shapes.rect(originX + food.x * cellSize, originY + food.y * cellSize, cellSize, cellSize);
    }

    private void drawSnakeSausage(float alpha) {
        if (snake == null || snake.isEmpty()) return;

        java.util.List<Vector2> curr = new java.util.ArrayList<>();
        java.util.List<Vector2> prev = new java.util.ArrayList<>();
        for (Point p : snake) curr.add(gridToCenter(p));
        if (prevSnake != null && prevSnake.size() == snake.size()) {
            for (Point p : prevSnake) prev.add(gridToCenter(p));
        } else {
            prev.addAll(curr);
        }

        int n = curr.size();
        if (n == 0) return;
        Vector2[] tangents = new Vector2[n];
        for (int i = 0; i < n; i++) {
            Vector2 a = (i == 0) ? curr.get(i) : curr.get(i - 1);
            Vector2 b = (i == n - 1) ? curr.get(i) : curr.get(i + 1);
            Vector2 t = new Vector2(b).sub(a);
            if (t.isZero(0.0001f)) t.set(1, 0);
            tangents[i] = t.nor();
        }

        float bodyRadius = cellSize * 0.35f;
        float A = cellSize * 0.26f; // stronger amplitude
        float k = 1.4f;             // higher spatial frequency
        float w = 5.0f;             // faster wave

        java.util.List<Vector2> pts = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Vector2 p0 = (i < prev.size()) ? prev.get(i) : curr.get(i);
            Vector2 p1 = curr.get(i);
            Vector2 p = new Vector2(p0).lerp(p1, alpha);
            Vector2 t = tangents[i];
            float phase = i * k - elapsedTime * w;
            float off = A * MathUtils.sin(phase);
            p.add(-t.y * off, t.x * off);
            pts.add(p);
        }

        // Body lines
        shapes.setColor(new Color(0.2f, 0.8f, 0.2f, 1f));
        for (int i = n - 1; i > 0; i--) {
            Vector2 a = pts.get(i);
            Vector2 b = pts.get(i - 1);
            shapes.rectLine(a.x, a.y, b.x, b.y, bodyRadius * 2f);
        }
        // Round joints to avoid hard corners between segments
        for (int i = 1; i < n - 1; i++) {
            Vector2 p = pts.get(i);
            shapes.circle(p.x, p.y, bodyRadius);
        }
        // Tail cap
        Vector2 tail = pts.get(n - 1);
        shapes.circle(tail.x, tail.y, bodyRadius);

        // Head
        Vector2 head = pts.get(0);
        float headRadius = bodyRadius * 1.15f;
        shapes.setColor(Color.LIME);
        shapes.circle(head.x, head.y, headRadius);

        // Eyes (use forward vector opposite to tangent at head)
        Vector2 headTan = tangents[0];
        float fwdX = -headTan.x, fwdY = -headTan.y;
        float norm = (float)Math.sqrt(fwdX*fwdX + fwdY*fwdY);
        if (norm > 1e-6f) { fwdX /= norm; fwdY /= norm; } else { fwdX = 1; fwdY = 0; }
        float nX = -fwdY, nY = fwdX;
        float eyeSide = headRadius * 0.6f;
        float eyeForward = headRadius * 0.15f;
        float eyeR = Math.max(2f, headRadius * 0.22f);
        float eyeLX = head.x + nX * eyeSide + fwdX * eyeForward;
        float eyeLY = head.y + nY * eyeSide + fwdY * eyeForward;
        float eyeRX = head.x - nX * eyeSide + fwdX * eyeForward;
        float eyeRY = head.y - nY * eyeSide + fwdY * eyeForward;
        shapes.setColor(Color.WHITE);
        shapes.circle(eyeLX, eyeLY, eyeR);
        shapes.circle(eyeRX, eyeRY, eyeR);
        shapes.setColor(Color.BLACK);
        float pupilR = Math.max(1.5f, eyeR * 0.45f);
        float pox = fwdX * eyeR * 0.2f;
        float poy = fwdY * eyeR * 0.2f;
        shapes.circle(eyeLX + pox, eyeLY + poy, pupilR);
        shapes.circle(eyeRX + pox, eyeRY + poy, pupilR);

        // Tongue (forked), red — forward
        shapes.setColor(Color.SCARLET);
        float mouthX = head.x + fwdX * headRadius * 1.05f;
        float mouthY = head.y + fwdY * headRadius * 1.05f;
        float tongueLen = headRadius * 1.2f;
        float ang = 18f * MathUtils.degreesToRadians;
        float cosA = MathUtils.cos(ang), sinA = MathUtils.sin(ang);
        float dirLx = fwdX * cosA - fwdY * sinA;
        float dirLy = fwdX * sinA + fwdY * cosA;
        float dirRx = fwdX * cosA + fwdY * sinA;
        float dirRy = -fwdX * sinA + fwdY * cosA;
        float tipLX = mouthX + dirLx * tongueLen;
        float tipLY = mouthY + dirLy * tongueLen;
        float tipRX = mouthX + dirRx * tongueLen;
        float tipRY = mouthY + dirRy * tongueLen;
        float baseOff = bodyRadius * 0.12f;
        float baseLX = mouthX + nX * baseOff;
        float baseLY = mouthY + nY * baseOff;
        float baseRX = mouthX - nX * baseOff;
        float baseRY = mouthY - nY * baseOff;
        shapes.triangle(baseLX, baseLY, mouthX, mouthY, tipLX, tipLY);
        shapes.triangle(baseRX, baseRY, mouthX, mouthY, tipRX, tipRY);
    }

    private Vector2 gridToCenter(Point p) {
        return new Vector2(
                originX + p.x * cellSize + cellSize / 2f,
                originY + p.y * cellSize + cellSize / 2f
        );
    }

    private Deque<Point> copySnake(Deque<Point> src) {
        return new ArrayDeque<>(src); // Points are immutable here
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
        cellSize = Math.max(1, Math.min(width / GRID_COLS, height / GRID_ROWS));
        worldWidth = cellSize * GRID_COLS;
        worldHeight = cellSize * GRID_ROWS;
        originX = (width - worldWidth) / 2;
        originY = (height - worldHeight) / 2;

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

    public static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (o == null || getClass() != o.getClass()) return false; Point point = (Point) o; return x == point.x && y == point.y; }
        @Override public int hashCode() { return (x * 73856093) ^ (y * 19349663); }
    }
}
