package com.projectblue.game.config;

import com.projectblue.game.logic.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class DifficultyTest {
    @ParameterizedTest
    @CsvSource({"NORMAL,30,175,40,4,2.2,1,1", "HARD,38,210,50,3.2,1.9130435,3,2",
        "EXPERT,47,253.75,64,2.5,1.6296296,5,3", "ABYSS,57,306.25,80,2,1.375,7,3"})
    void configuredMultipliersProduceExpectedGameplay(Difficulty difficulty, int hp, float speed, int count,
        float spawnInterval, float shotInterval, int fan, int phases) {
        CampaignConfig.Tuning t = CampaignConfig.DEFAULT.tuning(difficulty);
        assertEquals(hp, t.droneHealth()); assertEquals(speed, t.shotSpeed(), .001f);
        assertEquals(count, t.droneCount()); assertEquals(spawnInterval, t.droneInterval(), .001f);
        assertEquals(shotInterval, t.shotInterval(), .001f);
        assertEquals(fan, t.bossProjectiles()); assertEquals(phases, t.bossPhases());
    }
    @Test void higherDifficultiesIncreaseMultipleIndependentThreats() {
        CampaignConfig.Tuning previous = null;
        for (Difficulty difficulty : Difficulty.values()) {
            CampaignConfig.Tuning current = CampaignConfig.DEFAULT.tuning(difficulty);
            if (previous != null) {
                assertTrue(current.health() > previous.health());
                assertTrue(current.bulletSpeed() > previous.bulletSpeed());
                assertTrue(current.spawnDensity() > previous.spawnDensity());
                assertTrue(current.fireRate() > previous.fireRate());
                assertTrue(current.bossMovement() > previous.bossMovement());
                assertTrue(current.bossInterval() < previous.bossInterval());
                assertTrue(current.bossProjectiles() > previous.bossProjectiles());
            }
            previous = current;
        }
    }
    @Test void originalEntryPointNowUsesAuthoredBlueCoastMission() {
        RunSpec original = RunSpec.original();
        assertEquals(LEVEL_SEED, original.level().seed());
        assertSame(MissionConfig.BLUE_COAST, original.mission());
        assertEquals(MissionConfig.BLUE_COAST.enemyCount(1), original.combatTargets());
        assertTrue(original.hasBoss()); assertEquals(Loadout.standard(), original.loadout());
        assertEquals(DRONE_HEALTH, original.tuning().droneHealth());
        assertEquals(DRONE_INTERVAL, original.tuning().droneInterval());
        assertEquals(ENEMY_SHOT_INTERVAL, original.tuning().shotInterval());
    }
    @Test void tenOrderedLevelsHaveDistinctSeedsAndNames() {
        CampaignConfig c = CampaignConfig.DEFAULT;
        assertEquals(10, c.levels().size());
        assertEquals(10, c.levels().stream().map(CampaignConfig.Level::seed).distinct().count());
        assertEquals(10, c.levels().stream().map(CampaignConfig.Level::name).distinct().count());
        for (int id = 1; id <= 10; id++) { assertEquals(id, c.level(id).id()); assertFalse(c.level(id).region().isBlank()); }
        assertThrows(IllegalArgumentException.class, () -> c.level(0));
        assertThrows(IllegalArgumentException.class, () -> c.level(11));
    }
    @Test void malformedOrUnsafeConfigurationFailsAtLoadTime() throws IOException {
        String config;
        try (InputStream input = getClass().getResourceAsStream("/config/campaign.properties")) {
            config = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String bad : new String[]{config.replace("difficulty.HARD.spawnDensity=1.25", "difficulty.HARD.spawnDensity=0"),
            config.replace("difficulty.HARD.health=1.25", "difficulty.HARD.health=NaN"),
            config.replace("difficulty.HARD.bossProjectiles=3", "difficulty.HARD.bossProjectiles=2.5"),
            config.replace("level.10.name=Living Abyss", "")}) {
            assertThrows(IOException.class, () -> CampaignConfig.read(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
        }
    }
    @ParameterizedTest @EnumSource(Difficulty.class)
    void simulationConsumesSpawnHealthAndBulletSpeedData(Difficulty difficulty) {
        RunSpec spec = RunSpec.create(8, difficulty, Loadout.standard());
        GameWorld w = new GameWorld(() -> .2f, spec);
        for (int i = 0; i < 125; i++) w.update(STEP, false, 0, 0);
        assertEquals(spec.tuning().droneHealth(), w.drones.at(0).health);
        Entity drone = w.drones.at(0);
        drone.x = 90; drone.y = 550; drone.timer = 0;
        w.update(STEP, false, 0, 0);
        Entity hostile = null;
        for (int i = 0; i < w.bullets.capacity(); i++) {
            Entity b = w.bullets.at(i);
            if (b.active && !b.friendly) { hostile = b; break; }
        }
        assertNotNull(hostile);
        assertEquals(spec.tuning().shotSpeed(), Math.hypot(hostile.vx, hostile.vy), .001);
        while (w.elapsed() < 30) { w.player.health = PLAYER_HEALTH; w.update(STEP, false, 0, 0); }
        assertEquals((int) Math.floor((w.elapsed() - DRONE_FIRST) / spec.tuning().droneInterval()) + 1, w.spawnedDrones());
    }
    @ParameterizedTest @EnumSource(Difficulty.class)
    void bossUsesConfiguredFanPhasesAndMovement(Difficulty difficulty) {
        RunSpec spec = RunSpec.create(8, difficulty, Loadout.standard());
        GameWorld w = new GameWorld(() -> .2f, spec);
        while (w.elapsed() < 130.1f) { w.player.health = PLAYER_HEALTH; w.update(STEP, false, 0, 0); }
        assertTrue(w.boss.active); assertEquals(spec.tuning().bossHealth(), w.boss.maxHealth);
        assertEquals(1, w.bossPhase());
        w.boss.health = Math.round(w.boss.maxHealth * .2f);
        assertEquals(spec.tuning().bossPhases(), w.bossPhase());
        for (int i = 0; i < w.bullets.capacity(); i++) w.bullets.at(i).active = false;
        for (int i = 0; i < w.drones.capacity(); i++) w.drones.at(i).active = false;
        w.boss.timer = 0;
        float before = w.boss.x;
        w.update(STEP, false, 0, 0);
        int hostile = 0;
        for (int i = 0; i < w.bullets.capacity(); i++) if (w.bullets.at(i).active && !w.bullets.at(i).friendly) hostile++;
        assertEquals(spec.tuning().bossProjectiles() + (w.bossPhase() == 3 ? 2 : 0), hostile);
        assertNotEquals(before, w.boss.x);
        assertEquals(spec.tuning().bossInterval() / (1 + (w.bossPhase() - 1) * .15f), w.boss.timer, .001f);
    }
    @Test void survivingAnUndefeatedBossDoesNotCompleteTheMission() {
        GameWorld w = new GameWorld(() -> .2f, RunSpec.create(8, Difficulty.NORMAL, Loadout.standard()));
        while (!w.finished()) {
            w.player.health = PLAYER_HEALTH;
            if (w.boss.active) w.boss.health = w.boss.maxHealth;
            w.update(STEP, false, 0, 0);
        }
        assertFalse(w.result().completed); assertEquals(0, w.result().stars);
    }
    @Test void destroyingTheBossAwardsOneKillAndAllowsMissionCompletion() {
        GameWorld w = new GameWorld(() -> .2f, RunSpec.create(8, Difficulty.NORMAL, Loadout.standard()));
        while (!w.boss.active) { w.player.health = PLAYER_HEALTH; w.update(STEP, false, 0, 0); }
        w.boss.health = PLAYER_DAMAGE;
        Entity bullet = w.bullets.obtain(); assertNotNull(bullet);
        bullet.x = w.boss.x; bullet.y = w.boss.y; bullet.friendly = true; bullet.radius = BULLET_RADIUS;
        bullet.damage = PLAYER_DAMAGE;
        int killsBefore = w.kills();
        w.update(STEP, false, 0, 0);
        assertFalse(w.boss.active); assertEquals(killsBefore + 1, w.kills());
        while (!w.finished()) { w.player.health = PLAYER_HEALTH; w.update(STEP, false, 0, 0); }
        assertFalse(w.boss.active); assertTrue(w.result().completed); assertTrue(w.result().stars >= 1);
    }
}
