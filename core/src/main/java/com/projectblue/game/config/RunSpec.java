package com.projectblue.game.config;

public record RunSpec(CampaignConfig.Level level, Difficulty difficulty, CampaignConfig.Tuning tuning, Loadout loadout) {
    public RunSpec {
        if (level == null || difficulty == null || tuning == null || loadout == null) throw new IllegalArgumentException("Missing run configuration");
    }
    public static RunSpec original() { return create(1, Difficulty.NORMAL, Loadout.standard()); }
    public static RunSpec create(int level, Difficulty difficulty, Loadout loadout) {
        return new RunSpec(CampaignConfig.DEFAULT.level(level), difficulty, CampaignConfig.DEFAULT.tuning(difficulty), loadout);
    }
    public MissionConfig mission() { return level.id() == 1 ? MissionConfig.BLUE_COAST : null; }
    public boolean hasBoss() { return level.id() == 1 || level.boss() || difficulty != Difficulty.NORMAL; }
    public int combatTargets() { return mission() == null ? tuning.droneCount() + (hasBoss() ? 1 : 0) : mission().enemyCount(tuning.spawnDensity()); }
}
