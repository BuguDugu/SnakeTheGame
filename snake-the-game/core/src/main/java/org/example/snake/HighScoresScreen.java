package org.example.snake;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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

import java.util.List;

public class HighScoresScreen extends ScreenAdapter {
    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private Texture buttonUpTex, buttonOverTex, buttonDownTex;
    private ShapeRenderer shapes;

    public HighScoresScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        setupStageAndSkin();
        createButtonTextures();
        buildFooterButtons();
        Gdx.input.setInputProcessor(stage);
    }

    private void setupStageAndSkin() {
        stage = new Stage(new ScreenViewport(), game.batch);
        shapes = new ShapeRenderer();

        skin = new Skin();
        skin.add("default-font", game.font, com.badlogic.gdx.graphics.g2d.BitmapFont.class);
    }

    private void createButtonTextures() {
        buttonUpTex = makeRoundRectTex(300, 40, new Color(0.85f, 0.7f, 0.15f, 1f));
        buttonOverTex = makeRoundRectTex(300, 40, new Color(0.95f, 0.8f, 0.25f, 1f));
        buttonDownTex = makeRoundRectTex(300, 40, new Color(0.75f, 0.6f, 0.12f, 1f));

        Drawable up = new TextureRegionDrawable(buttonUpTex);
        Drawable over = new TextureRegionDrawable(buttonOverTex);
        Drawable down = new TextureRegionDrawable(buttonDownTex);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, game.font);
        style.over = over;
        skin.add("default", style);
    }

    private void buildFooterButtons() {
        Table table = new Table();
        table.setFillParent(true);
        table.bottom().pad(12);

        TextButton back = new TextButton("Back", skin);
        TextButton clear = new TextButton("Clear", skin);

        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showMenu();
            }
        });
        clear.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                HighScores.clearAll();
            }
        });

        table.add(back).width(140).height(40).padRight(8);
        table.add(clear).width(140).height(40).padLeft(8);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        if (handleBackKeys()) return;

        ScreenUtils.clear(0.07f, 0.07f, 0.1f, 1f);
        drawMeadowBackground();

        game.batch.begin();
        drawScoresList();
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    private boolean handleBackKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.showMenu();
            return true;
        }
        return false;
    }

    private void drawScoresList() {
        game.font.draw(game.batch, "High Scores (Top 10)", 20, Gdx.graphics.getHeight() - 20);

        List<Integer> list = HighScores.top();
        int startY = Gdx.graphics.getHeight() - 80;

        if (list.isEmpty()) {
            game.font.draw(game.batch, "No scores yet", 64, startY);
        } else {
            for (int i = 0; i < list.size(); i++) {
                float y = startY - i * 26;
                drawMedal(i, 40, (int) y - 18);

                game.font.setColor(Color.WHITE);
                game.font.draw(game.batch, (i + 1) + ". " + list.get(i), 72, y);
            }
        }

        game.font.setColor(Color.WHITE);
        game.font.draw(game.batch, "ENTER/ESC to back or use Back; Clear to reset", 20, 64);
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
        shapes.end();
    }

    private void drawMedal(int index, int x, int y) {
        Color c;
        if (index == 0) c = new Color(1f, 0.84f, 0f, 1f);
        else if (index == 1) c = new Color(0.75f, 0.75f, 0.75f, 1f);
        else if (index == 2) c = new Color(0.8f, 0.5f, 0.2f, 1f);
        else c = new Color(0.2f, 0.6f, 1f, 1f);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(c);
        shapes.rect(x, y, 20, 20);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(x + 4, y + 20, 4, 8);
        shapes.rect(x + 12, y + 20, 4, 8);
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
        if (shapes != null) shapes.dispose();
    }
}
