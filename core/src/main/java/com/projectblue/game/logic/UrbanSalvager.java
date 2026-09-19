package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Deterministic three-stage Sunken City boss controller. */
public final class UrbanSalvager {
    public enum State { DORMANT, ARRIVAL, SCRAP_VOLLEY, ARMOR_WARNING, ARMOR_PLATES, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final int maxHealth,maxPlateHealth;
    private final float cadence;
    private State state=State.DORMANT;
    private int health,leftPlate,rightPlate;
    private float stateTime,attackTimer;
    private boolean scrap,turret;

    public UrbanSalvager(MissionConfig.Boss config,float healthScale,float cadence) {
        this.config=config; this.cadence=cadence;
        maxHealth=Math.round(config.coreHealth()*healthScale);
        maxPlateHealth=Math.round(config.pipeHealth()*healthScale);
        health=maxHealth;
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    public void update(float dt) {
        if (state==State.DORMANT || state==State.DEFEATED || dt<=0) return;
        stateTime+=dt;
        if (state==State.ARRIVAL && stateTime>=config.arrivalSeconds()) enter(State.SCRAP_VOLLEY);
        else if (state==State.ARMOR_WARNING && stateTime>=config.telegraphSeconds()) enter(State.ARMOR_PLATES);
        if (state==State.SCRAP_VOLLEY || state==State.CORE_EXPOSED) {
            attackTimer-=dt;
            if (attackTimer<=0) {
                scrap=true;
                turret=state==State.SCRAP_VOLLEY && ((int)(stateTime/Math.max(.1f,config.attackInterval())))%2==1;
                attackTimer=config.attackInterval()/(cadence*(state==State.CORE_EXPOSED?1.35f:1));
            }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int before=health;
        if (state==State.SCRAP_VOLLEY) {
            int threshold=Math.max(1,Math.round(maxHealth*.62f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.ARMOR_WARNING);
        } else {
            health=Rules.damage(health,damage);
            if (health==0) enter(State.DEFEATED);
        }
        return before-health;
    }
    public int hitPlate(boolean left,int damage) {
        if (state!=State.ARMOR_PLATES || damage<=0) return 0;
        int before=left?leftPlate:rightPlate;
        if (left) leftPlate=Rules.damage(leftPlate,damage); else rightPlate=Rules.damage(rightPlate,damage);
        if (leftPlate==0 && rightPlate==0) enter(State.CORE_EXPOSED);
        return before-(left?leftPlate:rightPlate);
    }
    private void enter(State next) {
        state=next; stateTime=0; attackTimer=.35f;
        if (next==State.ARMOR_WARNING) leftPlate=rightPlate=maxPlateHealth;
    }
    public boolean consumeScrap() { boolean value=scrap; scrap=false; return value; }
    public boolean consumeTurret() { boolean value=turret; turret=false; return value; }
    public boolean coreVulnerable() { return state==State.SCRAP_VOLLEY || state==State.CORE_EXPOSED; }
    public boolean telegraphing() { return state==State.ARMOR_WARNING; }
    public boolean defeated() { return state==State.DEFEATED; }
    public int phase() { return state==State.SCRAP_VOLLEY?1:state==State.ARMOR_WARNING||state==State.ARMOR_PLATES?2:state==State.CORE_EXPOSED?3:0; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int plateHealth(boolean left) { return left?leftPlate:rightPlate; }
    public int maxPlateHealth() { return maxPlateHealth; }
}
