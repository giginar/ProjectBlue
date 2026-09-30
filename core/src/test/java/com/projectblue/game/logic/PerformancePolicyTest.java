package com.projectblue.game.logic;

import org.junit.jupiter.api.Test;
import com.projectblue.game.config.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class PerformancePolicyTest {
    private static GameWorld destroyOneDrone(boolean reduced) {
        GameWorld world=new GameWorld(()->.5f);
        world.setReducedEffects(reduced);
        Entity enemy=world.drones.obtain();
        enemy.x=world.player.x; enemy.y=world.player.y+80; enemy.radius=20;
        enemy.health=enemy.maxHealth=1; enemy.timer=999;
        world.playerProjectile(enemy.x,enemy.y,0,0,1,0);
        world.update(STEP,false,0,0);
        assertFalse(enemy.active);
        return world;
    }

    @Test void oneEnemyDeathEmitsOneEventAndOneBoundedScaledEffect() {
        GameWorld world=new GameWorld(()->.5f); int[] deaths={0};
        world.events.subscribe((type,x,y,value) -> { if(type==com.projectblue.game.events.GameEvents.Type.DRONE_DESTROYED) deaths[0]++; });
        Entity enemy=world.drones.obtain(); enemy.x=world.player.x; enemy.y=world.player.y+80;
        enemy.radius=32; enemy.health=enemy.maxHealth=1; enemy.timer=999;
        world.playerProjectile(enemy.x,enemy.y,0,0,1,0); world.update(STEP,false,0,0);
        assertEquals(1,deaths[0]); assertTrue(world.particles.activeCount()>=4); assertTrue(world.particles.activeCount()<=8);
        for(int i=0;i<world.particles.capacity();i++) if(world.particles.at(i).active) {
            assertEquals(0,world.particles.at(i).value); assertTrue(world.particles.at(i).progress>1);
        }
        world.update(STEP,false,0,0); assertEquals(1,deaths[0]);
    }

    @Test void reducedEffectsCutsCosmeticParticlesWithoutChangingPoolCapacity() {
        GameWorld full=destroyOneDrone(false), reduced=destroyOneDrone(true);
        assertEquals(5,full.particles.activeCount());
        assertEquals(1,reduced.particles.activeCount());
        assertEquals(PARTICLE_CAPACITY,reduced.particles.capacity());
    }

    @Test void missionSalvageCapAlsoBoundsDynamicAndOptionalRewards() {
        GameWorld world=new GameWorld(()->.5f,RunSpec.create(1,Difficulty.NORMAL,Loadout.standard()));
        for (int i=0;i<world.salvage.capacity();i++) {
            Entity drop=world.salvage.obtain();
            drop.x=world.player.x; drop.y=world.player.y; drop.value=10;
        }
        world.update(STEP,false,0,0);
        assertEquals(world.mission().salvageCap,world.salvageCount());
    }
}
