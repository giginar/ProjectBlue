package com.projectblue.game.logic;

/** A small reusable body; interpreted by the pool that owns it. */
public final class Entity {
    public boolean active, friendly;
    public float x, y, vx, vy, timer, progress, radius;
    public int health, value;
    void reset() {
        active = true;
        friendly = false;
        x = y = vx = vy = timer = progress = radius = 0;
        health = value = 0;
    }
}

