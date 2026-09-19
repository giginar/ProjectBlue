package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static com.projectblue.game.config.GameConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseCandidateWorldTest {
    static Stream<Arguments> missions() {
        return IntStream.rangeClosed(1, CampaignConfig.LEVEL_COUNT).boxed()
            .flatMap(id -> Stream.of(Difficulty.values()).map(difficulty -> Arguments.of(id, difficulty)));
    }

    @ParameterizedTest(name = "sector {0} / {1}") @MethodSource("missions")
    void fullTimelineEndsWithoutInvalidStateOrDroppedEnemyWaves(int id, Difficulty difficulty) {
        GameWorld world = new GameWorld(RandomProvider.seeded(id), RunSpec.create(id, difficulty, Loadout.standard()));
        int budget = (int)Math.ceil((world.mission().deadlineSeconds + 2) / STEP);
        // Keep hull alive to exercise the entire authored route; this is not a player balance test.
        while (!world.finished() && budget-- > 0) {
            world.player.health = world.player.maxHealth;
            world.update(STEP, false, 0, 0);
            assertTrue(Float.isFinite(world.player.x) && Float.isFinite(world.player.y));
            assertTrue(world.hostileBullets() <= world.mission().hostileBulletLimit);
        }
        assertTrue(world.finished(), "Mission must end by its authored deadline");
        assertFalse(world.result().completed, "Survival alone must not bypass boss gates");
        assertEquals(0, world.result().stars);
        int authoredEnemies = world.mission().waves().stream()
            .mapToInt(wave -> Math.max(1, Math.round(wave.count() * world.spec().tuning().spawnDensity()))).sum();
        assertTrue(world.spawnedDrones() >= authoredEnemies, "Enemy pool dropped authored waves");
    }

    @Test void rescuedWildlifeLeavesTheVortexPoolAndTheSlotCanBeReused() {
        GameWorld world = new GameWorld(RandomProvider.seeded(9), RunSpec.create(9, Difficulty.NORMAL, Loadout.standard()));
        Entity creature = world.turtles.obtain();
        assertNotNull(creature);
        creature.friendly = true;
        creature.x = WIDTH + DESPAWN_MARGIN - 1;
        creature.y = SPAWN_Y;
        for (int i = 0; i < 600 && creature.active; i++) world.update(STEP, false, 0, 0);
        assertFalse(creature.active, "Freed wildlife must not stay clamped at the vortex boundary");
        assertSame(creature, world.turtles.obtain());
    }

    @Test void laserHitsProtectedCoralBeforeEnemiesAndNeverPaysForHabitatDamage() {
        GameWorld world = new GameWorld(RandomProvider.seeded(1));
        Entity coral = world.corals.obtain();
        assertNotNull(coral);
        coral.x = world.player.x; coral.y = world.player.y + 100;
        coral.radius = 42; coral.health = coral.maxHealth = 30;
        Entity enemy = world.drones.obtain();
        assertNotNull(enemy);
        enemy.x = coral.x; enemy.y = coral.y + 150;
        enemy.radius = 20; enemy.health = enemy.maxHealth = 50;
        world.fireLaser(10);
        assertEquals(20, coral.health);
        assertEquals(50, enemy.health);
        assertEquals(10, world.coralDamage());
        world.fireLaser(30);
        assertFalse(coral.active);
        assertEquals(30, world.coralDamage());
        assertEquals(0, world.kills());
        assertEquals(0, world.salvage.activeCount());
        world.fireLaser(10);
        assertEquals(40, enemy.health);
    }
}
