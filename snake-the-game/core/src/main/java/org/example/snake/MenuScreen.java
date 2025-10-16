package org.example.snake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MenuScreen extends ScreenAdapter {
    private final MainGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private Stage stage;
    private Skin skin;
    private Texture buttonUpTex, buttonOverTex, buttonDownTex;
    private ShapeRenderer shapes;
    private Texture snakeImage;

    public MenuScreen(MainGame game) {
        this.game = game;
        this.batch = game.batch;
        this.font = game.font;
    }

    @Override
    public void show() {
        setupStageAndSkin();
        createButtonTextures();
        buildMenuTable();
        Gdx.input.setInputProcessor(stage);
    }

    private void setupStageAndSkin() {
        stage = new Stage(new ScreenViewport(), batch);
        shapes = new ShapeRenderer();

        skin = new Skin();
        skin.add("default-font", font, BitmapFont.class);
    }

    private void createButtonTextures() {
        buttonUpTex = makeRoundRectTex(320, 48, new Color(0.15f, 0.7f, 0.3f, 1f));
        buttonOverTex = makeRoundRectTex(320, 48, new Color(0.20f, 0.85f, 0.4f, 1f));
        buttonDownTex = makeRoundRectTex(320, 48, new Color(0.10f, 0.55f, 0.25f, 1f));

        Drawable up = new TextureRegionDrawable(buttonUpTex);
        Drawable over = new TextureRegionDrawable(buttonOverTex);
        Drawable down = new TextureRegionDrawable(buttonDownTex);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, font);
        style.over = over;
        skin.add("default", style);
        // Create decorative snake image
        snakeImage = makeSnakeImage(360, 100);
    }

    private void buildMenuTable() {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().width(320).height(48).pad(8);

        TextButton play = new TextButton("Play", skin);
        TextButton scores = new TextButton("High Scores", skin);
        TextButton exit = new TextButton("Exit", skin);
        TextButton settings = new TextButton("Settings", skin);

        play.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showLevelSelect();
            }
        });
        scores.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showHighScores();
            }
        });
        settings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showSettings();
            }
        });
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        table.add(play).row();
        table.add(scores).row();
            table.add(settings).row();
        table.add(exit).row();

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        handleInput();
        ScreenUtils.clear(0.07f, 0.07f, 0.1f, 1f);
        drawMeadowBackground();
        batch.begin();
        font.draw(batch, "Snake (LibGDX)", 20, Gdx.graphics.getHeight() - 20);
        if (snakeImage != null) {
            int w = Gdx.graphics.getWidth();
            float imgW = Math.min(480, w * 0.8f);
            float imgH = snakeImage.getHeight() * (imgW / snakeImage.getWidth());
            float x = (w - imgW) / 2f;
            float y = Gdx.graphics.getHeight() - imgH - 40;
            batch.draw(snakeImage, x, y, imgW, imgH);
        }
        batch.end();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    private void drawMeadowBackground() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = 0; y < h; y += 16) {
            float t = (float) y / h;
            shapes.setColor(new Color(0.08f + 0.12f * t, 0.35f + 0.45f * t, 0.08f, 1f));
            shapes.rect(0, y, w, 16);
        }
        shapes.setColor(new Color(0.0f, 0.1f, 0.0f, 0.12f));
        for (int y = 0; y < h; y += 24) {
            for (int x = 0; x < w; x += 24) {
                if (((x + y) / 24) % 2 == 0) shapes.rect(x, y, 24, 24);
            }
        }
        float cx = w * 0.75f;
        float cy = h * 0.35f;
        float s = 12f;
        shapes.setColor(new Color(0.18f, 0.8f, 0.2f, 1f));
        for (int i = 0; i < 18; i++) {
            float px = cx - i * s * 1.2f;
            float py = cy + (float) Math.sin(i * 0.6f) * 10f;
            shapes.rect(px, py, s, s);
        }
        shapes.setColor(new Color(0.15f, 0.7f, 0.18f, 1f));
        shapes.rect(cx, cy, s, s);
        shapes.setColor(Color.BLACK);
        shapes.rect(cx + 3, cy + s - 6, 3, 3);
        shapes.rect(cx + 7, cy + s - 6, 3, 3);
        shapes.setColor(Color.PINK);
        shapes.rect(cx + s - 3, cy + s / 2f - 2, 4, 4);
        shapes.end();
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

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
        if (buttonUpTex != null) buttonUpTex.dispose();
        if (buttonOverTex != null) buttonOverTex.dispose();
        if (buttonDownTex != null) buttonDownTex.dispose();
        if (snakeImage != null) snakeImage.dispose();
        if (shapes != null) shapes.dispose();
    }

    private Texture makeSnakeImage(int width, int height) {
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pm.setColor(0, 0, 0, 0);
        pm.fill();
        int s = Math.max(6, height / 8);
        int margin = s;
        int y = height / 2;
        for (int i = 0; i < (width - 2 * margin) / (s + 6); i++) {
            float t = i / 6f;
            int yy = (int) (y + Math.sin(t * 2.2) * (height * 0.2));
            pm.setColor(0.18f, 0.8f, 0.2f, 1f);
            pm.fillRectangle(margin + i * (s + 6), yy - s / 2, s, s);
        }
        // Head
        pm.setColor(0.15f, 0.7f, 0.18f, 1f);
        pm.fillRectangle(width - margin - s, y - s / 2, s, s);
        // Eyes
        pm.setColor(0, 0, 0, 1);
        pm.fillRectangle(width - margin - s + 3, y + s / 2 - 6, 3, 3);
        pm.fillRectangle(width - margin - s + 7, y + s / 2 - 6, 3, 3);
        // Tongue
        pm.setColor(1f, 0.3f, 0.4f, 1f);
        pm.fillRectangle(width - margin - 2, y - 2, 4, 4);

        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }
}
