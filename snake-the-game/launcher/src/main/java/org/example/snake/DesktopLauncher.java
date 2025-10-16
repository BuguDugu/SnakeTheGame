package org.example.snake;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Snake (LibGDX)");
        com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences prefs = new com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences("settings", null);
        int width = prefs.getInteger("width", GameScreen.GRID_COLS * GameScreen.DEFAULT_CELL_SIZE);
        int height = prefs.getInteger("height", GameScreen.GRID_ROWS * GameScreen.DEFAULT_CELL_SIZE);
        boolean fullscreen = prefs.getBoolean("fullscreen", false);
        if (fullscreen) {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            config.setWindowedMode(width, height);
        }
        config.setResizable(false);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new MainGame(), config);
    }
}
