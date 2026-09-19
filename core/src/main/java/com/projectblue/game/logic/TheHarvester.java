package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Abyss Mine boss: drill pressure, powered armor, then an exposed high-pressure core. */
public final class TheHarvester {
    public enum State { DORMANT, ARRIVAL, DRILL_ARMS, ARMOR_WARNING, POWERED_ARMOR, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final int maxHealth,maxStationHealth;
    private final float cadence;
    private State state=State.DORMANT;
    private int health,leftStation,rightStation;
    private float stateTime,attackTimer;
    private boolean drillVolley,debris,pressureBurst;

    public TheHarvester(MissionConfig.Boss config,float healthScale,float cadence) {
        this.config=config; this.cadence=cadence;
        maxHealth=Math.round(config.coreHealth()*healthScale);
        maxStationHealth=Math.round(config.pipeHealth()*healthScale);
        health=maxHealth;
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    public void update(float dt) {
        if (state==State.DORMANT || state==State.DEFEATED || dt<=0) return;
        stateTime+=dt;
        if (state==State.ARRIVAL && stateTime>=config.arrivalSeconds()) enter(State.DRILL_ARMS);
        else if (state==State.ARMOR_WARNING && stateTime>=config.telegraphSeconds()) enter(State.POWERED_ARMOR);
        if (state==State.DRILL_ARMS || state==State.POWERED_ARMOR || state==State.CORE_EXPOSED) {
            attackTimer-=dt;
            if (attackTimer<=0) {
                drillVolley=true;
                debris=state!=State.CORE_EXPOSED;
                pressureBurst=state==State.CORE_EXPOSED;
                attackTimer=config.attackInterval()/(cadence*(state==State.CORE_EXPOSED?1.45f:1));
            }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int before=health;
        if (state==State.DRILL_ARMS) {
            int threshold=Math.max(1,Math.round(maxHealth*.62f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.ARMOR_WARNING);
        } else {
            health=Rules.damage(health,damage);
            if (health==0) enter(State.DEFEATED);
        }
        return before-health;
    }
    public boolean disableStation(boolean left) {
        if (state!=State.POWERED_ARMOR) return false;
        if (left) leftStation=0; else rightStation=0;
        if (leftStation==0 && rightStation==0) enter(State.CORE_EXPOSED);
        return true;
    }
    private void enter(State next) {
        state=next; stateTime=0; attackTimer=.35f;
        if (next==State.POWERED_ARMOR) leftStation=rightStation=maxStationHealth;
    }
    public boolean consumeDrillVolley() { boolean value=drillVolley; drillVolley=false; return value; }
    public boolean consumeDebris() { boolean value=debris; debris=false; return value; }
    public boolean consumePressureBurst() { boolean value=pressureBurst; pressureBurst=false; return value; }
    public boolean coreVulnerable() { return state==State.DRILL_ARMS || state==State.CORE_EXPOSED; }
    public boolean telegraphing() { return state==State.ARMOR_WARNING; }
    public boolean defeated() { return state==State.DEFEATED; }
    public int phase() { return state==State.DRILL_ARMS?1:state==State.ARMOR_WARNING||state==State.POWERED_ARMOR?2:
        state==State.CORE_EXPOSED?3:0; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int stationHealth(boolean left) { return left?leftStation:rightStation; }
    public int maxStationHealth() { return maxStationHealth; }
}
