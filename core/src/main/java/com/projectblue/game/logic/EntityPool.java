package com.projectblue.game.logic;

/** Fixed-capacity pool, prewarmed at level creation. Exhaustion skips cosmetic/spawn work safely. */
public final class EntityPool {
    private final Entity[] items;
    public EntityPool(int capacity) {
        items = new Entity[capacity];
        for (int i = 0; i < capacity; i++) items[i] = new Entity();
    }
    public Entity obtain() {
        for (int i = 0; i < items.length; i++) {
            if (!items[i].active) { items[i].reset(); return items[i]; }
        }
        return null;
    }
    public int capacity() { return items.length; }
    public Entity at(int index) { return items[index]; }
    public int activeCount() {
        int count = 0;
        for (int i = 0; i < items.length; i++) if (items[i].active) count++;
        return count;
    }
}

