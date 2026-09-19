package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class BlueCoastWorldTest {
    private GameWorld world() {
        return new GameWorld(RandomProvider.seeded(19),RunSpec.create(1,Difficulty.NORMAL,Loadout.standard()));
    }
    private void keepAlive(GameWorld world,float target) {
        while(world.elapsed()<target && !world.finished()) {
            world.player.health=world.player.maxHealth;
            world.update(STEP,false,0,0);
        }
    }
    @Test void hostileProjectileLimitAndCleanupRiskAreApplied() {
        GameWorld limited=world();
        for(int i=0;i<140;i++) limited.hostileProjectile(20+i,500,0,-20,3,0);
        assertEquals(limited.mission().hostileBulletLimit,limited.hostileBullets());

        GameWorld free=world(),cleaning=world();
        Entity waste=cleaning.plastics.obtain(); assertNotNull(waste);
        waste.x=cleaning.player.x; waste.y=cleaning.player.y; waste.radius=12;
        waste.waste=cleaning.mission().waste(MissionConfig.WasteKind.BOTTLE);
        float target=cleaning.player.x+200;
        free.update(STEP,true,target,free.player.y);
        cleaning.update(STEP,true,target,cleaning.player.y);
        assertTrue(cleaning.cleaning()); assertTrue(cleaning.player.x<free.player.x);

        cleaning.hostileProjectile(cleaning.player.x,cleaning.player.y,0,0,0,cleaning.mission().netSeconds);
        cleaning.update(STEP,false,0,0);
        float before=cleaning.player.x;
        cleaning.update(STEP,true,target,cleaning.player.y);
        assertTrue(cleaning.slowed()); assertTrue(cleaning.player.x>before);
    }
    @Test void missionCompletesOnlyAfterAllBossGatesAndRecovery() {
        GameWorld world=world(); keepAlive(world,world.mission().boss.start()+.1f);
        assertTrue(world.boss.active); assertFalse(world.finished());
        ShorelineCompactor boss=world.compactor();
        while(boss.state()==ShorelineCompactor.State.ARRIVAL) world.update(STEP,false,0,0);
        boss.hitCore(Integer.MAX_VALUE);
        while(boss.state()==ShorelineCompactor.State.PRESS_WARNING) world.update(STEP,false,0,0);
        boss.hitCore(Integer.MAX_VALUE);
        while(boss.state()==ShorelineCompactor.State.PIPE_WARNING) world.update(STEP,false,0,0);
        assertFalse(world.finished()); boss.hitPipe(true,Integer.MAX_VALUE); boss.hitPipe(false,Integer.MAX_VALUE);
        boss.hitCore(Integer.MAX_VALUE); world.update(STEP,false,0,0);
        assertTrue(world.recovering()); assertEquals(0,world.hostileBullets());
        int spawned=world.spawnedDrones(); keepAlive(world,world.elapsed()+world.mission().recoverySeconds+.1f);
        assertTrue(world.finished()); assertTrue(world.result().completed); assertTrue(world.result().stars>=1);
        assertEquals(spawned,world.spawnedDrones()); assertTrue(world.result().salvage>=world.mission().boss.salvage());
    }
    @Test void coralDamageReducesCleanupAndIntegrityScores() {
        RunSpec spec=RunSpec.original();
        MissionOutcome safe=new MissionOutcome(10,10,10,spec.mission().wasteCount,3,20,100,0,0,1000,1);
        MissionOutcome damaged=new MissionOutcome(10,10,10,spec.mission().wasteCount,3,20,100,0,60,1000,.8f);
        LevelResult pristine=new LevelResult(spec,true,safe), harmed=new LevelResult(spec,true,damaged);
        assertTrue(harmed.cleanup<pristine.cleanup); assertTrue(harmed.integrity<pristine.integrity);
    }
    @Test void missingBossKillFailsAtTheDeadline() {
        GameWorld world=world(); keepAlive(world,world.mission().deadlineSeconds);
        assertTrue(world.finished()); assertFalse(world.result().completed); assertEquals(0,world.result().stars);
    }
}
