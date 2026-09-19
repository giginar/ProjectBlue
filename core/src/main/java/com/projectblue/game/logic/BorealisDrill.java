package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Deterministic Frozen Depths boss controller with thermal pressure and cooling-unit gates. */
public final class BorealisDrill {
    public enum State { DORMANT, ARRIVAL, DRILL_ARMS, VENT_WARNING, THERMAL_VENTS, COOLING_UNITS, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final int maxHealth,maxUnitHealth;
    private final float cadence;
    private State state=State.DORMANT;
    private int health,leftUnit,rightUnit;
    private float stateTime,attackTimer;
    private boolean iceFall,drillVolley,thermalVent;

    public BorealisDrill(MissionConfig.Boss config,float healthScale,float cadence) {
        this.config=config; this.cadence=cadence;
        maxHealth=Math.round(config.coreHealth()*healthScale);
        maxUnitHealth=Math.round(config.pipeHealth()*healthScale);
        health=maxHealth;
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    public void update(float dt) {
        if (state==State.DORMANT || state==State.DEFEATED || dt<=0) return;
        stateTime+=dt;
        if (state==State.ARRIVAL && stateTime>=config.arrivalSeconds()) enter(State.DRILL_ARMS);
        else if (state==State.VENT_WARNING && stateTime>=config.telegraphSeconds()) enter(State.THERMAL_VENTS);
        else if (state==State.THERMAL_VENTS && stateTime>=12f/Math.max(1,cadence*.6f)) enter(State.COOLING_UNITS);
        if (state==State.DRILL_ARMS || state==State.THERMAL_VENTS || state==State.COOLING_UNITS || state==State.CORE_EXPOSED) {
            attackTimer-=dt;
            if (attackTimer<=0) {
                drillVolley=true; iceFall=state==State.DRILL_ARMS || state==State.COOLING_UNITS;
                thermalVent=state==State.THERMAL_VENTS || state==State.CORE_EXPOSED;
                attackTimer=config.attackInterval()/(cadence*(state==State.CORE_EXPOSED?1.4f:1));
            }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int before=health;
        if (state==State.DRILL_ARMS) {
            int threshold=Math.max(1,Math.round(maxHealth*.62f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.VENT_WARNING);
        } else {
            health=Rules.damage(health,damage);
            if (health==0) enter(State.DEFEATED);
        }
        return before-health;
    }
    public int hitUnit(boolean left,int damage) {
        if (state!=State.COOLING_UNITS || damage<=0) return 0;
        int before=left?leftUnit:rightUnit;
        if (left) leftUnit=Rules.damage(leftUnit,damage); else rightUnit=Rules.damage(rightUnit,damage);
        if (leftUnit==0 && rightUnit==0) enter(State.CORE_EXPOSED);
        return before-(left?leftUnit:rightUnit);
    }
    private void enter(State next) {
        state=next; stateTime=0; attackTimer=.35f;
        if (next==State.COOLING_UNITS) leftUnit=rightUnit=maxUnitHealth;
    }
    public boolean consumeIceFall() { boolean value=iceFall; iceFall=false; return value; }
    public boolean consumeDrillVolley() { boolean value=drillVolley; drillVolley=false; return value; }
    public boolean consumeThermalVent() { boolean value=thermalVent; thermalVent=false; return value; }
    public boolean coreVulnerable() { return state==State.DRILL_ARMS || state==State.CORE_EXPOSED; }
    public boolean telegraphing() { return state==State.VENT_WARNING; }
    public boolean defeated() { return state==State.DEFEATED; }
    public int phase() { return state==State.DRILL_ARMS?1:state==State.VENT_WARNING||state==State.THERMAL_VENTS?2:
        state==State.COOLING_UNITS||state==State.CORE_EXPOSED?3:0; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int unitHealth(boolean left) { return left?leftUnit:rightUnit; }
    public int maxUnitHealth() { return maxUnitHealth; }
}
