package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;

/** Final boss controller. Every emitted attack spends a warning window before becoming active. */
public final class LeviathanCore {
    public enum State {
        DORMANT, ARRIVAL, ARCHIVE_WARNING, ARCHIVE_ASSAULT,
        SHIELD_WARNING, SHIELD_GENERATORS,
        RESTORATION_WARNING, RESTORATION_SYSTEMS,
        CORE_WARNING, CORE_EXPOSED, ESCAPE_WARNING, ESCAPE, ESCAPED, ESCAPE_FAILED
    }
    public enum Attack {
        ARCHIVE_FAN, NET_CROSS, OIL_SURGE, SONAR_RING,
        SHIELD_LANES, RESCUE_SWEEP, CORE_BURST, COLLAPSE
    }

    private final MissionConfig.Boss config;
    private final Difficulty difficulty;
    private final float cadence;
    private final int maxHealth, maxGeneratorHealth;
    private final int cleanupRequired, rescueRequired, sonarRequired;
    private final float escapeLimit, escapeHoldRequired;
    private State state = State.DORMANT;
    private int health, leftGenerator, rightGenerator;
    private int cleanupProgress, rescueProgress, sonarProgress, patternCursor;
    private float stateTime, attackCooldown, warningRemaining, escapeRemaining, escapeProgress;
    private Attack warningAttack, readyAttack;

    public LeviathanCore(MissionConfig.Boss config, Difficulty difficulty, float healthScale, float cadence) {
        if (config == null || difficulty == null || healthScale <= 0 || cadence <= 0)
            throw new IllegalArgumentException("Invalid Leviathan Core configuration");
        this.config = config;
        this.difficulty = difficulty;
        this.cadence = cadence;
        health = maxHealth = Math.round(config.coreHealth() * healthScale);
        maxGeneratorHealth = Math.round(config.pipeHealth() * healthScale);
        cleanupRequired = Math.min(config.restorationCleanup(), 2 + difficulty.ordinal());
        rescueRequired = Math.min(config.restorationRescues(), 1 + difficulty.ordinal() / 2);
        sonarRequired = Math.min(config.restorationSonarPulses(), 1 + difficulty.ordinal() / 2);
        escapeLimit = config.escapeSeconds() * (1f - difficulty.ordinal() * .1f);
        escapeHoldRequired = 1.4f + difficulty.ordinal() * .2f;
    }

    public void start() { if (state == State.DORMANT) enter(State.ARRIVAL); }

    public void update(float dt, boolean inEscapeZone) {
        if (dt <= 0 || !Float.isFinite(dt) || state == State.DORMANT || finished()) return;
        stateTime += dt;
        switch (state) {
            case ARRIVAL -> { if (stateTime >= config.arrivalSeconds()) enter(State.ARCHIVE_WARNING); }
            case ARCHIVE_WARNING -> { if (stateTime >= telegraphSeconds()) enter(State.ARCHIVE_ASSAULT); }
            case SHIELD_WARNING -> { if (stateTime >= telegraphSeconds()) enter(State.SHIELD_GENERATORS); }
            case RESTORATION_WARNING -> { if (stateTime >= telegraphSeconds()) enter(State.RESTORATION_SYSTEMS); }
            case CORE_WARNING -> { if (stateTime >= telegraphSeconds()) enter(State.CORE_EXPOSED); }
            case ESCAPE_WARNING -> { if (stateTime >= telegraphSeconds()) enter(State.ESCAPE); }
            case ARCHIVE_ASSAULT, SHIELD_GENERATORS, RESTORATION_SYSTEMS, CORE_EXPOSED -> updateAttack(dt);
            case ESCAPE -> {
                updateAttack(dt);
                escapeRemaining = Math.max(0, escapeRemaining - dt);
                escapeProgress = inEscapeZone
                    ? Math.min(escapeHoldRequired, escapeProgress + dt)
                    : Math.max(0, escapeProgress - dt * .5f);
                if (escapeProgress >= escapeHoldRequired) enter(State.ESCAPED);
                else if (escapeRemaining <= 0) enter(State.ESCAPE_FAILED);
            }
            default -> { }
        }
    }

    private void updateAttack(float dt) {
        if (readyAttack != null) return;
        if (warningAttack != null) {
            warningRemaining -= dt;
            if (warningRemaining <= 0) {
                readyAttack = warningAttack;
                warningAttack = null;
                attackCooldown = interval();
            }
            return;
        }
        attackCooldown -= dt;
        if (attackCooldown <= 0) {
            warningAttack = nextAttack();
            warningRemaining = telegraphSeconds();
        }
    }

    private Attack nextAttack() {
        int step = patternCursor++;
        return switch (state) {
            case ARCHIVE_ASSAULT -> switch (difficulty) {
                case NORMAL -> step % 2 == 0 ? Attack.ARCHIVE_FAN : Attack.NET_CROSS;
                case HARD -> new Attack[]{Attack.ARCHIVE_FAN, Attack.NET_CROSS, Attack.OIL_SURGE}[step % 3];
                case EXPERT -> new Attack[]{Attack.ARCHIVE_FAN, Attack.OIL_SURGE, Attack.NET_CROSS, Attack.SONAR_RING}[step % 4];
                case ABYSS -> new Attack[]{Attack.ARCHIVE_FAN, Attack.NET_CROSS, Attack.SONAR_RING, Attack.OIL_SURGE}[step % 4];
            };
            case SHIELD_GENERATORS -> difficulty.ordinal() < 2 || step % 2 == 0
                ? Attack.SHIELD_LANES : Attack.OIL_SURGE;
            case RESTORATION_SYSTEMS -> difficulty == Difficulty.ABYSS && step % 3 == 2
                ? Attack.OIL_SURGE : step % 2 == 0 ? Attack.RESCUE_SWEEP : Attack.SONAR_RING;
            case CORE_EXPOSED -> step % (difficulty.ordinal() >= 2 ? 3 : 2) == 0
                ? Attack.CORE_BURST : step % 2 == 0 ? Attack.SONAR_RING : Attack.ARCHIVE_FAN;
            case ESCAPE -> Attack.COLLAPSE;
            default -> Attack.ARCHIVE_FAN;
        };
    }

