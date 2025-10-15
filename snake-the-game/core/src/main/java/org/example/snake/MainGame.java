package org.example.snake;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MainGame extends Game {
    SpriteBatch batch;
    BitmapFont font;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
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
}