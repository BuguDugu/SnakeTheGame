package org.example.snake.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputMultiplexer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import org.example.snake.MainGame;

public class MultiplayerSetupScreen extends ScreenAdapter {
    private static final String DEFAULT_SERVER = "ws://localhost:8080/ws/game";

    private final MainGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;

    private Stage stage;
    private Skin skin;
    private Texture buttonUpTex;
    private Texture buttonOverTex;
    private Texture buttonDownTex;
    private TextField nameField;
    private TextField serverField;

    public MultiplayerSetupScreen(MainGame game) {
        this.game = game;
        this.batch = game.batch;
        this.font = game.font;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport(), batch);
        skin = new Skin();
        skin.add("default-font", font, BitmapFont.class);
        createButtonStyles();
        buildUi();
        InputMultiplexer multiplexer = new InputMultiplexer(stage);
        Gdx.input.setInputProcessor(multiplexer);
    }

    private void createButtonStyles() {
        buttonUpTex = makeRoundRectTex(320, 48, new Color(0.18f, 0.55f, 0.85f, 1f));
        buttonOverTex = makeRoundRectTex(320, 48, new Color(0.24f, 0.70f, 1.0f, 1f));
        buttonDownTex = makeRoundRectTex(320, 48, new Color(0.12f, 0.42f, 0.65f, 1f));
        Drawable up = new TextureRegionDrawable(buttonUpTex);
        Drawable over = new TextureRegionDrawable(buttonOverTex);
        Drawable down = new TextureRegionDrawable(buttonDownTex);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(up, down, up, font);
        style.over = over;
        skin.add("default", style);
        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = font;
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.background = new TextureRegionDrawable(makeRoundRectTex(320, 44, new Color(0.1f, 0.1f, 0.14f, 0.9f)));
        fieldStyle.cursor = new TextureRegionDrawable(makeRectTex(2, 40, Color.WHITE));
        fieldStyle.selection = new TextureRegionDrawable(makeRectTex(2, 40, new Color(0.3f, 0.6f, 1f, 0.45f)));
        skin.add("default", fieldStyle);
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.defaults().width(360).pad(8);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        Label title = new Label("Join Multiplayer", labelStyle);
        nameField = new TextField("", skin);
        nameField.setMessageText("Name");
        serverField = new TextField(DEFAULT_SERVER, skin);

        TextButton join = new TextButton("Join", skin);
        TextButton cancel = new TextButton("Back", skin);

        join.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinGame();
            }
        });
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showMenu();
            }
        });

        root.add(title).padBottom(12).row();
        root.add(nameField).height(48).row();
        root.add(serverField).height(48).row();
        root.add(join).height(48).padTop(12).row();
        root.add(cancel).height(48).row();

        stage.addActor(root);
    }

    private void joinGame() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = "Player";
        }
        String server = serverField.getText().trim();
        if (server.isEmpty()) {
            server = DEFAULT_SERVER;
        }
        game.startMultiplayer(name, server);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.07f, 0.07f, 0.1f, 1f);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showMenu();
            return;
        }
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (skin != null) skin.dispose();
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
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    private Texture makeRectTex(int width, int height, Color color) {
        Pixmap pm = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }
}
