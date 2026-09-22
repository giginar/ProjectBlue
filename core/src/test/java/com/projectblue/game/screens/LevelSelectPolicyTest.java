package com.projectblue.game.screens;

import com.projectblue.game.config.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.save.Profile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LevelSelectPolicyTest {
    @Test void unlockedCardStartsAndLockedCardCannotStart() {
        Profile profile = new Profile();
        assertTrue(LevelSelectPolicy.canStart(profile, 1, Difficulty.NORMAL));
        assertFalse(LevelSelectPolicy.canStart(profile, 2, Difficulty.NORMAL));
        assertFalse(LevelSelectPolicy.canStart(profile, 1, Difficulty.HARD));
    }
    @Test void checkingCardInteractionDoesNotChangeProgression() {
        Profile profile = new Profile();
        int stars = profile.level(1).bestStars;
        LevelSelectPolicy.canStart(profile, 1, Difficulty.NORMAL);
        LevelSelectPolicy.canStart(profile, 2, Difficulty.NORMAL);
        assertEquals(stars, profile.level(1).bestStars);
        assertFalse(profile.level(2).unlocked);
    }
    @Test void existingCompletionStillControlsUnlocks() {
        Profile profile = new Profile();
        RunSpec spec = RunSpec.create(1, Difficulty.NORMAL, Loadout.standard());
        assertTrue(profile.record(new LevelResult(spec, true, spec.combatTargets(), 20, 2, 50, 100)));
        assertTrue(LevelSelectPolicy.canStart(profile, 2, Difficulty.NORMAL));
    }
}
