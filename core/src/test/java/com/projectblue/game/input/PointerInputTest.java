package com.projectblue.game.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.config.RunSpec;
import com.projectblue.game.logic.GameWorld;
import com.projectblue.game.logic.RandomProvider;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PointerInputTest {
    private final GameWorld world = new GameWorld(RandomProvider.seeded(1),
        RunSpec.create(6, Difficulty.NORMAL, Loadout.standard()));
    private final AtomicInteger pauses = new AtomicInteger();
    // Deterministic screen-to-world projection without a GPU or device density dependency.
    private final Viewport viewport = new Viewport() {
        @Override public Vector2 unproject(Vector2 point) { return point.set(point.x, 960 - point.y); }
    };
    private final PointerInput input = new PointerInput(viewport, world, pauses::incrementAndGet);

    @Test void dragIsRelativeAndCancellationReleasesTheCapturedPointer() {
        assertTrue(input.touchDown(100, 600, 0, Input.Buttons.LEFT));
        assertEquals(world.player.x, input.targetX());
        assertEquals(world.player.y, input.targetY());
        assertTrue(input.touchDragged(130, 580, 0));
        assertEquals(world.player.x + 30, input.targetX());
        assertEquals(world.player.y + 20, input.targetY());
        assertFalse(input.touchDown(220, 620, 1, Input.Buttons.LEFT));
        assertFalse(input.touchUp(220, 620, 1, Input.Buttons.LEFT));
        assertTrue(input.moving());
        assertTrue(input.touchCancelled(130, 580, 0, Input.Buttons.LEFT));
        assertFalse(input.moving());
    }

    @Test void secondFingerCanUseSonarAndPauseWithoutStealingMovement() {
        assertTrue(input.touchDown(100, 600, 0, Input.Buttons.LEFT));
        float energy = world.sonarEnergy();
        assertTrue(input.touchDown(450, 280, 1, Input.Buttons.LEFT));
        assertTrue(world.sonarEnergy() < energy);
        assertTrue(input.moving());
        assertFalse(input.touchDragged(460, 290, 1));
        assertFalse(input.touchUp(450, 280, 1, Input.Buttons.LEFT));
        assertTrue(input.touchDown(480, 50, 1, Input.Buttons.LEFT));
        assertEquals(1, pauses.get());
        assertTrue(input.touchDragged(140, 600, 0));
        assertEquals(world.player.x + 40, input.targetX());
    }

    @Test void letterboxingAndNonPrimaryMouseButtonsCannotCaptureMovement() {
        assertFalse(input.touchDown(-1, 500, 0, Input.Buttons.LEFT));
        assertFalse(input.touchDown(541, 500, 0, Input.Buttons.LEFT));
        assertFalse(input.touchDown(200, 500, 0, Input.Buttons.RIGHT));
        assertFalse(input.moving());
        assertTrue(input.keyDown(Input.Keys.BACK));
        assertEquals(1, pauses.get());
    }
}
