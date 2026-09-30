package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.STEP;
import static org.junit.jupiter.api.Assertions.*;

class FormationEncounterTest {
    private static GameWorld world() {
        return new GameWorld(RandomProvider.seeded(33),RunSpec.original());
    }
    private static void advance(GameWorld world,float seconds) {
        while(world.elapsed()<seconds) { world.player.health=world.player.maxHealth; world.update(STEP,false,0,0); }
    }
    @Test void membersMoveAsAGroupButKeepIndependentDeathAndRewardState() {
        GameWorld world=world(); advance(world,2.55f);
        Entity first=null,second=null;
        for(int i=0;i<world.drones.capacity();i++) if(world.drones.at(i).active) {
            if(first==null) first=world.drones.at(i); else { second=world.drones.at(i); break; }
        }
        assertNotNull(first); assertNotNull(second); assertTrue(first.vx<0); assertTrue(second.vx<0);
        int killsBefore=world.kills();
        first.x=world.player.x; first.y=world.player.y+120; first.health=1;
        second.x=world.player.x+180; second.y=world.player.y+180; second.health=second.maxHealth;
        world.playerProjectile(first.x,first.y,0,0,1,0); world.update(STEP,false,0,0);
        assertFalse(first.active); assertTrue(second.active); assertEquals(killsBefore+1,world.kills());
        assertEquals(1,world.salvage.activeCount());
    }
    @Test void closeClearsFormationEntitiesProjectilesAndTimelineState() {
        GameWorld world=world(); advance(world,9);
        assertTrue(world.drones.activeCount()>0);
        world.close();
        assertEquals(0,world.drones.activeCount()); assertEquals(0,world.bullets.activeCount());
        int spawned=world.spawnedDrones(); world.update(2,false,0,0); assertEquals(spawned,world.spawnedDrones());
    }
}
