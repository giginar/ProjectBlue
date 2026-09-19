package com.projectblue.game.logic;

/** Reusable, allocation-free progress rules for nets in any mission. */
public final class NetCuttingSystem {
    private static final float STANDARD_SHOT_PROGRESS = .06f;
    private static final float CUTTER_PROGRESS_PER_SECOND = 1.8f;

    private NetCuttingSystem() {}

    public static float standardShot(float progress) {
        return Rules.clamp(progress + STANDARD_SHOT_PROGRESS, 0, 1);
    }

    public static float cutter(float progress, float delta, float cleanupPower) {
        if (delta <= 0 || !Float.isFinite(delta) || cleanupPower <= 0 || !Float.isFinite(cleanupPower)) return progress;
        return Rules.clamp(progress + delta * CUTTER_PROGRESS_PER_SECOND * cleanupPower, 0, 1);
    }

    public static boolean opened(float progress) { return progress >= 1; }
}
