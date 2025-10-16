package org.example.snake.ui;

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

import org.example.snake.MainGame;

public class SettingsScreen extends ScreenAdapter {
    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private Texture buttonUpTex, buttonOverTex, buttonDownTex;
    private ShapeRenderer shapes;

    private final int[][] resolutions = new int[][]{
            {800, 600},
            {1024, 768},
            {1280, 720},
            {1600, 900},
            {1920, 1080}
    };
    private int selectedIndex = 2; // default 1280x720
    private boolean fullscreen = false;

    private TextButton resolutionBtn;
    private TextButton fullscreenBtn;

    public SettingsScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        setupStageAndSkin();
        createButtonTextures();
        buildUi();
        Gdx.input.setInputProcessor(stage);
    }

    private void setupStageAndSkin() {
        stage = new Stage(new ScreenViewport(), game.batch);
        shapes = new ShapeRenderer();

        skin = new Skin();
        skin.add("default-font", game.font, com.badlogic.gdx.graphics.g2d.BitmapFont.class);
    }

    private void createButtonTextures() {
        buttonUpTex = makeRoundRectTex(360, 44, new Color(0.6f, 0.5f, 0.85f, 1f));
        buttonOverTex = makeRoundRectTex(360, 44, new Color(0.72f, 0.62f, 0.95f, 1f));
        buttonDownTex = makeRoundRectTex(360, 44, new Color(0.5f, 0.42f, 0.75f, 1f));

        Drawable up = new TextureRegionDrawable(buttonUpTex);
        Drawable over = new TextureRegionDrawable(buttonOverTex);
        Drawable down = new TextureRegionDrawable(buttonDownTex);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, game.font);
        style.over = over;
        skin.add("default", style);
    }

    private void buildUi() {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().width(360).height(44).pad(8);

        resolutionBtn = new TextButton(currentResolutionLabel(), skin);
        resolutionBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedIndex = (selectedIndex + 1) % resolutions.length;
                resolutionBtn.setText(currentResolutionLabel());
            }
        });

        fullscreenBtn = new TextButton(fullscreenLabel(), skin);
        fullscreenBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fullscreen = !fullscreen;
                fullscreenBtn.setText(fullscreenLabel());
            }
        });

        TextButton apply = new TextButton("Apply", skin);
        apply.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                applyResolution();
            }
        });

        TextButton back = new TextButton("Back", skin);
        back.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showMenu();
            }
        });

        table.add(resolutionBtn).row();
        table.add(fullscreenBtn).row();
        table.add(apply).row();
        table.add(back).row();

        stage.addActor(table);
    }

    private void applyResolution() {
        int appliedW;
        int appliedH;
        if (fullscreen) {
            com.badlogic.gdx.Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(dm);
            appliedW = dm.width;
            appliedH = dm.height;
        } else {
            int[] r = resolutions[selectedIndex];
            Gdx.graphics.setWindowedMode(r[0], r[1]);
            appliedW = r[0];
            appliedH = r[1];
        }
        // Persist user choice for next launch
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("settings");
        prefs.putInteger("width", appliedW);
        prefs.putInteger("height", appliedH);
        prefs.putBoolean("fullscreen", fullscreen);
        prefs.flush();
    }

    private String currentResolutionLabel() {
        int[] r = resolutions[selectedIndex];
        return "Resolution: " + r[0] + "x" + r[1];
    }

    private String fullscreenLabel() {
        return "Fullscreen: " + (fullscreen ? "On" : "Off");
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showMenu();
            return;
        }

        ScreenUtils.clear(0.07f, 0.07f, 0.1f, 1f);
        drawBackground();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    private void drawBackground() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = 0; y < h; y += 16) {
            float t = (float) y / h;
            shapes.setColor(new Color(0.08f + 0.12f * t, 0.35f + 0.45f * t, 0.08f, 1f));
            shapes.rect(0, y, w, 16);
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
