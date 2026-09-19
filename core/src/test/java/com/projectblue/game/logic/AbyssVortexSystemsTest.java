package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.config.MissionConfig;
import com.projectblue.game.config.RunSpec;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.STEP;
import static org.junit.jupiter.api.Assertions.*;

class AbyssVortexSystemsTest {
    @Test void pressureRisesGraduallyWarnsBeforeDamageAndRecoversInSafeWater() {
        PressureSystem pressure=new PressureSystem(MissionConfig.ABYSS_MINE.pressure,Difficulty.NORMAL);
        for (int i=0;i<60;i++) pressure.update(1f/60f,false,1);
        assertTrue(pressure.pressure()>0); assertTrue(pressure.pressure()<pressure.threshold());
        assertEquals(0,pressure.consumeDamage());
        while (!pressure.warning()) pressure.update(1f/60f,false,1);
        float warned=pressure.pressure();
        for (int i=0;i<120;i++) pressure.update(1f/60f,true,0);
        assertTrue(pressure.pressure()<warned);
    }
    @Test void cleanupComboDecaysOneStepAtATimeAndKeepsTheBestChain() {
        CleanupCombo combo=new CleanupCombo(2,1,8);
        assertEquals(1,combo.collect()); assertEquals(2,combo.collect()); assertEquals(3,combo.collect());
        combo.update(2.1f);
        assertEquals(2,combo.value()); assertEquals(3,combo.best());
        combo.update(1.1f);
        assertEquals(1,combo.value()); assertEquals(3,combo.best());
    }
    @Test void fixedStepVortexForcesAreDeterministicAndDifficultyScalesTheirPattern() {
        VortexSystem first=new VortexSystem(MissionConfig.PLASTIC_VORTEX.vortex,Difficulty.NORMAL);
        VortexSystem second=new VortexSystem(MissionConfig.PLASTIC_VORTEX.vortex,Difficulty.NORMAL);
        VortexSystem abyss=new VortexSystem(MissionConfig.PLASTIC_VORTEX.vortex,Difficulty.ABYSS);
        for (int i=0;i<600;i++) { first.update(1f/60f); second.update(1f/60f); abyss.update(1f/60f); }
        assertEquals(first.forceX(170,420),second.forceX(170,420),0);
        assertEquals(first.forceY(170,420),second.forceY(170,420),0);
        double normal=Math.hypot(first.forceX(170,420),first.forceY(170,420));
        double hard=Math.hypot(abyss.forceX(170,420),abyss.forceY(170,420));
        assertTrue(hard>normal);
        first.beginEscape(); assertTrue(first.forceY(270,170)>=MissionConfig.PLASTIC_VORTEX.vortex.escapeBoost());
    }
    @Test void abyssDensityTimelinesFitThePrewarmedGameplayPools() {
        for (int level:new int[]{8,9}) {
            GameWorld world=new GameWorld(RandomProvider.seeded(99),RunSpec.create(level,Difficulty.ABYSS,Loadout.standard()));
            while (world.elapsed()<world.mission().boss.start()-.05f) {
                world.player.health=world.player.maxHealth;
                world.update(STEP,false,0,0);
            }
            int expected=world.mission().waves().stream().mapToInt(w -> w.count()*2).sum();
            assertEquals(expected,world.spawnedDrones(),"Enemy pool exhausted in sector "+level);
            assertTrue(world.drones.activeCount()<=world.drones.capacity());
            assertTrue(world.environments.activeCount()<=world.environments.capacity());
            assertTrue(world.bullets.activeCount()<=world.bullets.capacity());
        }
    }
}
