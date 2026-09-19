package com.projectblue.game.save;

import com.projectblue.game.config.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.logic.MissionOutcome;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FinalePersistenceTest {
    private static LevelResult clear(int level) {
        RunSpec spec=RunSpec.create(level,Difficulty.NORMAL,Loadout.standard());
        MissionConfig mission=spec.mission();
        int enemies=mission.enemyCount(spec.tuning().spawnDensity());
        MissionOutcome outcome=new MissionOutcome(enemies,enemies,mission.plasticCount,mission.cleanupCount(),
            mission.rescueCount(),100,spec.loadout().health(),0,0,6000,1);
        return new LevelResult(spec,true,outcome);
    }

    @Test void sectorNineUnlocksFinaleAndSectorTenUnlocksGuardianWithoutResettingProgress() throws Exception {
        Profile profile=new Profile();
        for (int level=1;level<=9;level++) assertTrue(profile.record(clear(level)));
        assertTrue(CampaignConfig.isAvailable(10));
        assertTrue(profile.canPlay(10,Difficulty.NORMAL));
        assertFalse(profile.campaignCompleted());
        assertFalse(profile.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE));

        int salvageBefore=profile.totalSalvage;
        assertTrue(profile.record(clear(10)));
        assertTrue(profile.campaignCompleted());
        assertTrue(profile.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE));
        assertTrue(profile.canPlay(1,Difficulty.NORMAL));
        assertTrue(profile.canPlay(10,Difficulty.NORMAL));
        assertTrue(profile.canPlay(10,Difficulty.HARD));
        assertTrue(profile.totalSalvage>salvageBefore);

        Profile reopened=new ProfileCodec().decode(new ProfileCodec().encode(profile));
        assertTrue(reopened.campaignCompleted());
        assertTrue(reopened.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE));
        assertTrue(reopened.notificationPending(Achievement.GUARDIAN_OF_THE_BLUE));
        for (int level=1;level<=10;level++) {
            assertTrue(reopened.level(level).bestStars>0);
            assertTrue(reopened.canPlay(level,Difficulty.NORMAL));
        }
        FinaleSummary summary=new FinaleSummary(reopened);
        assertTrue(summary.completed()); assertEquals(10,summary.completedSectors());
        assertEquals(30,summary.totalStars());
        assertEquals(100,summary.cleanupAverage(),.001f);
        assertEquals(100,summary.rescueAverage(),.001f);
        assertFalse(summary.rescuedSpecies().isEmpty());
    }

    @Test void finaleWildlifeOnlyComesFromRegionsWithRecordedRescues() {
        Profile profile=new Profile();
        profile.level(1).bestStars=1;
        assertTrue(new FinaleSummary(profile).rescuedSpecies().isEmpty());
        profile.level(1).bestRescue=50;
        assertTrue(new FinaleSummary(profile).rescuedSpecies().contains(MissionConfig.CreatureKind.TURTLE));
    }
}
