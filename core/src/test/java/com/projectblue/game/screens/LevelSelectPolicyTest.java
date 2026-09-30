package com.projectblue.game.screens;

import com.projectblue.game.config.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.save.Profile;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
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
    @Test void oneSwipeMovesExactlyOnePageAndSmallMotionDoesNothing() {
        assertEquals(6,LevelSelectPolicy.afterSwipe(5,LevelSelectPolicy.SWIPE_THRESHOLD+1));
        assertEquals(4,LevelSelectPolicy.afterSwipe(5,-LevelSelectPolicy.SWIPE_THRESHOLD-1));
        assertEquals(5,LevelSelectPolicy.afterSwipe(5,LevelSelectPolicy.SWIPE_THRESHOLD-1));
        assertEquals(5,LevelSelectPolicy.afterSwipe(5,Float.NaN));
    }
    @Test void keyboardNavigationStopsAtFirstAndLastPage() {
        assertEquals(1,LevelSelectPolicy.move(1,-1));
        assertEquals(2,LevelSelectPolicy.move(1,1));
        assertEquals(CampaignConfig.LEVEL_COUNT,LevelSelectPolicy.move(CampaignConfig.LEVEL_COUNT,1));
        assertEquals(CampaignConfig.LEVEL_COUNT-1,LevelSelectPolicy.move(CampaignConfig.LEVEL_COUNT,-1));
    }
    @Test void leftAndRightSelectOnlyUnlockedDifficulties() {
        Profile profile=new Profile();
        assertEquals(Difficulty.NORMAL,LevelSelectPolicy.changeDifficulty(profile,1,Difficulty.NORMAL,1));
        RunSpec normal=RunSpec.create(1,Difficulty.NORMAL,Loadout.standard());
        profile.record(new LevelResult(normal,true,normal.combatTargets(),20,2,50,100));
        assertEquals(Difficulty.HARD,LevelSelectPolicy.changeDifficulty(profile,1,Difficulty.NORMAL,1));
        assertEquals(Difficulty.NORMAL,LevelSelectPolicy.changeDifficulty(profile,1,Difficulty.HARD,-1));
        assertEquals(Difficulty.HARD,LevelSelectPolicy.changeDifficulty(profile,1,Difficulty.HARD,1));
    }
    @Test void cardAndCtaStartButDifficultyControlsNeverLaunch() {
        assertEquals(LevelSelectPolicy.CardAction.START,LevelSelectPolicy.cardAction("start-dive",true));
        assertEquals(LevelSelectPolicy.CardAction.START,LevelSelectPolicy.cardAction(null,true));
        assertEquals(LevelSelectPolicy.CardAction.DIFFICULTY,LevelSelectPolicy.cardAction("difficulty-next",true));
        assertEquals(LevelSelectPolicy.CardAction.NONE,LevelSelectPolicy.cardAction("start-dive",false));
    }
    @Test void levelSelectKeepsCardActionAndDoesNotRestoreAGlobalPlayButton() throws Exception {
        String source=Files.readString(Path.of("src/main/java/com/projectblue/game/screens/LevelSelectScreen.java"));
        assertTrue(source.contains("\"start-dive\""));
        assertFalse(source.contains("\"play\""));
    }
}
