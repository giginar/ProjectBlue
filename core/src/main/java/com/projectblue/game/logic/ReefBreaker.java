package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Three-phase Reef Breaker controller with telegraphed hazards and shield generators. */
public final class ReefBreaker {
    public enum State { DORMANT, ARRIVAL, CUTTER_WARNING, CUTTER_SWEEP, GENERATOR_WARNING, GENERATORS,
                        CORAL_WARNING, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final float cadence;
    private final int maxHealth, maxGeneratorHealth;
    private State state=State.DORMANT;
    private int health, leftGenerator, rightGenerator, dronesLaunched;
    private float stateTime, attackTimer;
    private boolean volley, drone, coralStrike;

    public ReefBreaker(MissionConfig.Boss config,float healthMultiplier,float cadence) {
        this.config=config; this.cadence=cadence;
        health=maxHealth=Math.round(config.coreHealth()*healthMultiplier);
        leftGenerator=rightGenerator=maxGeneratorHealth=Math.round(config.pipeHealth()*healthMultiplier);
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    private void enter(State next) { state=next; stateTime=0; attackTimer=interval(); volley=drone=coralStrike=false; }
    public void update(float dt) {
        if (dt<=0 || !Float.isFinite(dt) || state==State.DORMANT || state==State.DEFEATED) return;
        stateTime+=dt;
        switch (state) {
            case ARRIVAL -> { if (stateTime>=config.arrivalSeconds()) enter(State.CUTTER_WARNING); }
            case CUTTER_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.CUTTER_SWEEP); }
            case GENERATOR_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.GENERATORS); }
            case CORAL_WARNING -> {
                if (stateTime>=config.telegraphSeconds()) { coralStrike=true; enterKeepingEvent(State.CORE_EXPOSED); }
            }
            case CUTTER_SWEEP, GENERATORS, CORE_EXPOSED -> {
                attackTimer-=dt;
                if (attackTimer<=0) {
                    volley=true; attackTimer+=interval();
                    if (state==State.CUTTER_SWEEP && dronesLaunched<config.droneBudget()) { drone=true; dronesLaunched++; }
                    if (state==State.CORE_EXPOSED) enterKeepingEvent(State.CORAL_WARNING);
                }
            }
            default -> { }
        }
    }
    private void enterKeepingEvent(State next) {
        boolean keepVolley=volley,keepDrone=drone,keepCoral=coralStrike;
        enter(next); volley=keepVolley; drone=keepDrone; coralStrike=keepCoral;
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int floor=state==State.CUTTER_SWEEP?maxHealth*2/3:0;
        int dealt=Math.min(damage,health-floor); health-=dealt;
        if (health==floor) {
            if (state==State.CUTTER_SWEEP) enter(State.GENERATOR_WARNING);
            else enter(State.DEFEATED);
        }
        return dealt;
    }
    public int hitGenerator(boolean left,int damage) {
        if (state!=State.GENERATORS || damage<=0) return 0;
        int current=left?leftGenerator:rightGenerator, dealt=Math.min(damage,current);
        if (left) leftGenerator-=dealt; else rightGenerator-=dealt;
        if (leftGenerator==0 && rightGenerator==0) enter(State.CORAL_WARNING);
        return dealt;
    }
    public boolean consumeVolley() { boolean value=volley; volley=false; return value; }
    public boolean consumeDrone() { boolean value=drone; drone=false; return value; }
    public boolean consumeCoralStrike() { boolean value=coralStrike; coralStrike=false; return value; }
    public boolean coreVulnerable() { return state==State.CUTTER_SWEEP || state==State.CORE_EXPOSED; }
    public boolean cuttersActive() { return state==State.CUTTER_SWEEP; }
    public boolean telegraphing() { return state==State.CUTTER_WARNING || state==State.GENERATOR_WARNING
        || state==State.CORAL_WARNING || (state==State.CUTTER_SWEEP && attackTimer<=.85f); }
    public boolean defeated() { return state==State.DEFEATED; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public float interval() { return config.attackInterval()/cadence; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int generatorHealth(boolean left) { return left?leftGenerator:rightGenerator; }
    public int maxGeneratorHealth() { return maxGeneratorHealth; }
    public int dronesLaunched() { return dronesLaunched; }
    public int phase() {
        return switch (state) {
            case DORMANT, DEFEATED -> 0;
            case ARRIVAL, CUTTER_WARNING, CUTTER_SWEEP -> 1;
            case GENERATOR_WARNING, GENERATORS -> 2;
            default -> 3;
        };
    }
}
