package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.config.MissionConfig;
import com.projectblue.game.config.RunSpec;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.STEP;
import static org.junit.jupiter.api.Assertions.*;

class NereidCoreWorldTest {
    private static void update(GameWorld world) {
        world.player.health=world.player.maxHealth;
        world.update(STEP,false,world.player.x,world.player.y);
    }
    private static void advance(GameWorld world,LeviathanCore.State state) {
        int guard=3000;
        while (world.leviathanCore().state()!=state && guard-->0) update(world);
        assertEquals(state,world.leviathanCore().state());
    }
    private static Entity active(EntityPool pool,MissionConfig.EnvironmentKind kind,int value) {
        for (int i=0;i<pool.capacity();i++) {
            Entity entity=pool.at(i);
            if (entity.active && entity.environment==kind && (value<0 || entity.value==value)) return entity;
        }
        return null;
    }
    private static Entity rescue(EntityPool pool) {
        for (int i=0;i<pool.capacity();i++) {
            Entity entity=pool.at(i);
            if (entity.active && !entity.friendly && entity.value==2) return entity;
        }
        return null;
    }

    @Test void baseLoadoutCanDisablePowerCompleteRecoverySystemsAndEscape() {
        GameWorld world=new GameWorld(RandomProvider.seeded(19),RunSpec.create(10,Difficulty.NORMAL,Loadout.standard()));
        while (world.elapsed()<world.mission().boss.start()) update(world);
        int guard=5000;
        while (!world.boss.active && guard-->0) {
            Entity station=active(world.environments,MissionConfig.EnvironmentKind.ENERGY_STATION,-1);
            if (station!=null) { world.player.x=station.x; world.player.y=station.y; }
            update(world);
        }
        assertTrue(world.boss.active); assertEquals(2,world.energyStationsDisabled());

        advance(world,LeviathanCore.State.ARCHIVE_ASSAULT);
        world.leviathanCore().hitCore(Integer.MAX_VALUE);
        advance(world,LeviathanCore.State.SHIELD_GENERATORS);
        world.leviathanCore().hitGenerator(true,Integer.MAX_VALUE);
        world.leviathanCore().hitGenerator(false,Integer.MAX_VALUE);
        advance(world,LeviathanCore.State.RESTORATION_SYSTEMS);

        while (world.leviathanCore().cleanupProgress()<world.leviathanCore().cleanupRequired() && guard-->0) {
            Entity target=active(world.hazards,MissionConfig.EnvironmentKind.OIL_FIELD,2);
            assertNotNull(target); world.player.x=target.x; world.player.y=target.y; update(world);
        }
        while (world.leviathanCore().rescueProgress()<world.leviathanCore().rescueRequired() && guard-->0) {
            Entity target=rescue(world.turtles);
            assertNotNull(target); world.player.x=target.x; world.player.y=target.y; update(world);
        }
        while (world.leviathanCore().sonarProgress()<world.leviathanCore().sonarRequired()) assertTrue(world.activateSonar());
        advance(world,LeviathanCore.State.CORE_EXPOSED);
        world.leviathanCore().hitCore(Integer.MAX_VALUE);
        advance(world,LeviathanCore.State.ESCAPE);
        world.player.y=720;
        while (!world.recovering() && !world.finished() && guard-->0) update(world);
        assertTrue(world.recovering()); assertFalse(world.leviathanCore().escapeFailed());
        while (!world.finished() && guard-->0) update(world);
        assertTrue(world.finished()); assertTrue(world.result().completed); assertTrue(world.result().stars>=1);
    }
}
