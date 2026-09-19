package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;

/** Reusable exposure meter. Hot and cold sources are supplied by the world each fixed step. */
public final class ThermalSystem {
    private final float capacity,threshold,hotGain,coldRecovery,passiveRecovery,damageInterval;
    private float heat,damageTimer;

    public ThermalSystem(MissionConfig.Thermal config,Difficulty difficulty) {
        if (config==null || difficulty==null) throw new IllegalArgumentException("Missing thermal configuration");
        float pressure=1+difficulty.ordinal()*.15f;
        capacity=config.maxHeat(); threshold=config.damageThreshold(); hotGain=config.hotGainPerSecond()*pressure;
        coldRecovery=config.coldRecoveryPerSecond(); passiveRecovery=config.passiveRecoveryPerSecond()/pressure;
        damageInterval=Math.max(.2f,config.damageInterval()-difficulty.ordinal()*.1f);
    }
    public void update(float dt,float hotExposure,boolean coldZone) {
        if (dt<=0 || !Float.isFinite(dt)) return;
        float gain=Math.max(0,hotExposure)*hotGain;
        float recovery=coldZone?coldRecovery:passiveRecovery;
        heat=Rules.clamp(heat+(gain-recovery)*dt,0,capacity);
        damageTimer=Math.max(0,damageTimer-dt);
    }
    public int consumeDamage() {
        if (heat<threshold || damageTimer>0) return 0;
        damageTimer=damageInterval;
        float severity=(heat-threshold)/Math.max(1,capacity-threshold);
        return 3+Math.round(severity*7);
    }
    public float heat() { return heat; }
    public float capacity() { return capacity; }
    public float threshold() { return threshold; }
    public boolean dangerous() { return heat>=threshold; }
}
