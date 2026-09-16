package com.projectblue.game.save;
import static com.projectblue.game.config.GameConfig.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.logic.Rules;

public final class Profile {
    public int version = PROFILE_VERSION;
    public boolean soundEnabled = true, musicEnabled = true;
    public float soundVolume = DEFAULT_SOUND_VOLUME, musicVolume = DEFAULT_MUSIC_VOLUME;
    public int bestScore, bestStars, totalSalvage, completedRuns;
    public void normalize() {
        version = PROFILE_VERSION;
        soundVolume = Rules.clamp(soundVolume, 0, 1);
        musicVolume = Rules.clamp(musicVolume, 0, 1);
        bestScore = Math.max(0, bestScore);
        bestStars = Math.max(0, Math.min(3, bestStars));
        totalSalvage = Math.max(0, totalSalvage);
        completedRuns = Math.max(0, completedRuns);
    }
    public void record(LevelResult result) {
        bestScore = Math.max(bestScore, result.score);
        bestStars = Math.max(bestStars, result.stars);
        totalSalvage += result.salvage;
        if (result.completed) completedRuns++;
        normalize();
    }
}