    public int hitCore(int damage) {
        if (!coreVulnerable() || damage <= 0) return 0;
        int before = health;
        if (state == State.ARCHIVE_ASSAULT) {
            int floor = Math.max(1, Math.round(maxHealth * .7f));
            health = Math.max(floor, Rules.damage(health, damage));
            if (health == floor) enter(State.SHIELD_WARNING);
        } else {
            health = Rules.damage(health, damage);
            if (health == 0) enter(State.ESCAPE_WARNING);
        }
        return before - health;
    }

    public int hitGenerator(boolean left, int damage) {
        if (state != State.SHIELD_GENERATORS || damage <= 0) return 0;
        int before = left ? leftGenerator : rightGenerator;
        if (left) leftGenerator = Rules.damage(leftGenerator, damage);
        else rightGenerator = Rules.damage(rightGenerator, damage);
        if (leftGenerator == 0 && rightGenerator == 0) enter(State.RESTORATION_WARNING);
        return before - (left ? leftGenerator : rightGenerator);
    }

    public boolean recordCleanup() {
        if (state != State.RESTORATION_SYSTEMS || cleanupProgress >= cleanupRequired) return false;
        cleanupProgress++;
        checkRestoration();
        return true;
    }

    public boolean recordRescue() {
        if (state != State.RESTORATION_SYSTEMS || rescueProgress >= rescueRequired) return false;
        rescueProgress++;
        checkRestoration();
        return true;
    }

    public boolean recordSonarPulse() {
        if (state != State.RESTORATION_SYSTEMS || sonarProgress >= sonarRequired) return false;
        sonarProgress++;
        checkRestoration();
        return true;
    }

    private void checkRestoration() {
        if (cleanupProgress >= cleanupRequired && rescueProgress >= rescueRequired && sonarProgress >= sonarRequired)
            enter(State.CORE_WARNING);
    }

    private void enter(State next) {
        state = next;
        stateTime = 0;
        attackCooldown = .35f;
        warningRemaining = 0;
        warningAttack = readyAttack = null;
        patternCursor = 0;
        if (next == State.SHIELD_GENERATORS) leftGenerator = rightGenerator = maxGeneratorHealth;
        if (next == State.ESCAPE) {
            escapeRemaining = escapeLimit;
            escapeProgress = 0;
        }
    }

    public Attack consumeAttack() {
        Attack attack = readyAttack;
        readyAttack = null;
        return attack;
    }

    public boolean coreVulnerable() { return state == State.ARCHIVE_ASSAULT || state == State.CORE_EXPOSED; }
    public boolean telegraphing() { return warningAttack != null || state == State.ARCHIVE_WARNING
        || state == State.SHIELD_WARNING || state == State.RESTORATION_WARNING
        || state == State.CORE_WARNING || state == State.ESCAPE_WARNING; }
    public boolean escaping() { return state == State.ESCAPE_WARNING || state == State.ESCAPE; }
    public boolean defeated() { return state == State.ESCAPED; }
    public boolean escapeFailed() { return state == State.ESCAPE_FAILED; }
    public boolean finished() { return state == State.ESCAPED || state == State.ESCAPE_FAILED; }
    public State state() { return state; }
    public int phase() {
        return switch (state) {
            case ARRIVAL, ARCHIVE_WARNING, ARCHIVE_ASSAULT -> 1;
            case SHIELD_WARNING, SHIELD_GENERATORS -> 2;
            case RESTORATION_WARNING, RESTORATION_SYSTEMS -> 3;
            case CORE_WARNING, CORE_EXPOSED, ESCAPE_WARNING, ESCAPE, ESCAPED -> 4;
            default -> 0;
        };
    }
    public float stateTime() { return stateTime; }
    public float telegraphSeconds() { return Math.max(.65f, config.telegraphSeconds() - difficulty.ordinal() * .16f); }
    public float warningRemaining() { return warningAttack == null ? 0 : Math.max(0, warningRemaining); }
    public Attack warningAttack() { return warningAttack; }
    public float interval() {
        float phaseRate = state == State.CORE_EXPOSED ? 1.3f : state == State.ESCAPE ? 1.55f : 1;
        return config.attackInterval() / (cadence * phaseRate);
    }
    public int attackComplexity() { return 1 + difficulty.ordinal(); }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int generatorHealth(boolean left) { return left ? leftGenerator : rightGenerator; }
    public int maxGeneratorHealth() { return maxGeneratorHealth; }
    public int cleanupProgress() { return cleanupProgress; }
    public int cleanupRequired() { return cleanupRequired; }
    public int rescueProgress() { return rescueProgress; }
    public int rescueRequired() { return rescueRequired; }
    public int sonarProgress() { return sonarProgress; }
    public int sonarRequired() { return sonarRequired; }
    public float escapeRemaining() { return escapeRemaining; }
    public float escapeLimit() { return escapeLimit; }
    public float escapeProgress() { return escapeProgress; }
    public float escapeHoldRequired() { return escapeHoldRequired; }
}
