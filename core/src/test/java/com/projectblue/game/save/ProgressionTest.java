package com.projectblue.game.save;

import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.logic.MissionOutcome;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressionTest {
    private LevelResult clear(int id, Difficulty difficulty) {
        RunSpec spec = RunSpec.create(id, difficulty, Loadout.standard());
        return new LevelResult(spec, true, spec.combatTargets(), 36, 5, 25, 100);
    }
    @Test void newProfileOnlyOpensLevelOneNormalAndRejectsInvalidIds() {
        Profile p = new Profile();
        for (int id = 1; id <= 10; id++) for (Difficulty d : Difficulty.values())
            assertEquals(id == 1 && d == Difficulty.NORMAL, p.canPlay(id, d));
        assertFalse(p.canPlay(0, Difficulty.NORMAL)); assertFalse(p.canPlay(11, Difficulty.NORMAL));
        assertFalse(p.canPlay(1, null));
    }
    @Test void oneStarOpensNextLevelAndHardButFailureDoesNot() {
        Profile p = new Profile();
        p.record(new LevelResult(false, 0, 0, 0, 0, 0));
        assertFalse(p.canPlay(2, Difficulty.NORMAL)); assertFalse(p.canPlay(1, Difficulty.HARD));
        LevelResult oneStar = new LevelResult(true, 0, 0, 0, 0, 1);
        assertEquals(1, oneStar.stars); p.record(oneStar);
        assertTrue(p.canPlay(2, Difficulty.NORMAL)); assertTrue(p.canPlay(1, Difficulty.HARD));
        assertFalse(p.canPlay(2, Difficulty.HARD)); assertFalse(p.canPlay(3, Difficulty.NORMAL));
    }
    @Test void authoredSectorCompletionsOpenAbyssMinePlasticVortexAndNereidCore() {
        Profile p=new Profile();
        assertTrue(p.record(clear(1,Difficulty.NORMAL))); assertTrue(p.canPlay(2,Difficulty.NORMAL));
        assertTrue(p.record(clear(2,Difficulty.NORMAL))); assertTrue(p.canPlay(3,Difficulty.NORMAL));
        assertTrue(p.record(clear(3,Difficulty.NORMAL))); assertTrue(p.canPlay(4,Difficulty.NORMAL));
        assertTrue(CampaignConfig.isAvailable(4));
        assertTrue(p.record(clear(4,Difficulty.NORMAL))); assertTrue(p.canPlay(5,Difficulty.NORMAL));
        assertTrue(p.achievementUnlocked(Achievement.SUNKEN_CITY_RESTORED));
        assertTrue(CampaignConfig.isAvailable(5));
        assertTrue(p.record(clear(5,Difficulty.NORMAL))); assertTrue(p.canPlay(6,Difficulty.NORMAL));
        assertTrue(p.achievementUnlocked(Achievement.BLACK_TIDE_CLEARED));
        assertTrue(CampaignConfig.isAvailable(6));
        assertTrue(p.record(clear(6,Difficulty.NORMAL))); assertTrue(p.canPlay(7,Difficulty.NORMAL));
        assertTrue(p.achievementUnlocked(Achievement.SILENT_REEF_RESTORED));
        assertTrue(CampaignConfig.isAvailable(7));
        assertTrue(p.record(clear(7,Difficulty.NORMAL))); assertTrue(p.canPlay(8,Difficulty.NORMAL));
        assertTrue(p.achievementUnlocked(Achievement.FROZEN_DEPTHS_CLEARED));
        assertTrue(CampaignConfig.isAvailable(8));
        assertTrue(p.record(clear(8,Difficulty.NORMAL))); assertTrue(p.canPlay(9,Difficulty.NORMAL));
        assertTrue(CampaignConfig.isAvailable(9));
        assertTrue(p.record(clear(9,Difficulty.NORMAL))); assertTrue(p.canPlay(10,Difficulty.NORMAL));
        assertTrue(CampaignConfig.isAvailable(10));
    }
    @Test void difficultyUnlocksAreSequentialAndLocalToEachLevel() {
        Profile p = new Profile();
        assertFalse(p.record(clear(1, Difficulty.HARD)));
        assertFalse(p.record(clear(2, Difficulty.NORMAL)));
        assertEquals(0, p.totalSalvage);
        for (Difficulty d : Difficulty.values()) {
            assertTrue(p.canPlay(1, d)); assertTrue(p.record(clear(1, d)));
            assertEquals(d, p.level(1).bestDifficulty());
            assertTrue(p.level(1).completed(d));
        }
        assertFalse(p.canPlay(2, Difficulty.HARD));
        assertTrue(p.record(clear(2, Difficulty.NORMAL)));
        assertTrue(p.canPlay(2, Difficulty.HARD)); assertFalse(p.canPlay(2, Difficulty.EXPERT));
        assertEquals(1, p.achievementProgress(Achievement.NIGHTMARE_BELOW));
    }
    @Test void replayRetainsIndependentBestRecordsAndHarderCompletions() {
        Profile p = new Profile();
        p.record(clear(1, Difficulty.NORMAL)); p.record(clear(1, Difficulty.HARD));
        int bestScore = p.level(1).bestScore;
        p.record(new LevelResult(true, 0, 0, 0, 0, 1));
        assertTrue(p.canPlay(1, Difficulty.NORMAL)); assertTrue(p.canPlay(1, Difficulty.HARD));
        assertEquals(3, p.level(1).bestStars); assertEquals(bestScore, p.level(1).bestScore);
        assertEquals(100, p.level(1).bestCleanup); assertEquals(100, p.level(1).bestRescue);
        assertEquals(Difficulty.HARD, p.level(1).bestDifficulty());
        assertEquals(3, p.completedRuns);
    }
    @Test void failedReplayCanImproveEcologyButNeverUnlockDifficulty() {
        Profile p = new Profile();
        p.record(new LevelResult(false, 0, 36, 5, 5, 0));
        assertEquals(100, p.level(1).bestCleanup); assertEquals(100, p.level(1).bestRescue);
        assertEquals(0, p.level(1).bestStars); assertNull(p.level(1).bestDifficulty());
        assertEquals(5, p.totalSalvage); assertEquals(36, p.achievementProgress(Achievement.RECYCLER_I));
    }
    @Test void campaignStopsAtTenAndEarlierLevelsRemainReplayable() {
        Profile p = new Profile();
        for (int id = 1; id <= 10; id++) assertTrue(p.record(clear(id, Difficulty.NORMAL)));
        assertEquals(1, p.achievementProgress(Achievement.GUARDIAN_OF_THE_BLUE));
        assertFalse(p.canPlay(11, Difficulty.NORMAL));
        for (int id = 1; id <= 10; id++) assertTrue(p.canPlay(id, Difficulty.NORMAL));
    }
    @Test void upgradesRequireFundsHaveCapsAndChangeRunSnapshot() {
        Profile p = new Profile();
        assertFalse(p.purchase(Upgrade.HULL));
        p.totalSalvage = 1000;
        Loadout before = Loadout.from(p);
        assertTrue(p.purchase(Upgrade.HULL)); assertEquals(1000-Upgrade.HULL.cost(0), p.totalSalvage);
        assertEquals(110, Loadout.from(p).health()); assertEquals(100, before.health());
        for (int i = 1; i < 5; i++) assertTrue(p.purchase(Upgrade.HULL));
        int remaining = p.totalSalvage;
        assertFalse(p.purchase(Upgrade.HULL)); assertEquals(remaining, p.totalSalvage);
        assertEquals(150, Loadout.from(p).health());
        assertTrue(p.purchase(Upgrade.PRIMARY_WEAPON)); assertEquals(12, Loadout.from(p).damage());
        assertTrue(p.purchase(Upgrade.SHIELD)); assertEquals(10, Loadout.from(p).shieldCapacity());
    }
    @Test void selectedCrewAndVesselHaveGameplayEffectsAndIntegrityUsesMaximumHull() {
        Profile p = new Profile();
        p.selectedPilot = Pilot.NERI; p.selectedSubmarine = Submarine.LEVIATHAN;
        Loadout loadout = Loadout.from(p);
        assertEquals(140, loadout.health()); assertEquals(825, loadout.speed(), .01f);
        assertEquals(1.25f, loadout.rescueSeconds(), .001f);
        RunSpec spec = RunSpec.create(1, Difficulty.NORMAL, loadout);
        LevelResult result = new LevelResult(spec, true, 0, 0, 0, 0, 140);
        assertEquals(100, result.integrity);
        p.selectedPilot = Pilot.KAIA; p.selectedSubmarine = Submarine.MANTA;
        assertTrue(Loadout.from(p).cleanupSeconds() < .42f / 1.2f); assertEquals(85, Loadout.from(p).health());
    }
    @Test void ecologyAndCombatDenominatorsAreIndependentOfDifficultyDensity() {
        RunSpec abyss = RunSpec.create(2, Difficulty.ABYSS, Loadout.standard());
        var mission=abyss.mission();
        MissionOutcome outcome=new MissionOutcome(40,50,12,mission.wasteCount/2,1,0,100,0,0,1000,.5f);
        LevelResult r = new LevelResult(abyss,true,outcome);
        assertEquals(80,r.combat,.001);
        assertEquals(50,r.cleanup,.001); assertEquals(100f/mission.creatureCount,r.rescue,.001);
    }
    @Test void countersCannotOverflowAndCorruptUnlockFlagsCannotBypassProgression() {
        Profile p = new Profile(); p.totalSalvage = Integer.MAX_VALUE; p.completedRuns = Integer.MAX_VALUE;
        p.record(clear(1, Difficulty.NORMAL));
        assertEquals(Integer.MAX_VALUE, p.totalSalvage); assertEquals(Integer.MAX_VALUE, p.completedRuns);
        p.level(8).unlocked = true;
        p.level(1).completedDifficulties = Difficulty.NORMAL.bit() | Difficulty.EXPERT.bit();
        p.normalize();
        assertFalse(p.canPlay(8, Difficulty.NORMAL)); assertFalse(p.canPlay(1, Difficulty.ABYSS));
    }
}
