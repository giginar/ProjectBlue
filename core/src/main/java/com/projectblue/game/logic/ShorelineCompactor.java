package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Pure boss state machine: phases and damage gates are independent of rendering and entity pools. */
public final class ShorelineCompactor {
    public enum State { DORMANT, ARRIVAL, DISCHARGE, PRESS_WARNING, PRESS_ACTIVE, PIPE_WARNING, PIPES, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final float cadence;
    private final int maxHealth, maxPipeHealth;
    private State state = State.DORMANT;
    private int health, leftPipe, rightPipe, dronesLaunched;
    private float stateTime, attackTimer;
    private boolean volley, drone;
    public ShorelineCompactor(MissionConfig.Boss config, float healthMultiplier, float cadence) {
        this.config=config; this.cadence=cadence;
        health=maxHealth=Math.round(config.coreHealth()*healthMultiplier);
        leftPipe=rightPipe=maxPipeHealth=Math.round(config.pipeHealth()*healthMultiplier);
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    private void enter(State next) { state=next; stateTime=0; attackTimer=interval(); volley=drone=false; }
    public void update(float dt) {
        if (dt<=0 || !Float.isFinite(dt) || state==State.DORMANT || state==State.DEFEATED) return;
        stateTime+=dt;
        switch (state) {
            case ARRIVAL -> { if (stateTime>=config.arrivalSeconds()) enter(State.DISCHARGE); }
            case PRESS_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.PRESS_ACTIVE); }
            case PIPE_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.PIPES); }
            case DISCHARGE, PRESS_ACTIVE, PIPES, CORE_EXPOSED -> {
                attackTimer-=dt;
                if (attackTimer<=0) {
                    volley=true; attackTimer+=interval();
                    if (state==State.DISCHARGE && dronesLaunched<config.droneBudget()) { drone=true; dronesLaunched++; }
                }
            }
            default -> { }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int floor = state==State.DISCHARGE ? maxHealth*2/3 : state==State.PRESS_ACTIVE ? maxHealth/3 : 0;
        int dealt=Math.min(damage,health-floor); health-=dealt;
        if (health==floor) {
            if (state==State.DISCHARGE) enter(State.PRESS_WARNING);
            else if (state==State.PRESS_ACTIVE) enter(State.PIPE_WARNING);
            else enter(State.DEFEATED);
        }
        return dealt;
    }
    public int hitPipe(boolean left,int damage) {
        if (state!=State.PIPES || damage<=0) return 0;
        int dealt=Math.min(damage,left?leftPipe:rightPipe);
        if (left) leftPipe-=dealt; else rightPipe-=dealt;
        if (leftPipe==0 && rightPipe==0) enter(State.CORE_EXPOSED);
        return dealt;
    }
    public boolean consumeVolley() { boolean pending=volley; volley=false; return pending; }
    public boolean consumeDrone() { boolean pending=drone; drone=false; return pending; }
    public boolean coreVulnerable() { return state==State.DISCHARGE || state==State.PRESS_ACTIVE || state==State.CORE_EXPOSED; }
    public boolean pressesActive() { return state==State.PRESS_ACTIVE || state==State.PIPE_WARNING || state==State.PIPES || state==State.CORE_EXPOSED; }
    public boolean telegraphing() { return state==State.PRESS_WARNING || state==State.PIPE_WARNING || (activeAttack() && attackTimer<=.85f); }
    private boolean activeAttack() { return state==State.DISCHARGE || state==State.PRESS_ACTIVE || state==State.PIPES || state==State.CORE_EXPOSED; }
    public boolean defeated() { return state==State.DEFEATED; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public float interval() { return config.attackInterval()/cadence; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int pipeHealth(boolean left) { return left?leftPipe:rightPipe; }
    public int maxPipeHealth() { return maxPipeHealth; }
    public int dronesLaunched() { return dronesLaunched; }
    public int phase() {
        return switch (state) {
            case DORMANT, DEFEATED -> 0;
            case ARRIVAL, DISCHARGE -> 1;
            case PRESS_WARNING, PRESS_ACTIVE -> 2;
            default -> 3;
        };
    }
}
