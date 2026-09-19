package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Three-phase Ghost Net Harvester controller with moving safe lanes and gated core damage. */
public final class GhostNetHarvester {
    public enum State { DORMANT, ARRIVAL, NET_WARNING, NET_BARRAGE, WALL_WARNING, NET_WALLS,
                        GENERATOR_WARNING, GENERATORS, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final float cadence;
    private final int maxHealth,maxGeneratorHealth;
    private State state=State.DORMANT;
    private int health,leftGenerator,rightGenerator,dronesLaunched;
    private float stateTime,attackTimer;
    private boolean volley,drone,largeNet;

    public GhostNetHarvester(MissionConfig.Boss config,float healthMultiplier,float cadence) {
        this.config=config; this.cadence=cadence;
        health=maxHealth=Math.round(config.coreHealth()*healthMultiplier);
        leftGenerator=rightGenerator=maxGeneratorHealth=Math.round(config.pipeHealth()*healthMultiplier);
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    private void enter(State next) { state=next; stateTime=0; attackTimer=interval(); volley=drone=largeNet=false; }
    public void update(float dt) {
        if (dt<=0 || !Float.isFinite(dt) || state==State.DORMANT || state==State.DEFEATED) return;
        stateTime+=dt;
        switch (state) {
            case ARRIVAL -> { if (stateTime>=config.arrivalSeconds()) enter(State.NET_WARNING); }
            case NET_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.NET_BARRAGE); }
            case WALL_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.NET_WALLS); }
            case GENERATOR_WARNING -> { if (stateTime>=config.telegraphSeconds()) enter(State.GENERATORS); }
            case NET_BARRAGE, NET_WALLS, GENERATORS, CORE_EXPOSED -> {
                attackTimer-=dt;
                if (attackTimer<=0) {
                    volley=true; attackTimer+=interval();
                    if (state==State.NET_BARRAGE) largeNet=true;
                    if ((state==State.NET_BARRAGE || state==State.NET_WALLS) && dronesLaunched<config.droneBudget()) {
                        drone=true; dronesLaunched++;
                    }
                }
            }
            default -> { }
        }
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int floor=state==State.NET_BARRAGE?maxHealth*2/3:state==State.NET_WALLS?maxHealth/3:0;
        int dealt=Math.min(damage,health-floor); health-=dealt;
        if (health==floor) {
            if (state==State.NET_BARRAGE) enter(State.WALL_WARNING);
            else if (state==State.NET_WALLS) enter(State.GENERATOR_WARNING);
            else enter(State.DEFEATED);
        }
        return dealt;
    }
    public int hitGenerator(boolean left,int damage) {
        if (state!=State.GENERATORS || damage<=0) return 0;
        int current=left?leftGenerator:rightGenerator,dealt=Math.min(damage,current);
        if (left) leftGenerator-=dealt; else rightGenerator-=dealt;
        if (leftGenerator==0 && rightGenerator==0) enter(State.CORE_EXPOSED);
        return dealt;
    }
    public boolean consumeVolley() { boolean value=volley; volley=false; return value; }
    public boolean consumeDrone() { boolean value=drone; drone=false; return value; }
    public boolean consumeLargeNet() { boolean value=largeNet; largeNet=false; return value; }
    public boolean coreVulnerable() { return state==State.NET_BARRAGE || state==State.NET_WALLS || state==State.CORE_EXPOSED; }
    public boolean wallsActive() { return state==State.NET_WALLS; }
    public float safeLaneX() { return 270+(float)Math.sin(stateTime*.62f)*145; }
    public boolean telegraphing() { return state==State.NET_WARNING || state==State.WALL_WARNING || state==State.GENERATOR_WARNING
        || ((state==State.NET_BARRAGE || state==State.NET_WALLS) && attackTimer<=.85f); }
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
            case ARRIVAL, NET_WARNING, NET_BARRAGE -> 1;
            case WALL_WARNING, NET_WALLS -> 2;
            default -> 3;
        };
    }
}
