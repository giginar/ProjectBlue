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
}
