package org.example.snake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.*;

public class GameScreen extends ScreenAdapter {
    public static final int CELL_SIZE = 20;
    public static final int GRID_COLS = 30;
    public static final int GRID_ROWS = 20;

    private final MainGame game;
    private final Level level;

    private ShapeRenderer shapes;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    private Deque<Point> snake;
    private Set<Point> snakeSet;
    private Direction dir = Direction.RIGHT;
    private Direction nextDir = Direction.RIGHT;
    private Point food;
    private Random rng;

    private float accumulator = 0f;
    private boolean gameOver = false;
    private int score = 0;

    public GameScreen(MainGame game, Level level) {
        this.game = game;
        this.level = level;
    }

    @Override
    public void show() {
        int width = GRID_COLS * CELL_SIZE;
        int height = GRID_ROWS * CELL_SIZE;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, width, height);
        shapes = new ShapeRenderer();
        shapes.setAutoShapeType(true);
        batch = game.batch;
        font = game.font;
        rng = new Random();
        resetGame();
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
        } else if (handleGameOverInput()) {
            return;
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
                    shapes.rect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    private void drawFood() {
        if (food == null) return;
        shapes.setColor(Color.SCARLET);
        shapes.rect(food.x * CELL_SIZE, food.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
    }

    private void drawSnake() {
        int i = 0;
        for (Point p : snake) {
            if (i == 0) shapes.setColor(Color.LIME);
            else shapes.setColor(new Color(0.2f, 0.8f, 0.2f, 1f));

            shapes.rect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            i++;
        }
    }

    private void drawHud() {
        String text = "Score: " + score + (gameOver ? "  |  Game Over! ENTER=Menu, R=Restart" : "");
        font.draw(batch, text, 8, GRID_ROWS * CELL_SIZE - 8);
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
            gameOver = true;
            return;
        }
        Point newHead = new Point(nx, ny);
        if (snakeSet.contains(newHead)) {
            gameOver = true;
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
        camera.setToOrtho(false, GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE);
    }

    @Override
    public void dispose() {
        if (shapes != null) shapes.dispose();
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