package com.projectblue.game.save;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.logic.Rules;

public final class LevelRecord {
    public boolean unlocked;
    public int bestStars, bestScore, completedDifficulties;
    public float bestCleanup, bestRescue;

    public boolean completed(Difficulty difficulty) { return (completedDifficulties & difficulty.bit()) != 0; }
    public Difficulty bestDifficulty() {
        Difficulty best = null;
        for (Difficulty d : Difficulty.values()) if (completed(d)) best = d;
        return best;
    }
    public boolean canPlay(Difficulty difficulty) {
        return unlocked && difficulty != null && (difficulty == Difficulty.NORMAL
            || completed(Difficulty.values()[difficulty.ordinal() - 1]));
    }
    public void record(LevelResult result) {
        bestScore = Math.max(bestScore, result.score);
        bestCleanup = Math.max(bestCleanup, result.cleanup);
        bestRescue = Math.max(bestRescue, result.rescue);
        bestStars = Math.max(bestStars, result.stars);
        if (result.completed && result.stars > 0) completedDifficulties |= result.difficulty.bit();
    }
    public void normalize() {
        bestStars = Math.max(0, Math.min(3, bestStars));
        bestScore = Math.max(0, bestScore);
        bestCleanup = Rules.clamp(bestCleanup, 0, 100);
        bestRescue = Rules.clamp(bestRescue, 0, 100);
        completedDifficulties &= 15;
        // Completions always form a prefix: reject orphan higher-tier flags.
        for (Difficulty d : Difficulty.values()) {
            if (!completed(d)) { completedDifficulties &= d.bit() - 1; break; }
        }
        if (bestStars == 0) completedDifficulties = 0;
    }
}
