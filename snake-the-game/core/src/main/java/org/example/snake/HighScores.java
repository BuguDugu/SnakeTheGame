package org.example.snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighScores {
    private static final List<Integer> scores = new ArrayList<>();
    private static final int MAX = 10;

    public static void submit(int score) {
        if (score <= 0) return;
        scores.add(score);
        scores.sort(Collections.reverseOrder());
        if (scores.size() > MAX) {
            scores.subList(MAX, scores.size()).clear();
        }
    }

    public static List<Integer> top() {
        return new ArrayList<>(scores);
    }

    public static void clearAll() {
        scores.clear();
    }
}