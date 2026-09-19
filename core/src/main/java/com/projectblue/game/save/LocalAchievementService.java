package com.projectblue.game.save;

import com.projectblue.game.platform.AchievementService;

/** Local state is authoritative. A future provider receives idempotent absolute snapshots. */
public final class LocalAchievementService {
    private final SaveService saves;
    public LocalAchievementService(SaveService saves) { this.saves = saves; }
    public Achievement nextNotification() {
        Profile p = saves.profile();
        for (Achievement a : Achievement.values()) {
            if (p.notificationPending(a)) {
                // Do not announce an unlock that has not reached disk yet.
                if (!saves.acknowledge(a)) return null;
                return a;
            }
        }
        return null;
    }
    public void synchronize(AchievementService destination) {
        Profile p = saves.profile();
        for (Achievement a : Achievement.values()) {
            destination.setProgress(a.name(),p.achievementProgress(a),p.content().achievement(a.name()).target());
            if (p.achievementUnlocked(a)) destination.unlock(a.name());
        }
    }
}
