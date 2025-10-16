package org.example.snake;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class MainGame extends Game {
    SpriteBatch batch;
    BitmapFont font;

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

    void startLevel(Level level) {
        setScreen(new GameScreen(this, level));
    }

    void showMenu() {
        setScreen(new MenuScreen(this));
    }

    void showLevelSelect() {
        setScreen(new LevelSelectScreen(this));
    }

    void showHighScores() {
        setScreen(new HighScoresScreen(this));
    }

    void showSettings() {
        setScreen(new SettingsScreen(this));
    }

    private BitmapFont loadFont() {
        try {
            // Try to load a TTF with emoji support from assets if present
            if (com.badlogic.gdx.files.FileHandle.class != null &&
                com.badlogic.gdx.Gdx.files != null &&
                com.badlogic.gdx.Gdx.files.internal("fonts/NotoEmoji-Regular.ttf").exists()) {
                FreeTypeFontGenerator gen = new FreeTypeFontGenerator(com.badlogic.gdx.Gdx.files.internal("fonts/NotoEmoji-Regular.ttf"));
                FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                p.size = 18;
                p.characters = FreeTypeFontGenerator.DEFAULT_CHARS // ASCII baseline
                        + "▶★⚙️❌🔙🗑️✅🏠🔁"; // add emoji we try to use (may render monochrome)
                BitmapFont f = gen.generateFont(p);
                gen.dispose();
                return f;
            }
        } catch (Throwable ignored) { }
        // Fallback
        return new BitmapFont();
    }
}
