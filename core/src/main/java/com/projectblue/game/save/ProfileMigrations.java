package com.projectblue.game.save;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.Loadout.*;
import java.util.Properties;

/** Only facts available in the old schema are migrated; unknown ecology records stay zero. */
final class ProfileMigrations {
    private ProfileMigrations() {}
    static void fromV0(Profile p) {
        p.level(1).bestScore = p.bestScore;
    }
    static void fromV1(Profile p) {
        fromV0(p);
        if (p.completedRuns > 0 && p.bestStars > 0) {
            p.level(1).bestStars = p.bestStars;
            p.level(1).completedDifficulties = Difficulty.NORMAL.bit();
            p.restoreAchievement(Achievement.FIRST_DIVE, 1);
            p.restoreAchievementState(Achievement.FIRST_DIVE, true, false);
        }
    }
    static void fromV2(Profile p, Properties fields) {
        p.selectedPilot = switch (fields.getProperty("selectedPilot", "")) {
            case "MARIN", "SOL" -> Pilot.KAIA;
            case "NERI" -> Pilot.NERI;
            default -> throw new IllegalArgumentException("Invalid v2 pilot");
        };
        p.selectedSubmarine = switch (fields.getProperty("selectedSubmarine", "")) {
            case "MINNOW" -> Submarine.TIDE;
            case "NEEDLE" -> Submarine.MANTA;
            case "BASTION" -> Submarine.LEVIATHAN;
            default -> throw new IllegalArgumentException("Invalid v2 submarine");
        };
        // Every old vessel/pilot was freely selectable. Preserve that access after migration.
        for (Submarine sub : Submarine.values()) p.restoreUnlocked(sub);
        p.restoreUnlocked(Pilot.KAIA); p.restoreUnlocked(Pilot.NERI);
        p.restoreUpgrade(Upgrade.HULL, value(fields,"upgrade.HULL"));
        p.restoreUpgrade(Upgrade.PRIMARY_WEAPON, value(fields,"upgrade.PULSE"));
        p.legacyMagnetLevel = value(fields,"upgrade.MAGNET");
        p.totalPlastic = Math.max(0,value(fields,"achievement.CLEANER"));
        p.legacyRescuerProgress = value(fields,"achievement.RESCUER");
        p.legacyExplorerProgress = value(fields,"achievement.EXPLORER");
        migrate(p,Achievement.FIRST_DIVE,value(fields,"achievement.FIRST_DIVE"));
        migrate(p,Achievement.RECYCLER_I,p.totalPlastic);
        migrate(p,Achievement.RECYCLER_II,p.totalPlastic);
        migrate(p,Achievement.NIGHTMARE_BELOW,value(fields,"achievement.ABYSS_CLEAR"));
    }
    private static void migrate(Profile p, Achievement a, int progress) {
        p.restoreAchievement(a,progress);
        p.restoreAchievementState(a,progress >= p.content().achievement(a.name()).target(),false);
    }
    private static int value(Properties p, String key) { return Integer.parseInt(p.getProperty(key)); }
}
