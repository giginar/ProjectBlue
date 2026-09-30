package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnemySystemsTest {
    private GameWorld world() {
        return new GameWorld(RandomProvider.seeded(8),RunSpec.original());
    }
    private Entity enemy(GameWorld world,String id,float x,float y) {
        Entity entity=world.drones.obtain(); assertNotNull(entity);
        entity.enemy=world.mission().enemy(id); entity.x=entity.originX=x; entity.y=y;
        entity.radius=entity.enemy.stats().radius(); entity.health=entity.maxHealth=entity.enemy.stats().health();
        entity.timer=0; entity.repairTimer=0; return entity;
    }
    private Entity hostileShot(GameWorld world) {
        for(int i=0;i<world.bullets.capacity();i++) {
            Entity shot=world.bullets.at(i);
            if(shot.active&&!shot.friendly) return shot;
        }
        fail("Expected a hostile projectile"); return null;
    }
    @Test void movementAndWeaponPatternsComposeWithoutEnemySubclasses() {
        GameWorld singleWorld=world(); Entity scout=enemy(singleWorld,"SCOUT",270,600); float before=scout.y;
        EnemySystems.update(singleWorld,scout,.1f);
        assertTrue(scout.y<before); assertEquals(1,singleWorld.hostileBullets());

        GameWorld spreadWorld=world(); Entity sweeper=enemy(spreadWorld,"SWEEPER",270,600);
        EnemySystems.update(spreadWorld,sweeper,.2f);
        assertNotEquals(270,sweeper.x); assertEquals(3,spreadWorld.hostileBullets());

        GameWorld netWorld=world(); Entity net=enemy(netWorld,"NET_LAUNCHER",270,600);
        net.aimX=netWorld.player.x; net.aimY=netWorld.player.y; EnemySystems.update(netWorld,net,.1f);
        Entity shot=null;
        for(int i=0;i<netWorld.bullets.capacity();i++) if(netWorld.bullets.at(i).active) shot=netWorld.bullets.at(i);
        assertNotNull(shot); assertEquals(netWorld.mission().netSeconds,shot.slowSeconds);
    }
    @Test void carrierArmorHasSideWeaknessAndRepairDroneHealsNearbyAlly() {
        GameWorld world=world(); Entity carrier=enemy(world,"CARRIER",270,600);
        assertTrue(EnemySystems.armoredHit(carrier,carrier.x,carrier.y-100));
        assertFalse(EnemySystems.armoredHit(carrier,carrier.x+carrier.radius,carrier.y-100));

        Entity repair=enemy(world,"REPAIR",230,600), ally=enemy(world,"SCOUT",300,600);
        ally.health=4; int before=ally.health;
        EnemySystems.update(world,repair,.1f);
        assertEquals(before+repair.enemy.stats().repairAmount(),ally.health);
        assertTrue(repair.effectTime>0);
    }
    @Test void aimedPlatformCapturesTargetDuringItsWarningWindow() {
        GameWorld world=world(); Entity turret=enemy(world,"TURRET",200,600); turret.timer=.55f;
        EnemySystems.update(world,turret,.1f);
        assertTrue(turret.warned); assertEquals(world.player.x,turret.aimX);
        assertEquals(world.player.y,turret.aimY);
    }
    @Test void aimedVelocityUsesPlayerPositionAtFireTimeInEveryDirection() {
        float[][] targets={{420,580},{80,580},{250,760},{250,340}};
        for(float[] target:targets) {
            GameWorld world=world(); Entity turret=enemy(world,"TURRET",250,600);
            world.player.x=target[0]; world.player.y=target[1];
            EnemySystems.update(world,turret,0);
            Entity shot=hostileShot(world);
            float dx=world.player.x-shot.x,dy=world.player.y-shot.y;
            assertTrue(shot.vx*dx+shot.vy*dy>0,"Projectile must point toward the player");
            assertEquals(0,shot.vx*dy-shot.vy*dx,.02f,"Velocity must be collinear with the muzzle-to-player vector");
        }
    }
    @Test void aimedShotUsesMuzzleOffsetAndDoesNotBecomeHoming() {
        GameWorld world=world(); Entity turret=enemy(world,"TURRET",210,610);
        world.player.x=390; world.player.y=300;
        EnemySystems.update(world,turret,0);
        Entity shot=hostileShot(world);
        assertEquals(turret.x,shot.x,.001f); assertEquals(turret.y-20,shot.y,.001f);
        float vx=shot.vx,vy=shot.vy;
        world.player.x=40; world.player.y=700;
        world.update(1f/60f,false,0,0);
        assertEquals(vx,shot.vx,.001f); assertEquals(vy,shot.vy,.001f); assertEquals(0,shot.tracking,.001f);
    }
    @Test void aimedShotAtMuzzlePositionHasFiniteStraightDownFallback() {
        GameWorld world=world(); Entity turret=enemy(world,"TURRET",270,600);
        world.player.x=turret.x; world.player.y=turret.y-20;
        EnemySystems.update(world,turret,0);
        Entity shot=hostileShot(world);
        assertTrue(Float.isFinite(shot.vx)); assertTrue(Float.isFinite(shot.vy));
        assertEquals(0,shot.vx,.001f); assertTrue(shot.vy<0);
    }
    @Test void explicitSinglePatternRemainsStraight() {
        GameWorld world=world(); Entity scout=enemy(world,"SCOUT",270,600);
        world.player.x=40; world.player.y=500;
        EnemySystems.update(world,scout,0);
        Entity shot=hostileShot(world);
        assertEquals(0,shot.vx,.001f); assertTrue(shot.vy<0);
    }
}
