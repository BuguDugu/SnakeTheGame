package org.example.snake;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Snake (LibGDX)");
        config.setWindowedMode(GameScreen.GRID_COLS * GameScreen.CELL_SIZE, GameScreen.GRID_ROWS * GameScreen.CELL_SIZE);
        config.setResizable(false);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new MainGame(), config);
    }
}