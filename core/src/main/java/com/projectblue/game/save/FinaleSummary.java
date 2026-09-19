package com.projectblue.game.save;

import com.projectblue.game.config.CampaignConfig;
import com.projectblue.game.config.MissionConfig;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable aggregate used by the final scene and persistence tests. */
public final class FinaleSummary {
    private final boolean completed;
    private final int completedSectors, totalStars, achievements;
    private final float cleanupAverage, rescueAverage;
    private final Set<MissionConfig.CreatureKind> rescuedSpecies;

    public FinaleSummary(Profile profile) {
        if (profile == null) throw new IllegalArgumentException("Missing profile");
        completed = profile.campaignCompleted();
        int sectors=0,stars=0,achievementCount=0;
        float cleanup=0,rescue=0;
        EnumSet<MissionConfig.CreatureKind> species=EnumSet.noneOf(MissionConfig.CreatureKind.class);
        for (int id=1;id<=CampaignConfig.LEVEL_COUNT;id++) {
            LevelRecord record=profile.level(id);
            stars+=record.bestStars; cleanup+=record.bestCleanup; rescue+=record.bestRescue;
            if (record.bestStars>0) sectors++;
            if (record.bestRescue>0) {
                MissionConfig mission=MissionConfig.forLevel(id);
                if (mission!=null) for (MissionConfig.Creature creature:mission.creatures()) species.add(creature.kind());
            }
        }
        for (Achievement achievement:Achievement.values()) if (profile.achievementUnlocked(achievement)) achievementCount++;
        completedSectors=sectors; totalStars=stars; achievements=achievementCount;
        cleanupAverage=cleanup/CampaignConfig.LEVEL_COUNT;
        rescueAverage=rescue/CampaignConfig.LEVEL_COUNT;
        rescuedSpecies=Collections.unmodifiableSet(species);
    }

    public boolean completed() { return completed; }
    public int completedSectors() { return completedSectors; }
    public int totalStars() { return totalStars; }
    public int achievements() { return achievements; }
    public float cleanupAverage() { return cleanupAverage; }
    public float rescueAverage() { return rescueAverage; }
    public Set<MissionConfig.CreatureKind> rescuedSpecies() { return rescuedSpecies; }
}
