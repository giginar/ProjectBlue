package com.projectblue.game.logic;

import static com.projectblue.game.config.GameConfig.*;

public final class Rules {
    private Rules() {}
    public static int damage(int health, int incoming) {
        return Math.max(0, Math.max(0, health) - Math.max(0, incoming));
    }
    public static float clamp(float value, float min, float max) {
        if (max < min) throw new IllegalArgumentException("Invalid bounds");
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
    public static float boundCenter(float position, float radius, float size) {
        if (radius < 0 || size < radius * 2) throw new IllegalArgumentException("Invalid body size");
        return clamp(position, radius, size - radius);
    }
    public static float percentage(int completed, int total) {
        return total <= 0 ? 0 : clamp(100f * completed / total, 0f, 100f);
    }
    public static float cleanup(int collected) { return percentage(collected, PLASTIC_COUNT); }
    public static float rescue(int saved) { return percentage(saved, TURTLE_COUNT); }
    public static int score(int kills, int plastic, int rescued, int salvage, int health, boolean complete) {
        return Math.max(0, kills) * KILL_SCORE + Math.max(0, plastic) * PLASTIC_SCORE
            + Math.max(0, rescued) * RESCUE_SCORE + Math.max(0, salvage) * SALVAGE_SCORE
            + (complete ? COMPLETION_SCORE + Math.max(0, health) * INTEGRITY_SCORE : 0);
    }
    public static int stars(boolean complete, float combat, float cleanup, float rescue, float integrity) {
        if (!complete) return 0;
        float c = clamp(combat, 0, 100), p = clamp(cleanup, 0, 100);
        float r = clamp(rescue, 0, 100), h = clamp(integrity, 0, 100);
        float average = (c + p + r + h) / 4f;
        if (average >= THREE_STAR_AVERAGE && Math.min(Math.min(c, p), Math.min(r, h)) >= THREE_STAR_MIN_CATEGORY) return 3;
        return average >= TWO_STAR_AVERAGE ? 2 : 1;
    }
    public static boolean overlaps(float ax, float ay, float ar, float bx, float by, float br) {
        float dx = ax - bx, dy = ay - by, radius = ar + br;
        return dx * dx + dy * dy <= radius * radius;
    }
}

