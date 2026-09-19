package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class AuthoredMissionWorldTest {
    private GameWorld world(int level) {
        return new GameWorld(RandomProvider.seeded(41),RunSpec.create(level,Difficulty.NORMAL,Loadout.standard()));
    }
    private void advance(GameWorld world,float target) {
        while (world.elapsed()<target && !world.finished()) {
            world.player.health=world.player.maxHealth; world.update(STEP,false,0,0);
        }
    }
    @Test void playerFireDamagesCoralButPassesRescueCreaturesSafely() {
        GameWorld world=world(2);
        Entity coral=world.corals.obtain(); coral.x=270; coral.y=400; coral.radius=42; coral.health=coral.maxHealth=30;
        Entity creature=world.turtles.obtain(); creature.x=120; creature.y=400; creature.radius=30;
        creature.creature=world.mission().creature(MissionConfig.CreatureKind.MANTA);
        world.playerProjectile(coral.x,coral.y,0,0,10,0);
        world.playerProjectile(creature.x,creature.y,0,0,10,0);
        world.update(STEP,false,0,0);
        assertEquals(20,coral.health); assertEquals(10,world.coralDamage());
        assertTrue(creature.active); assertFalse(creature.friendly);
    }
    @Test void ghostCurrentMovesNetsAndTimedRescuesExpireWithoutADeathState() {
        GameWorld world=world(3);
        Entity net=world.plastics.obtain(); net.x=300; net.y=700; net.radius=35; net.vy=0;
        net.waste=world.mission().waste(MissionConfig.WasteKind.NET);
        Entity creature=world.turtles.obtain(); creature.x=80; creature.y=700; creature.radius=25; creature.vy=0;
        creature.creature=world.mission().creature(MissionConfig.CreatureKind.TURTLE); creature.lifetime=.05f;
        float before=net.x;
        for (int i=0;i<5;i++) world.update(STEP,false,0,0);
        assertNotEquals(before,net.x); assertFalse(creature.active); assertEquals(0,world.rescueCount());
    }
    @Test void coralCutterTargetsAndDamagesTheNearestProtectedCoral() {
        GameWorld world=world(2);
        Entity coral=world.corals.obtain(); coral.x=270; coral.y=680; coral.radius=42; coral.health=coral.maxHealth=30;
        Entity cutter=world.drones.obtain(); cutter.enemy=world.mission().enemy("CORAL_CUTTER");
        cutter.x=cutter.originX=270; cutter.y=680; cutter.radius=25; cutter.health=cutter.maxHealth=1000; cutter.repairTimer=0;
        world.update(STEP,false,0,0);
        assertTrue(coral.health<coral.maxHealth); assertTrue(world.coralDamage()>0);
    }
    @Test void shieldCarrierTemporarilyReducesDamageToNearbyEnemies() {
        GameWorld world=world(2);
        Entity carrier=world.drones.obtain(); carrier.enemy=world.mission().enemy("SHIELD_CARRIER");
        carrier.x=carrier.originX=100; carrier.y=500; carrier.radius=29; carrier.health=carrier.maxHealth=200; carrier.repairTimer=0;
        Entity ally=world.drones.obtain(); ally.enemy=world.mission().enemy("BURROW_DRONE");
        ally.x=ally.originX=180; ally.y=500; ally.radius=21; ally.health=ally.maxHealth=100;
        world.update(STEP,false,0,0);
        assertTrue(ally.shieldTime>0);
        world.playerProjectile(ally.x,ally.y,0,0,12,0); world.update(STEP,false,0,0);
        assertEquals(96,ally.health);
    }
    @Test void reefBreakerAndHarvesterCompleteOnlyAfterTheirDamageGates() {
        GameWorld coral=world(2); advance(coral,coral.mission().boss.start()+.1f);
        ReefBreaker reef=coral.reefBreaker();
        while (reef.state()!=ReefBreaker.State.CUTTER_SWEEP) coral.update(STEP,false,0,0);
        reef.hitCore(Integer.MAX_VALUE);
        while (reef.state()!=ReefBreaker.State.GENERATORS) coral.update(STEP,false,0,0);
        assertEquals(0,reef.hitCore(100));
        reef.hitGenerator(true,Integer.MAX_VALUE); reef.hitGenerator(false,Integer.MAX_VALUE);
        while (reef.state()!=ReefBreaker.State.CORE_EXPOSED) coral.update(STEP,false,0,0);
        reef.hitCore(Integer.MAX_VALUE); coral.update(STEP,false,0,0);
        assertTrue(coral.recovering()); advance(coral,coral.elapsed()+coral.mission().recoverySeconds+.1f);
        assertTrue(coral.result().completed);

        GameWorld nets=world(3); advance(nets,nets.mission().boss.start()+.1f);
        GhostNetHarvester harvester=nets.harvester();
        while (harvester.state()!=GhostNetHarvester.State.NET_BARRAGE) nets.update(STEP,false,0,0);
        harvester.hitCore(Integer.MAX_VALUE);
        while (harvester.state()!=GhostNetHarvester.State.NET_WALLS) nets.update(STEP,false,0,0);
        harvester.hitCore(Integer.MAX_VALUE);
        while (harvester.state()!=GhostNetHarvester.State.GENERATORS) nets.update(STEP,false,0,0);
        assertEquals(0,harvester.hitCore(100));
        harvester.hitGenerator(true,Integer.MAX_VALUE); harvester.hitGenerator(false,Integer.MAX_VALUE);
        harvester.hitCore(Integer.MAX_VALUE); nets.update(STEP,false,0,0);
        assertTrue(nets.recovering()); advance(nets,nets.elapsed()+nets.mission().recoverySeconds+.1f);
        assertTrue(nets.result().completed);
    }
    @Test void urbanSalvagerAndOilKrakenCompleteThroughTheirWorldDamageGates() {
        GameWorld city=world(4); advance(city,city.mission().boss.start()+.1f);
        UrbanSalvager salvager=city.urbanSalvager();
        while (salvager.state()!=UrbanSalvager.State.SCRAP_VOLLEY) city.update(STEP,false,0,0);
        salvager.hitCore(Integer.MAX_VALUE);
        while (salvager.state()!=UrbanSalvager.State.ARMOR_PLATES) city.update(STEP,false,0,0);
        salvager.hitPlate(true,Integer.MAX_VALUE); salvager.hitPlate(false,Integer.MAX_VALUE);
        salvager.hitCore(Integer.MAX_VALUE); city.update(STEP,false,0,0);
        assertTrue(city.recovering()); advance(city,city.elapsed()+city.mission().recoverySeconds+.1f);
        assertTrue(city.result().completed);

        GameWorld tide=world(5); advance(tide,tide.mission().boss.start()+.1f);
        OilKraken kraken=tide.oilKraken();
        while (kraken.state()!=OilKraken.State.PIPE_ARMS) tide.update(STEP,false,0,0);
        kraken.hitCore(Integer.MAX_VALUE);
        while (kraken.state()!=OilKraken.State.VALVES) tide.update(STEP,false,0,0);
        kraken.closeValve(true); kraken.closeValve(false);
        for (int i=0;i<tide.hazards.capacity();i++) if (tide.hazards.at(i).value==1) tide.hazards.at(i).active=false;
        tide.update(STEP,false,0,0);
        assertEquals(OilKraken.State.CORE_EXPOSED,kraken.state());
        kraken.hitCore(Integer.MAX_VALUE); tide.update(STEP,false,0,0);
        assertTrue(tide.recovering()); advance(tide,tide.elapsed()+tide.mission().recoverySeconds+.1f);
        assertTrue(tide.result().completed);
    }
}
