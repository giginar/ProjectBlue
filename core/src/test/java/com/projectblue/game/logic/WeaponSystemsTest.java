package com.projectblue.game.logic;
import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.save.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class WeaponSystemsTest {
    private Profile prepared() {
        Profile p = new Profile();
        for (int i = 1; i <= 5; i++) p.record(new LevelResult(RunSpec.create(i,Difficulty.NORMAL,Loadout.standard()),true,30,0,0,0,100));
        return p;
    }
    private GameWorld world(Weapon weapon) {
        Profile p = prepared(); p.selectedWeapon = weapon;
        return new GameWorld(() -> .1f,RunSpec.create(1,Difficulty.NORMAL,Loadout.from(p)));
    }
    private Entity enemy(GameWorld w,float x,float y,int hp) {
        Entity e = w.drones.obtain(); assertNotNull(e);
        e.x = x; e.y = y; e.radius = DRONE_RADIUS; e.health = e.maxHealth = hp; e.timer = 999; return e;
    }
    private void step(GameWorld w,int count) { for (int i = 0; i < count; i++) w.update(STEP,false,0,0); }
    @Test void pulseKeepsOriginalSingleShotDamageAndCadence() {
        GameWorld w = world(Weapon.PULSE_CANNON); step(w,1);
        assertEquals(1,w.bullets.activeCount()); Entity b = w.bullets.at(0);
        assertEquals(10,b.damage); assertEquals(0,b.vx); assertEquals(680,b.vy);
        step(w,10); assertEquals(2,w.bullets.activeCount());
    }
    @Test void spreadHasThreeDistinctLanesWithReducedDamage() {
        GameWorld w = world(Weapon.SPREAD_CANNON); step(w,1);
        assertEquals(3,w.bullets.activeCount()); assertTrue(w.bullets.at(0).vx > 0);
        assertEquals(0,w.bullets.at(1).vx,.001f); assertTrue(w.bullets.at(2).vx < 0);
        assertEquals(6,w.bullets.at(0).damage);
    }
    @Test void laserHitsNearestInLaneAndDoesNotDamageOffAxisTargets() {
        GameWorld w = world(Weapon.FOCUS_LASER);
        Entity near = enemy(w,w.player.x,w.player.y+120,10);
        Entity behind = enemy(w,w.player.x,w.player.y+240,10);
        Entity side = enemy(w,w.player.x+100,w.player.y+80,10);
        step(w,1);
        assertEquals(5,near.health); assertEquals(10,behind.health); assertEquals(10,side.health);
        assertEquals(0,w.bullets.activeCount()); assertTrue(w.laser.timer > 0);
        step(w,8); assertFalse(near.active); assertEquals(1,w.kills());
    }
    @Test void homingChangesDirectionAndReacquiresWhenTargetDisappears() {
        GameWorld w = world(Weapon.HOMING_MICRO_TORPEDO);
        Entity target = enemy(w,w.player.x+140,w.player.y+260,100);
        step(w,1); Entity b = w.bullets.at(0); assertTrue(b.tracking > 0); assertTrue(b.vx > 0);
        target.active = false; enemy(w,w.player.x-140,w.player.y+260,100);
        step(w,20); assertTrue(b.vx < 0); assertEquals(430,Math.hypot(b.vx,b.vy),.1);
    }
    @Test void supportDroneAimsFromItsOrbitAndAuxiliaryUpgradeAddsFirepower() {
        GameWorld drone = world(Weapon.SUPPORT_DRONE);
        enemy(drone,drone.player.x+120,drone.player.y+200,100); step(drone,1);
        assertTrue(drone.bullets.at(0).x > drone.player.x);
        assertTrue(drone.bullets.at(0).vx > 0);
        SaveService saves = new SaveService(new SaveStore() {
            public String read() { return null; } public void write(String s) {}
        });
        saves.profile().totalSalvage = 50; saves.purchase(Upgrade.SUPPORT_DRONE);
        GameWorld auxiliary = new GameWorld(() -> .1f,RunSpec.create(1,Difficulty.NORMAL,Loadout.from(saves.profile())));
        step(auxiliary,1); assertEquals(2,auxiliary.bullets.activeCount());
        assertEquals(10,auxiliary.bullets.at(0).damage); assertEquals(2,auxiliary.bullets.at(1).damage);
    }
    @Test void shieldAbsorbsDamageButCannotFalselyAwardUntouched() {
        Profile p = prepared(); p.selectedSubmarine = Submarine.LEVIATHAN;
        GameWorld w = new GameWorld(() -> .1f,RunSpec.create(1,Difficulty.NORMAL,Loadout.from(p)));
        Entity b = w.bullets.obtain(); b.x = w.player.x; b.y = w.player.y; b.radius = BULLET_RADIUS;
        step(w,1);
        assertEquals(140,w.player.health); assertEquals(12,w.shield()); assertEquals(8,w.damageTaken());
        step(w,40); b = w.bullets.obtain(); b.x = w.player.x; b.y = w.player.y; b.radius = BULLET_RADIUS; step(w,1);
        step(w,40); b = w.bullets.obtain(); b.x = w.player.x; b.y = w.player.y; b.radius = BULLET_RADIUS; step(w,1);
        assertEquals(0,w.shield()); assertEquals(136,w.player.health); assertEquals(24,w.damageTaken());
        Profile results = new Profile();
        results.record(new LevelResult(w.spec(),true,0,0,0,0,140,w.damageTaken()));
        assertFalse(results.achievementUnlocked(Achievement.UNTOUCHED));
    }
    @Test void pilotBonusesAreSpecializedAndVesselsTradeSpeedForHull() {
        Profile p = prepared(); p.selectedPilot = Pilot.KAIA; Loadout kaia = Loadout.from(p);
        assertEquals(100,kaia.health()); assertEquals(10,kaia.damage()); assertEquals(CLEAN_SECONDS/1.15f,kaia.cleanupSeconds(),.001);
        p.selectedPilot = Pilot.ATLAS; assertEquals(115,Loadout.from(p).health());
        p.selectedPilot = Pilot.NERI; assertEquals(1.25,Loadout.from(p).rescueSeconds(),.001);
        p.selectedPilot = Pilot.ROOK; assertEquals(11,Loadout.from(p).damage());
        p.selectedSubmarine = Submarine.MANTA; Loadout manta = Loadout.from(p);
        p.selectedSubmarine = Submarine.LEVIATHAN; Loadout leviathan = Loadout.from(p);
        assertTrue(manta.speed() > leviathan.speed()); assertTrue(manta.health() < leviathan.health());
        assertTrue(manta.cleanupSeconds() < leviathan.cleanupSeconds()); assertTrue(manta.damage() < leviathan.damage());
    }
    @Test void cleanupAndRescueUpgradesChangeActualCompletionTimes() {
        SaveService saves = new SaveService(new SaveStore() {
            public String read() { return null; } public void write(String s) {}
        });
        saves.profile().totalSalvage = 1000;
        for (int i = 0; i < 5; i++) { saves.purchase(Upgrade.CLEANUP_BEAM); saves.purchase(Upgrade.RESCUE_SYSTEM); }
        GameWorld w = new GameWorld(() -> .1f,RunSpec.create(1,Difficulty.NORMAL,Loadout.from(saves.profile())));
        Entity plastic = w.plastics.obtain(); plastic.x = w.player.x; plastic.y = w.player.y;
        Entity turtle = w.turtles.obtain(); turtle.x = w.player.x; turtle.y = w.player.y;
        step(w,14); assertEquals(1,w.plasticCount());
        step(w,47); assertEquals(1,w.rescueCount());
    }
    @ParameterizedTest @EnumSource(Weapon.class)
    void weaponsStayWithinPoolsAndRunSnapshotCannotChangeMidDive(Weapon weapon) {
        Profile p = prepared(); p.selectedWeapon = weapon;
        Loadout snapshot = Loadout.from(p);
        GameWorld w = new GameWorld(() -> .1f,RunSpec.create(1,Difficulty.NORMAL,snapshot));
        p.selectedWeapon = Weapon.PULSE_CANNON; p.selectedSubmarine = Submarine.LEVIATHAN;
        step(w,600);
        assertEquals(weapon.name(),w.spec().loadout().weapon().id()); assertEquals(100,w.player.maxHealth);
        assertTrue(w.bullets.activeCount() <= BULLET_CAPACITY);
    }
}
