package com.projectblue.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProjectBlueGameLifecycleTest {
    @Test void lifecycleCallbacksBeforeCreateAreSafe() {
        ProjectBlueGame game = new ProjectBlueGame(null);

        assertDoesNotThrow(game::resume);
        assertDoesNotThrow(game::pause);
    }
}
