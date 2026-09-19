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
            config.replace("level.10.name=NEREID Core", "")}) {
            assertThrows(IOException.class, () -> CampaignConfig.read(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8))));
        }
    }
    @ParameterizedTest @EnumSource(Difficulty.class)
    void simulationConsumesSpawnHealthAndBulletSpeedData(Difficulty difficulty) {
        RunSpec spec = RunSpec.create(10, difficulty, Loadout.standard());
        GameWorld w = new GameWorld(() -> .2f, spec);
        while (w.elapsed()<13) { w.player.health=PLAYER_HEALTH; w.update(STEP, false, 0, 0); }
        Entity drone = w.drones.at(0);
        assertTrue(drone.active);
        assertEquals(Math.round(spec.mission().enemy("DEFENSE_DRONE").stats().health()*spec.tuning().health()),drone.health);
        drone.x = 90; drone.y = 550; drone.timer = 0;
        w.update(STEP, false, 0, 0);
        Entity hostile = null;
        for (int i = 0; i < w.bullets.capacity(); i++) {
            Entity b = w.bullets.at(i);
            if (b.active && !b.friendly) { hostile = b; break; }
        }
        assertNotNull(hostile);
        assertEquals(spec.mission().enemy("DEFENSE_DRONE").stats().bulletSpeed()*spec.tuning().bulletSpeed(),
            Math.hypot(hostile.vx, hostile.vy), .001);
        while (w.elapsed() < 30) { w.player.health = PLAYER_HEALTH; w.update(STEP, false, 0, 0); }
        assertTrue(w.spawnedDrones()>=3);
    }
    @ParameterizedTest @EnumSource(Difficulty.class)
    void bossUsesConfiguredFanPhasesAndMovement(Difficulty difficulty) {
        CampaignConfig.Tuning tuning=CampaignConfig.DEFAULT.tuning(difficulty);
        LeviathanCore boss=new LeviathanCore(MissionConfig.NEREID_CORE.boss,difficulty,tuning.health(),tuning.bossCadence());
        boss.start();
        while (boss.state()!=LeviathanCore.State.ARCHIVE_ASSAULT) boss.update(.1f,false);
        assertEquals(Math.round(MissionConfig.NEREID_CORE.boss.coreHealth()*tuning.health()),boss.maxHealth());
        assertEquals(1,boss.phase()); assertEquals(1+difficulty.ordinal(),boss.attackComplexity());
        assertEquals(MissionConfig.NEREID_CORE.boss.attackInterval()/tuning.bossCadence(),boss.interval(),.001f);
    }
    @Test void survivingAnUndefeatedBossDoesNotCompleteTheMission() {
        GameWorld w = new GameWorld(() -> .2f, RunSpec.create(10, Difficulty.NORMAL, Loadout.standard()));
        while (!w.finished()) {
            w.player.health = PLAYER_HEALTH;
            if (w.boss.active) w.boss.health = w.boss.maxHealth;
            w.update(STEP, false, 0, 0);
        }
        assertFalse(w.result().completed); assertEquals(0, w.result().stars);
    }
    @Test void finalCoreCannotBeCompletedByDamageAlone() {
        LeviathanCore boss=new LeviathanCore(MissionConfig.NEREID_CORE.boss,Difficulty.NORMAL,1,1);
        boss.start();
        while (boss.state()!=LeviathanCore.State.ARCHIVE_ASSAULT) boss.update(.1f,false);
        boss.hitCore(Integer.MAX_VALUE);
        assertEquals(LeviathanCore.State.SHIELD_WARNING,boss.state());
        assertFalse(boss.defeated());
    }
}
