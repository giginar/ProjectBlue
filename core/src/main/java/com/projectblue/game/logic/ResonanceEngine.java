package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Deterministic Silent Reef boss controller with sonar-gated moving weak points. */
public final class ResonanceEngine {
    public enum State { DORMANT, ARRIVAL, SONAR_WAVES, DECOY_FIELD, WEAK_POINTS, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final int maxHealth,maxWeakPointHealth;
    private final float cadence;
    private State state=State.DORMANT;
    private int health,leftWeakPoint,rightWeakPoint;
    private float stateTime,attackTimer;
    private boolean wave,mine,decoyShift;

    public ResonanceEngine(MissionConfig.Boss config,float healthScale,float cadence) {
        this.config=config; this.cadence=cadence;
        maxHealth=Math.round(config.coreHealth()*healthScale);
        maxWeakPointHealth=Math.round(config.pipeHealth()*healthScale);
        health=maxHealth;
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    public void update(float dt) {
        if (state==State.DORMANT || state==State.DEFEATED || dt<=0) return;
        stateTime+=dt;
        if (state==State.ARRIVAL && stateTime>=config.arrivalSeconds()) enter(State.SONAR_WAVES);
        if (state==State.SONAR_WAVES || state==State.DECOY_FIELD || state==State.WEAK_POINTS || state==State.CORE_EXPOSED) {
            attackTimer-=dt;
            if (attackTimer<=0) {
                wave=true; mine=state==State.SONAR_WAVES || state==State.WEAK_POINTS;
                decoyShift=state==State.DECOY_FIELD;
                float acceleration=state==State.CORE_EXPOSED?1.35f:1;
                attackTimer=config.attackInterval()/(cadence*acceleration);
            }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int before=health;
        if (state==State.SONAR_WAVES) {
            int threshold=Math.max(1,Math.round(maxHealth*.66f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.DECOY_FIELD);
        } else if (state==State.DECOY_FIELD) {
            int threshold=Math.max(1,Math.round(maxHealth*.33f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.WEAK_POINTS);
        } else {
            health=Rules.damage(health,damage);
            if (health==0) enter(State.DEFEATED);
        }
        return before-health;
    }
    public int hitWeakPoint(boolean left,int damage,boolean sonarRevealing) {
        if (state!=State.WEAK_POINTS || !sonarRevealing || damage<=0) return 0;
        int before=left?leftWeakPoint:rightWeakPoint;
        if (left) leftWeakPoint=Rules.damage(leftWeakPoint,damage); else rightWeakPoint=Rules.damage(rightWeakPoint,damage);
        if (leftWeakPoint==0 && rightWeakPoint==0) enter(State.CORE_EXPOSED);
        return before-(left?leftWeakPoint:rightWeakPoint);
    }
    private void enter(State next) {
        state=next; stateTime=0; attackTimer=.35f;
        if (next==State.WEAK_POINTS) leftWeakPoint=rightWeakPoint=maxWeakPointHealth;
    }
    public boolean consumeWave() { boolean value=wave; wave=false; return value; }
    public boolean consumeMine() { boolean value=mine; mine=false; return value; }
    public boolean consumeDecoyShift() { boolean value=decoyShift; decoyShift=false; return value; }
    public boolean coreVulnerable() { return state==State.SONAR_WAVES || state==State.DECOY_FIELD || state==State.CORE_EXPOSED; }
    public boolean weakPointsActive() { return state==State.WEAK_POINTS; }
    public boolean defeated() { return state==State.DEFEATED; }
    public int phase() { return state==State.SONAR_WAVES?1:state==State.DECOY_FIELD?2:
        state==State.WEAK_POINTS||state==State.CORE_EXPOSED?3:0; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int weakPointHealth(boolean left) { return left?leftWeakPoint:rightWeakPoint; }
    public int maxWeakPointHealth() { return maxWeakPointHealth; }
}
