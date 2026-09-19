package com.projectblue.game.logic;
import com.projectblue.game.config.MissionConfig;

/** A small reusable body; interpreted by the pool that owns it. */
public final class Entity {
    public boolean active, friendly;
    public float x, y, vx, vy, timer, progress, radius, tracking;
    public int health, maxHealth, value, damage;
    public float age, originX, aimX, aimY, lifetime, slowSeconds, repairTimer, effectTime;
    public boolean warned;
    public MissionConfig.Enemy enemy;
    public MissionConfig.Waste waste;
    void reset() {
        active = true;
        friendly = false;
        x = y = vx = vy = timer = progress = radius = tracking = 0;
        health = maxHealth = value = damage = 0;
        age = originX = aimX = aimY = lifetime = slowSeconds = repairTimer = effectTime = 0;
        warned = false; enemy = null; waste = null;
    }
}
