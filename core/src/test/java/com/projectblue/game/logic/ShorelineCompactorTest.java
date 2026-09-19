package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShorelineCompactorTest {
    @Test void threePhasesEnforceWarningsPipesAndCoreDamageGates() {
        MissionConfig.Boss config=MissionConfig.BLUE_COAST.boss;
        ShorelineCompactor boss=new ShorelineCompactor(config,1,1);
        assertEquals(ShorelineCompactor.State.DORMANT,boss.state());
        boss.start(); assertEquals(ShorelineCompactor.State.ARRIVAL,boss.state());
        boss.update(config.arrivalSeconds());
        assertEquals(ShorelineCompactor.State.DISCHARGE,boss.state()); assertTrue(boss.coreVulnerable());
        int first=boss.hitCore(Integer.MAX_VALUE);
        assertEquals(boss.maxHealth()/3,first); assertEquals(ShorelineCompactor.State.PRESS_WARNING,boss.state());
        assertEquals(0,boss.hitCore(100)); assertTrue(boss.telegraphing());
        boss.update(config.telegraphSeconds());
        assertEquals(ShorelineCompactor.State.PRESS_ACTIVE,boss.state()); assertTrue(boss.pressesActive());
        boss.hitCore(Integer.MAX_VALUE); assertEquals(ShorelineCompactor.State.PIPE_WARNING,boss.state());
        assertEquals(0,boss.hitPipe(true,100));
        boss.update(config.telegraphSeconds()); assertEquals(ShorelineCompactor.State.PIPES,boss.state());
        assertEquals(0,boss.hitCore(100));
        boss.hitPipe(true,Integer.MAX_VALUE); assertEquals(ShorelineCompactor.State.PIPES,boss.state());
        boss.hitPipe(false,Integer.MAX_VALUE); assertEquals(ShorelineCompactor.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated()); assertEquals(0,boss.phase());
    }
    @Test void attacksExposeConsumableTelegraphsAndRespectDroneBudget() {
        MissionConfig.Boss config=MissionConfig.BLUE_COAST.boss;
        ShorelineCompactor boss=new ShorelineCompactor(config,1,1);
        boss.start(); boss.update(config.arrivalSeconds());
        int drones=0;
        for(int i=0;i<config.droneBudget()+3;i++) {
            boss.update(boss.interval());
            assertTrue(boss.consumeVolley()); assertFalse(boss.consumeVolley());
            if(boss.consumeDrone()) drones++;
        }
        assertEquals(config.droneBudget(),drones);
    }
}
