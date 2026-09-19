package com.projectblue.game.save;
import com.projectblue.game.config.ContentCatalog;

/** Stable local IDs; a platform adapter can map these to provider IDs. */
public enum Achievement {
    FIRST_DIVE, CLEAN_START, PERFECT_BLUE, NO_ONE_LEFT_BEHIND, UNTOUCHED,
    RECYCLER_I, RECYCLER_II, DRONE_HUNTER, DEEP_EXPLORER, GUARDIAN_OF_THE_BLUE, NIGHTMARE_BELOW, FULLY_EQUIPPED;
    public ContentCatalog.AchievementDef definition() { return ContentCatalog.DEFAULT.achievement(name()); }
}
