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

    @Test void reducedEffectsCutsCosmeticParticlesWithoutChangingPoolCapacity() {
        GameWorld full=destroyOneDrone(false), reduced=destroyOneDrone(true);
        assertEquals(PARTICLES_PER_BURST,full.particles.activeCount());
        assertEquals(PARTICLES_PER_BURST/3,reduced.particles.activeCount());
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
