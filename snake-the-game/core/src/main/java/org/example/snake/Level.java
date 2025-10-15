package org.example.snake;

public enum Level {
    LEVEL_1("Level 1 - Slow", 0.20f),
    LEVEL_2("Level 2 - Normal", 0.14f),
    LEVEL_3("Level 3 - Fast", 0.10f),
    LEVEL_4("Level 4 - Faster", 0.075f),
    LEVEL_5("Level 5 - Insane", 0.050f);

    public final String title;
    public final float stepTime;

    Level(String title, float stepTime) {
        this.title = title;
        this.stepTime = stepTime;
    }
}