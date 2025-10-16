package org.example.snake;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import org.example.snake.game.Level;
import org.example.snake.game.GameScreen;
import org.example.snake.ui.MenuScreen;
import org.example.snake.ui.LevelSelectScreen;
import org.example.snake.ui.HighScoresScreen;
import org.example.snake.ui.SettingsScreen;

public class MainGame extends Game {
    public SpriteBatch batch;
    public BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = loadFont();
        font.setColor(Color.WHITE);
        setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        batch.dispose();
        font.dispose();
    }

    public void startLevel(Level level) {
        setScreen(new GameScreen(this, level));
    }

    public void showMenu() {
        setScreen(new MenuScreen(this));
    }

    public void showLevelSelect() {
        setScreen(new LevelSelectScreen(this));
    }

    public void showHighScores() {
        setScreen(new HighScoresScreen(this));
    }

    public void showSettings() {
        setScreen(new SettingsScreen(this));
    }

    private BitmapFont loadFont() {
        try {
            if (com.badlogic.gdx.files.FileHandle.class != null &&
                com.badlogic.gdx.Gdx.files != null &&
                com.badlogic.gdx.Gdx.files.internal("fonts/NotoEmoji-Regular.ttf").exists()) {
                FreeTypeFontGenerator gen = new FreeTypeFontGenerator(com.badlogic.gdx.Gdx.files.internal("fonts/NotoEmoji-Regular.ttf"));
                FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                p.size = 28;
                p.minFilter = TextureFilter.Linear;
                p.magFilter = TextureFilter.Linear;
                p.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "▶★⚙️❌🔙🗑️✅🏠🔁";
                BitmapFont f = gen.generateFont(p);
                gen.dispose();
                return f;
            }
        } catch (Throwable ignored) { }
        BitmapFont f = new BitmapFont();
        f.getData().setScale(1.8f);
        for (TextureRegion r : f.getRegions()) {
            if (r != null && r.getTexture() != null) {
                r.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            }
        }
        return f;
    }
}
