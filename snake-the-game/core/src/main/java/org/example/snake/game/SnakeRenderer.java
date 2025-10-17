package org.example.snake.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class SnakeRenderer {
    private static final Color BACKGROUND_COLOR = new Color(0.015f, 0.02f, 0.05f, 1f);
    private static final Color VIGNETTE_COLOR = new Color(0.02f, 0.04f, 0.09f, 0.6f);
    private SnakeRenderer() {}

    public static void drawBackgroundGrid(ShapeRenderer shapes, int originX, int originY, int cellSize, int cols, int rows) {
        // A calm, dark navy background that keeps the focus on the snakes.
        shapes.setColor(BACKGROUND_COLOR);
        shapes.rect(originX, originY, cols * cellSize, rows * cellSize);

        // Subtle vignette to avoid a completely flat look without introducing a harsh pattern.
        shapes.setColor(VIGNETTE_COLOR);
        float margin = cellSize * 2f;
        shapes.rect(originX + margin,
                originY + margin,
                Math.max(0, cols * cellSize - margin * 2f),
                Math.max(0, rows * cellSize - margin * 2f));
    }

    public static void drawFood(ShapeRenderer shapes, int originX, int originY, int cellSize, GameScreen.Point food) {
        if (food == null) return;
        shapes.setColor(Color.SCARLET);
        shapes.rect(originX + food.x * cellSize, originY + food.y * cellSize, cellSize, cellSize);
    }

    public static void drawSnakeSausage(ShapeRenderer shapes,
                                        int originX, int originY, int cellSize,
                                        Deque<GameScreen.Point> snake,
                                        Deque<GameScreen.Point> prevSnake,
                                        float alpha, float elapsedTime) {
        if (snake == null || snake.isEmpty()) return;

        List<Vector2> curr = new ArrayList<>();
        List<Vector2> prev = new ArrayList<>();
        for (GameScreen.Point p : snake) curr.add(gridToCenter(originX, originY, cellSize, p));
        if (prevSnake != null && prevSnake.size() == snake.size()) {
            for (GameScreen.Point p : prevSnake) prev.add(gridToCenter(originX, originY, cellSize, p));
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
        float A = cellSize * 0.26f;
        float k = 1.4f;
        float w = 5.0f;

        List<Vector2> pts = new ArrayList<>(n);
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

        shapes.setColor(new Color(0.2f, 0.8f, 0.2f, 1f));
        for (int i = n - 1; i > 0; i--) {
            Vector2 a = pts.get(i);
            Vector2 b = pts.get(i - 1);
            shapes.rectLine(a.x, a.y, b.x, b.y, bodyRadius * 2f);
        }
        for (int i = 1; i < n - 1; i++) {
            Vector2 p = pts.get(i);
            shapes.circle(p.x, p.y, bodyRadius);
        }
        Vector2 tail = pts.get(n - 1);
        shapes.circle(tail.x, tail.y, bodyRadius);

        Vector2 head = pts.get(0);
        float headRadius = bodyRadius * 1.15f;
        shapes.setColor(Color.LIME);
        shapes.circle(head.x, head.y, headRadius);

        Vector2 t0 = tangents[0];
        float fwdX = -t0.x, fwdY = -t0.y;
        float inv = 1f / (float) Math.sqrt(fwdX * fwdX + fwdY * fwdY);
        fwdX *= inv; fwdY *= inv;
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

    public static void drawHud(SpriteBatch batch, BitmapFont font, int originX, int worldHeight, int score, boolean gameOver) {
        if (gameOver) return;
        font.draw(batch, "Score: " + score, originX + 8, worldHeight - 8);
    }

    private static Vector2 gridToCenter(int originX, int originY, int cellSize, GameScreen.Point p) {
        return new Vector2(originX + p.x * cellSize + cellSize / 2f,
                originY + p.y * cellSize + cellSize / 2f);
    }
}

