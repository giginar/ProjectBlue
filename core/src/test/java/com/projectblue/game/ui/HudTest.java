package com.projectblue.game.ui;

import com.projectblue.game.logic.GameWorld;
import com.projectblue.game.logic.RandomProvider;
import org.junit.jupiter.api.Test;

import static com.projectblue.game.config.GameConfig.STEP;
import static org.junit.jupiter.api.Assertions.*;

final class HudTest {
    @Test void rewardedContinueRefreshesCachedHullLabel() {
        GameWorld world = new GameWorld(RandomProvider.seeded(7));
        Hud hud = new Hud(null);
        world.player.health = 0;
        world.update(STEP, false, 0, 0);
        assertTrue(world.finished());
        hud.update(0, world);
        assertEquals("HULL 0/100", hud.healthText());

        assertTrue(world.continueAfterFailure());
        hud.refresh(world);
        assertEquals("HULL 100/100", hud.healthText());
    }
}
