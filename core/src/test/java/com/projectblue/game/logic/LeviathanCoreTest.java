package com.projectblue.game.logic;

import com.projectblue.game.config.CampaignConfig;
import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.config.MissionConfig;
import com.projectblue.game.save.Profile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

class LeviathanCoreTest {
    private static void advance(LeviathanCore boss,LeviathanCore.State target) {
        int guard=2000;
        while (boss.state()!=target && guard-->0) boss.update(.05f,false);
        assertEquals(target,boss.state());
    }
    private static LeviathanCore reachEscape(Difficulty difficulty) {
        var tuning=CampaignConfig.DEFAULT.tuning(difficulty);
        LeviathanCore boss=new LeviathanCore(MissionConfig.NEREID_CORE.boss,difficulty,tuning.health(),tuning.bossCadence());
        boss.start(); advance(boss,LeviathanCore.State.ARCHIVE_ASSAULT);
        boss.hitCore(Integer.MAX_VALUE); advance(boss,LeviathanCore.State.SHIELD_GENERATORS);
        boss.hitGenerator(true,Integer.MAX_VALUE); boss.hitGenerator(false,Integer.MAX_VALUE);
        advance(boss,LeviathanCore.State.RESTORATION_SYSTEMS);
        for (int i=0;i<boss.cleanupRequired();i++) assertTrue(boss.recordCleanup());
        for (int i=0;i<boss.rescueRequired();i++) assertTrue(boss.recordRescue());
        for (int i=0;i<boss.sonarRequired();i++) assertTrue(boss.recordSonarPulse());
        advance(boss,LeviathanCore.State.CORE_EXPOSED);
        boss.hitCore(Integer.MAX_VALUE); advance(boss,LeviathanCore.State.ESCAPE);
        return boss;
    }

    @Test void fourPhasesRequireCombatShieldsCleanupSonarRescueAndEscape() {
        LeviathanCore boss=reachEscape(Difficulty.NORMAL);
        assertEquals(4,boss.phase()); assertFalse(boss.defeated());
        for (int i=0;i<10;i++) boss.update(.05f,false);
        assertEquals(LeviathanCore.State.ESCAPE,boss.state());
        int guard=500;
        while (!boss.defeated() && guard-->0) boss.update(.05f,true);
        assertTrue(boss.defeated()); assertEquals(LeviathanCore.State.ESCAPED,boss.state());
    }

    @Test void escapeCountdownFailsWhenThePlayerDoesNotReachTheExit() {
        LeviathanCore boss=reachEscape(Difficulty.EXPERT);
        boss.update(boss.escapeLimit()+.1f,false);
        assertTrue(boss.escapeFailed()); assertFalse(boss.defeated());
    }

    @Test void everyArchivedAttackHasAReadableWarningBeforeItCanFire() {
        var tuning=CampaignConfig.DEFAULT.tuning(Difficulty.ABYSS);
        LeviathanCore boss=new LeviathanCore(MissionConfig.NEREID_CORE.boss,Difficulty.ABYSS,tuning.health(),tuning.bossCadence());
        boss.start(); advance(boss,LeviathanCore.State.ARCHIVE_ASSAULT);
        EnumSet<LeviathanCore.Attack> patterns=EnumSet.noneOf(LeviathanCore.Attack.class);
        for (int attackIndex=0;attackIndex<8;attackIndex++) {
            int guard=2000;
            while (boss.warningAttack()==null && guard-->0) boss.update(.05f,false);
            assertNotNull(boss.warningAttack());
            patterns.add(boss.warningAttack());
            assertTrue(boss.warningRemaining()>=.6f);
            assertNull(boss.consumeAttack());
            LeviathanCore.Attack fired=null;
            while (fired==null && guard-->0) { boss.update(.05f,false); fired=boss.consumeAttack(); }
            assertNotNull(fired);
        }
        assertTrue(patterns.containsAll(EnumSet.of(LeviathanCore.Attack.ARCHIVE_FAN,
            LeviathanCore.Attack.NET_CROSS,LeviathanCore.Attack.OIL_SURGE,LeviathanCore.Attack.SONAR_RING)));
    }

    @ParameterizedTest @EnumSource(Difficulty.class)
    void difficultyChangesPatternsInteractionLoadAndEscapeWithoutUpgradeRequirements(Difficulty difficulty) {
        Profile fresh=new Profile();
        Loadout base=Loadout.from(fresh);
        assertTrue(base.damage()>0); assertTrue(base.cleanupRadius()>0); assertTrue(base.rescueSeconds()>0);
        var tuning=CampaignConfig.DEFAULT.tuning(difficulty);
        LeviathanCore boss=new LeviathanCore(MissionConfig.NEREID_CORE.boss,difficulty,tuning.health(),tuning.bossCadence());
        int ordinal=difficulty.ordinal();
        assertEquals(Math.min(4,2+ordinal),boss.cleanupRequired());
        assertEquals(Math.min(2,1+ordinal/2),boss.rescueRequired());
        assertEquals(Math.min(2,1+ordinal/2),boss.sonarRequired());
        assertEquals(1+ordinal,boss.attackComplexity());
        assertEquals(MissionConfig.NEREID_CORE.boss.escapeSeconds()*(1-ordinal*.1f),boss.escapeLimit(),.001f);
        assertTrue(MissionConfig.NEREID_CORE.sonar.maxEnergy() >= boss.sonarRequired()*MissionConfig.NEREID_CORE.sonar.pulseCost());
    }
}
