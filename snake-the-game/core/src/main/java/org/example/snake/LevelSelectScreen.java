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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class LevelSelectScreen extends ScreenAdapter {
    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private Texture buttonUpTex, buttonOverTex, buttonDownTex;
    private ShapeRenderer shapes;
    private final Level[] levels = Level.values();
    private Level selected;
    private Label selectedLabel;

    public LevelSelectScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        setupStageAndSkin();
        createButtonTextures();
        buildLevelTable();
        Gdx.input.setInputProcessor(stage);
    }

    private void setupStageAndSkin() {
        stage = new Stage(new ScreenViewport(), game.batch);
        shapes = new ShapeRenderer();

        skin = new Skin();
        skin.add("default-font", game.font, com.badlogic.gdx.graphics.g2d.BitmapFont.class);
    }

    private void createButtonTextures() {
        buttonUpTex = makeRoundRectTex(360, 44, new Color(0.15f, 0.6f, 0.8f, 1f));
        buttonOverTex = makeRoundRectTex(360, 44, new Color(0.20f, 0.75f, 0.95f, 1f));
        buttonDownTex = makeRoundRectTex(360, 44, new Color(0.10f, 0.5f, 0.68f, 1f));

        Drawable up = new TextureRegionDrawable(buttonUpTex);
        Drawable over = new TextureRegionDrawable(buttonOverTex);
        Drawable down = new TextureRegionDrawable(buttonDownTex);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, game.font);
        style.over = over;
        skin.add("default", style);
    }

    private void buildLevelTable() {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().width(360).height(44).pad(6);

        for (Level lvl : levels) {
            TextButton b = new TextButton(lvl.title, skin);
            b.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selected = lvl;
                    updateSelectedLabel();
                }
            });
            table.add(b).row();
        }

        // Footer panel with Start and Back
        TextButton start = new TextButton("Start", skin);
        TextButton back = new TextButton("Back", skin);

        start.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selected != null) {
                    game.startLevel(selected);
                }
            }
        });
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showMenu();
            }
        });

        // Selected label
        selectedLabel = new Label("Selected: None", new Label.LabelStyle(game.font, com.badlogic.gdx.graphics.Color.WHITE));

        table.add(selectedLabel).width(360).height(32).padTop(12).row();
        table.add(start).row();
        table.add(back).row();

        stage.addActor(table);
    }

    private void updateSelectedLabel() {
        if (selectedLabel != null) {
            String name = selected == null ? "None" : selected.title;
            selectedLabel.setText("Selected: " + name);
        }
    }

    @Override
    public void render(float delta) {
        handleInput();
        ScreenUtils.clear(0.07f, 0.07f, 0.1f, 1f);
        drawMeadowBackground();
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
        if (shapes == null) return;
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
            game.showMenu();
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
        if (shapes != null) shapes.dispose();
    }
}
