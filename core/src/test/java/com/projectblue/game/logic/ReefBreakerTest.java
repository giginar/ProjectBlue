package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReefBreakerTest {
    private static void advance(ReefBreaker boss,ReefBreaker.State state) {
        for (int i=0;i<600 && boss.state()!=state;i++) boss.update(1f/60f);
        assertEquals(state,boss.state());
    }
    @Test void generatorsGateTheCoreAndFinalCoralStrikeIsTelegraphed() {
        ReefBreaker boss=new ReefBreaker(MissionConfig.CORAL_GARDENS.boss,1,1);
        boss.start(); advance(boss,ReefBreaker.State.CUTTER_SWEEP);
        assertEquals(1,boss.phase());
        boss.hitCore(Integer.MAX_VALUE);
        assertEquals(ReefBreaker.State.GENERATOR_WARNING,boss.state());
        assertEquals(0,boss.hitCore(100));
        advance(boss,ReefBreaker.State.GENERATORS);
        boss.hitGenerator(true,Integer.MAX_VALUE);
        assertFalse(boss.coreVulnerable());
        boss.hitGenerator(false,Integer.MAX_VALUE);
        assertEquals(ReefBreaker.State.CORAL_WARNING,boss.state());
        assertTrue(boss.telegraphing());
        advance(boss,ReefBreaker.State.CORE_EXPOSED);
        assertTrue(boss.consumeCoralStrike());
        assertEquals(3,boss.phase());
        boss.hitCore(Integer.MAX_VALUE);
        assertTrue(boss.defeated());
    }
}
