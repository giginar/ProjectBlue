package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;

/** Deterministic gradual pressure load. Hazards are armed separately so damage is always telegraphed. */
public final class PressureSystem {
    private final MissionConfig.Pressure config;
    private final float difficultyScale;
    private float pressure, damageTimer;
    private int pendingDamage;

    public PressureSystem(MissionConfig.Pressure config,Difficulty difficulty) {
        if (config==null || difficulty==null) throw new IllegalArgumentException("Missing pressure tuning");
        this.config=config;
        difficultyScale=1+difficulty.ordinal()*.16f;
    }
    public void update(float dt,boolean safe,float hazardExposure) {
        if (dt<=0 || !Float.isFinite(dt)) return;
        if (safe) pressure-=config.safeRecoveryPerSecond()*dt;
        else pressure+=(config.ambientGainPerSecond()+Math.max(0,hazardExposure)*config.hazardGainPerSecond())*difficultyScale*dt;
        pressure=Rules.clamp(pressure,0,config.maxPressure());
        damageTimer=Math.max(0,damageTimer-dt);
        if (pressure>=config.dangerThreshold() && damageTimer<=0) {
            pendingDamage=6+Math.round((difficultyScale-1)*10);
            damageTimer=config.damageInterval()/difficultyScale;
        }
    }
    public int consumeDamage() { int value=pendingDamage; pendingDamage=0; return value; }
    public float pressure() { return pressure; }
    public float capacity() { return config.maxPressure(); }
    public float threshold() { return config.dangerThreshold(); }
    public boolean warning() { return pressure>=config.dangerThreshold()*.82f; }
}
