package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.RunSpec;
import static com.projectblue.game.config.GameConfig.*;

public final class LevelResult {
    public final boolean completed;
    public final int score, salvage, stars;
    public final float combat, cleanup, rescue, integrity;
    public final int levelId, plasticCollected, turtlesRescued, enemiesDestroyed, damageTaken;
    public final Difficulty difficulty;
    public int wasteCleaned, wasteTotal, creaturesTotal, coralDamage;
    public float beforeRestoration = .08f, afterRestoration;
    public LevelResult(boolean completed, int kills, int plastic, int turtles, int salvage, int health) {
        this(RunSpec.original(), completed, kills, plastic, turtles, salvage, health);
    }
    public LevelResult(RunSpec spec, boolean completed, int kills, int plastic, int turtles, int salvage, int health) {
        this(spec, completed, kills, plastic, turtles, salvage, health, Math.max(0, spec.loadout().health() - health));
    }
    public LevelResult(RunSpec spec, boolean completed, int kills, int plastic, int turtles, int salvage, int health, int damageTaken) {
        levelId = spec.level().id(); difficulty = spec.difficulty();
        enemiesDestroyed = Math.max(0, Math.min(spec.combatTargets(), kills));
        this.damageTaken = Math.max(0, damageTaken);
        this.completed = completed;
        this.salvage = Math.max(0, salvage);
        plasticCollected = Math.max(0, Math.min(PLASTIC_COUNT, plastic));
        turtlesRescued = Math.max(0, Math.min(TURTLE_COUNT, turtles));
        combat = Rules.percentage(kills, spec.combatTargets());
        cleanup = Rules.cleanup(plastic);
        rescue = Rules.rescue(turtles);
        integrity = Rules.percentage(health, spec.loadout().health());
        score = Rules.score(kills, plastic, turtles, salvage, health, completed);
        stars = Rules.stars(completed, combat, cleanup, rescue, integrity);
        wasteCleaned = plasticCollected; wasteTotal = PLASTIC_COUNT; creaturesTotal = TURTLE_COUNT;
        afterRestoration = (cleanup * CLEANUP_RESTORE_WEIGHT + rescue * RESCUE_RESTORE_WEIGHT) / 100f;
    }
    public LevelResult(RunSpec spec, boolean completed, MissionOutcome outcome) {
        levelId=spec.level().id(); difficulty=spec.difficulty(); this.completed=completed;
        var mission=spec.mission();
        enemiesDestroyed=outcome.kills(); plasticCollected=outcome.plastic(); turtlesRescued=outcome.rescued();
        wasteCleaned=outcome.cleaned(); wasteTotal=mission.cleanupCount(); creaturesTotal=mission.turtleCount;
        salvage=Math.max(0,outcome.salvage()); damageTaken=outcome.damageTaken(); coralDamage=outcome.coralDamage();
        float coralLoss=Rules.percentage(coralDamage,Math.max(1,mission.coralCount*30))/100f;
        combat=Rules.percentage(enemiesDestroyed,outcome.enemiesEncountered());
        cleanup=Rules.clamp(Rules.percentage(wasteCleaned,wasteTotal)-coralLoss*30,0,100);
        rescue=Rules.percentage(turtlesRescued,creaturesTotal);
        integrity=Rules.clamp(Rules.percentage(outcome.health(),spec.loadout().health())-coralLoss*25,0,100);
        score=Rules.score(0,wasteCleaned,turtlesRescued,salvage,outcome.health(),completed)+outcome.combatScore();
        stars=Rules.stars(completed,combat,cleanup,rescue,integrity);
        afterRestoration=outcome.restoration();
    }
}
