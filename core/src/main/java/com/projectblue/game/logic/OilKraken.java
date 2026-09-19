package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Deterministic Black Tide boss controller with valve and oil-clearance gates. */
public final class OilKraken {
    public enum State { DORMANT, ARRIVAL, PIPE_ARMS, VALVE_WARNING, VALVES, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final int maxHealth,maxValveHealth;
    private final float cadence;
    private State state=State.DORMANT;
    private int health,leftValve,rightValve;
    private float stateTime,attackTimer,oilClearance;
    private boolean volley,oilSpray;

    public OilKraken(MissionConfig.Boss config,float healthScale,float cadence) {
        this.config=config; this.cadence=cadence;
        maxHealth=Math.round(config.coreHealth()*healthScale);
        maxValveHealth=Math.round(config.pipeHealth()*healthScale);
        health=maxHealth;
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    public void update(float dt,float clearance) {
        oilClearance=Rules.clamp(clearance,0,1);
        if (state==State.DORMANT || state==State.DEFEATED || dt<=0) return;
        stateTime+=dt;
        if (state==State.ARRIVAL && stateTime>=config.arrivalSeconds()) enter(State.PIPE_ARMS);
        else if (state==State.VALVE_WARNING && stateTime>=config.telegraphSeconds()) enter(State.VALVES);
        if (state==State.VALVES && leftValve==0 && rightValve==0 && oilClearance>=.6f) enter(State.CORE_EXPOSED);
        if (state==State.PIPE_ARMS || state==State.VALVES || state==State.CORE_EXPOSED) {
            attackTimer-=dt;
            if (attackTimer<=0) {
                volley=true; oilSpray=state!=State.CORE_EXPOSED || oilClearance<.85f;
                float acceleration=state==State.CORE_EXPOSED ? 1+oilClearance*.75f : 1;
                attackTimer=config.attackInterval()/(cadence*acceleration);
            }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int before=health;
        if (state==State.PIPE_ARMS) {
            int threshold=Math.max(1,Math.round(maxHealth*.66f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.VALVE_WARNING);
        } else {
            health=Rules.damage(health,damage);
            if (health==0) enter(State.DEFEATED);
        }
        return before-health;
    }
    public void closeValve(boolean left) {
        if (state!=State.VALVES) return;
        if (left) leftValve=0; else rightValve=0;
    }
    private void enter(State next) {
        state=next; stateTime=0; attackTimer=.4f;
        if (next==State.VALVE_WARNING) leftValve=rightValve=maxValveHealth;
    }
    public boolean consumeVolley() { boolean value=volley; volley=false; return value; }
    public boolean consumeOilSpray() { boolean value=oilSpray; oilSpray=false; return value; }
    public boolean coreVulnerable() { return state==State.PIPE_ARMS || state==State.CORE_EXPOSED; }
    public boolean telegraphing() { return state==State.VALVE_WARNING; }
    public boolean defeated() { return state==State.DEFEATED; }
    public int phase() { return state==State.PIPE_ARMS?1:state==State.VALVE_WARNING||state==State.VALVES?2:state==State.CORE_EXPOSED?3:0; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public float oilClearance() { return oilClearance; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int valveHealth(boolean left) { return left?leftValve:rightValve; }
    public int maxValveHealth() { return maxValveHealth; }
}
