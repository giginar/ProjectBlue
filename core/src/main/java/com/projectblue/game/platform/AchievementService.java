package com.projectblue.game.platform;
public interface AchievementService {
    void unlock(String id);
    /** Absolute progress, so a later provider can safely retry synchronization. */
    default void setProgress(String id, int progress, int target) {}
}
