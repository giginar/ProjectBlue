package com.projectblue.game.screens;

import com.projectblue.game.config.CampaignConfig;
import com.projectblue.game.config.Difficulty;
import com.projectblue.game.save.Profile;

/** Pure interaction policy shared by card UI and focused tests. */
public final class LevelSelectPolicy {
    private LevelSelectPolicy() {}
    public static boolean canStart(Profile profile, int levelId, Difficulty difficulty) {
        return profile != null && difficulty != null && CampaignConfig.isAvailable(levelId) && profile.canPlay(levelId, difficulty);
    }
}
