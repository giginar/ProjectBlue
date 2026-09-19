package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;

/** Reusable energy-backed reveal ability. It has no rendering or input dependencies. */
public final class SonarSystem {
    private final float capacity,cost,regen,revealDuration;
    private float energy,revealTime,pulseTime;

    public SonarSystem(MissionConfig.Sonar config,Difficulty difficulty) {
        if (config==null || difficulty==null) throw new IllegalArgumentException("Missing sonar configuration");
        float pressure=1+difficulty.ordinal()*.08f;
        capacity=config.maxEnergy(); cost=Math.min(capacity,config.pulseCost()*pressure);
        regen=config.regenPerSecond()/pressure; revealDuration=config.revealSeconds(); energy=capacity;
    }
    public void update(float dt) {
        if (dt<=0 || !Float.isFinite(dt)) return;
        energy=Math.min(capacity,energy+regen*dt);
        revealTime=Math.max(0,revealTime-dt); pulseTime=Math.max(0,pulseTime-dt);
    }
    public boolean activate() {
        if (energy+0.0001f<cost) return false;
        energy-=cost; revealTime=revealDuration; pulseTime=.65f; return true;
    }
    public void recharge(float amount) { if (amount>0 && Float.isFinite(amount)) energy=Math.min(capacity,energy+amount); }
    public boolean revealing() { return revealTime>0; }
    public float energy() { return energy; }
    public float capacity() { return capacity; }
    public float cost() { return cost; }
    public float revealTime() { return revealTime; }
    public float pulseProgress() { return pulseTime<=0?0:1-pulseTime/.65f; }
}
