package com.projectblue.game.logic;
import com.projectblue.game.config.MissionConfig;

/** A small reusable body; interpreted by the pool that owns it. */
public final class Entity {
    public boolean active, friendly;
    public float x, y, vx, vy, timer, progress, radius, tracking;
    public int health, maxHealth, value, damage;
    public float age, originX, aimX, aimY, lifetime, slowSeconds, repairTimer, effectTime, shieldTime, hiddenTime;
    public boolean warned;
    public MissionConfig.Enemy enemy;
    public MissionConfig.Waste waste;
    public MissionConfig.Creature creature;
    public MissionConfig.EnvironmentKind environment;
    void reset() {
        active = true;
        friendly = false;
        x = y = vx = vy = timer = progress = radius = tracking = 0;
        health = maxHealth = value = damage = 0;
        age = originX = aimX = aimY = lifetime = slowSeconds = repairTimer = effectTime = shieldTime = hiddenTime = 0;
        warned = false; enemy = null; waste = null; creature = null; environment = null;
    }
}
