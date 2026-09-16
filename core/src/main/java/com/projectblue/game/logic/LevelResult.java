package com.projectblue.game.logic;

import static com.projectblue.game.config.GameConfig.*;

public final class LevelResult {
    public final boolean completed;
    public final int score, salvage, stars;
    public final float combat, cleanup, rescue, integrity;
    public LevelResult(boolean completed, int kills, int plastic, int turtles, int salvage, int health) {
        this.completed = completed;
        this.salvage = salvage;
        combat = Rules.percentage(kills, DRONE_COUNT);
        cleanup = Rules.cleanup(plastic);
        rescue = Rules.rescue(turtles);
        integrity = Rules.percentage(health, PLAYER_HEALTH);
        score = Rules.score(kills, plastic, turtles, salvage, health, completed);
        stars = Rules.stars(completed, combat, cleanup, rescue, integrity);
    }
}

