package com.projectblue.game.logic;

import com.projectblue.game.events.GameEvents;
import static com.projectblue.game.events.GameEvents.Type.*;
import static com.projectblue.game.config.GameConfig.*;

/** Pure Java simulation. No rendering, device input, platform or libGDX dependencies. */
public final class GameWorld {
    public final Entity player = new Entity();
    public final EntityPool bullets = new EntityPool(BULLET_CAPACITY);
    public final EntityPool drones = new EntityPool(DRONE_CAPACITY);
    public final EntityPool plastics = new EntityPool(ITEM_CAPACITY);
    public final EntityPool turtles = new EntityPool(TURTLE_CAPACITY);
    public final EntityPool salvage = new EntityPool(ITEM_CAPACITY);
    public final EntityPool particles = new EntityPool(PARTICLE_CAPACITY);
    public final GameEvents events = new GameEvents();
    private final RandomProvider random;
    // Separate stream: changing a visual effect must never change gameplay spawns.
    private final RandomProvider effects = RandomProvider.seeded(7);
    private float elapsed, shotTimer, invulnerability;
    private int spawnedDrones, spawnedPlastic, spawnedTurtles, kills, plasticCount, rescueCount, salvageCount;
    private boolean finished;
    private LevelResult result;

    public GameWorld(RandomProvider random) {
        this.random = random;
        player.reset();
        player.x = WIDTH / 2f;
        player.y = PLAYER_START_Y;
        player.radius = PLAYER_RADIUS;
        player.health = PLAYER_HEALTH;
    }
    public void update(float dt, boolean moving, float targetX, float targetY) {
        if (finished || dt <= 0 || !Float.isFinite(dt)) return;
        dt = Math.min(dt, STEP); // Callers use a fixed-step accumulator; never simulate a resume-time jump.
        elapsed = Math.min(LEVEL_SECONDS, elapsed + dt);
        invulnerability = Math.max(0, invulnerability - dt);
        if (moving) {
            float x = Rules.boundCenter(targetX, PLAYER_RADIUS, WIDTH);
            float y = Rules.clamp(targetY, PLAY_MIN_Y, PLAY_MAX_Y);
            float dx = x - player.x, dy = y - player.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            float fraction = distance > 0 ? Math.min(1, PLAYER_SPEED * dt / distance) : 0;
            player.x += dx * fraction;
            player.y += dy * fraction;
        }
        spawnScheduled();
        shotTimer -= dt;
        if (shotTimer <= 0) {
            spawnBullet(player.x, player.y + SHOT_OFFSET_Y, 0, PLAYER_BULLET_SPEED, true);
            shotTimer += SHOT_INTERVAL;
            events.emit(SHOT, player.x, player.y, 0);
        }
        updateDrones(dt);
        updateBullets(dt);
        updatePlastic(dt);
        updateTurtles(dt);
        updateSalvage(dt);
        updateParticles(dt);
        if (player.health <= 0 || elapsed >= LEVEL_SECONDS) {
            finished = true;
            result = new LevelResult(player.health > 0, kills, plasticCount, rescueCount, salvageCount, player.health);
            events.emit(FINISHED, player.x, player.y, result.score);
        }
    }
    private float spawnX() { return SPAWN_MARGIN + random.nextFloat() * (WIDTH - 2 * SPAWN_MARGIN); }
    private void spawnScheduled() {
        if (spawnedDrones < DRONE_COUNT && elapsed >= DRONE_FIRST + spawnedDrones * DRONE_INTERVAL) {
            Entity e = drones.obtain();
            if (e != null) {
                e.x = spawnX(); e.y = SPAWN_Y; e.vy = -DRONE_SPEED;
                e.radius = DRONE_RADIUS; e.health = DRONE_HEALTH;
                e.timer = ENEMY_SHOT_INTERVAL; e.vx = (random.nextFloat() * 2 - 1) * DRONE_DRIFT;
            }
            spawnedDrones++;
        }
        if (spawnedPlastic < PLASTIC_COUNT && elapsed >= PLASTIC_FIRST + spawnedPlastic * PLASTIC_INTERVAL) {
            Entity e = plastics.obtain();
            if (e != null) {
                e.x = spawnX(); e.y = SPAWN_Y; e.vy = -PLASTIC_SPEED; e.radius = PLASTIC_RADIUS;
            }
            spawnedPlastic++;
        }
        if (spawnedTurtles < TURTLE_COUNT && elapsed >= TURTLE_FIRST + spawnedTurtles * TURTLE_INTERVAL) {
            Entity e = turtles.obtain();
            if (e != null) {
                e.x = spawnX(); e.y = SPAWN_Y; e.radius = TURTLE_RADIUS; e.vy = -TURTLE_SPEED;
            }
            spawnedTurtles++;
        }
    }
    private void updateDrones(float dt) {
        for (int i = 0; i < drones.capacity(); i++) {
            Entity e = drones.at(i);
            if (!e.active) continue;
            e.y += e.vy * dt;
            e.x += e.vx * dt;
            if (e.x < SPAWN_MARGIN || e.x > WIDTH - SPAWN_MARGIN) e.vx = -e.vx;
            e.timer -= dt;
            if (e.timer <= 0 && e.y < PLAY_MAX_Y + DRONE_RADIUS && e.y > 0) {
                float dx = player.x - e.x, dy = player.y - e.y;
                float length = Math.max(1, (float) Math.sqrt(dx * dx + dy * dy));
                spawnBullet(e.x, e.y, dx / length * ENEMY_BULLET_SPEED, dy / length * ENEMY_BULLET_SPEED, false);
                e.timer = ENEMY_SHOT_INTERVAL;
            }
            if (Rules.overlaps(e.x, e.y, e.radius, player.x, player.y, player.radius)) hitPlayer(CONTACT_DAMAGE);
            if (e.y < -e.radius) e.active = false;
        }
    }
    private void spawnBullet(float x, float y, float vx, float vy, boolean friendly) {
        Entity b = bullets.obtain();
        if (b == null) return;
        b.x = x; b.y = y; b.vx = vx; b.vy = vy; b.friendly = friendly; b.radius = BULLET_RADIUS;
    }
    private void updateBullets(float dt) {
        for (int i = 0; i < bullets.capacity(); i++) {
            Entity b = bullets.at(i);
            if (!b.active) continue;
            b.x += b.vx * dt; b.y += b.vy * dt;
            if (b.y < -DESPAWN_MARGIN || b.y > SPAWN_Y || b.x < -DESPAWN_MARGIN || b.x > WIDTH + DESPAWN_MARGIN) { b.active = false; continue; }
            if (b.friendly) {
                for (int j = 0; j < drones.capacity(); j++) {
                    Entity e = drones.at(j);
                    if (!e.active || !Rules.overlaps(b.x, b.y, b.radius, e.x, e.y, e.radius)) continue;
                    b.active = false;
                    e.health = Rules.damage(e.health, PLAYER_DAMAGE);
                    if (e.health == 0) {
                        e.active = false; kills++;
                        burst(e.x, e.y, 0);
                        Entity drop = salvage.obtain();
                        if (drop != null) { drop.x = e.x; drop.y = e.y; drop.value = SALVAGE_PER_KILL; }
                        events.emit(DRONE_DESTROYED, e.x, e.y, KILL_SCORE);
                    }
                    break;
                }
            } else if (Rules.overlaps(b.x, b.y, b.radius, player.x, player.y, player.radius)) {
                b.active = false;
                hitPlayer(ENEMY_DAMAGE);
            }
        }
    }
    private void hitPlayer(int damage) {
        if (invulnerability > 0) return;
        player.health = Rules.damage(player.health, damage);
        invulnerability = INVULNERABILITY;
        events.emit(PLAYER_HIT, player.x, player.y, damage);
    }
    private boolean near(Entity e, float radius) {
        return Rules.overlaps(e.x, e.y, 0, player.x, player.y, radius);
    }
    private void updatePlastic(float dt) {
        for (int i = 0; i < plastics.capacity(); i++) {
            Entity e = plastics.at(i);
            if (!e.active) continue;
            e.y += e.vy * dt;
            if (near(e, CLEAN_RADIUS)) {
                e.progress += dt;
                if (e.progress >= CLEAN_SECONDS) {
                    e.active = false; plasticCount++; burst(e.x, e.y, 1);
                    events.emit(PLASTIC_COLLECTED, e.x, e.y, PLASTIC_SCORE);
                }
            } else e.progress = 0;
            if (e.y < -DESPAWN_MARGIN) e.active = false;
        }
    }
    private void updateTurtles(float dt) {
        for (int i = 0; i < turtles.capacity(); i++) {
            Entity e = turtles.at(i);
            if (!e.active) continue;
            if (e.friendly) {
                e.x += FREED_TURTLE_SPEED_X * dt; e.y += FREED_TURTLE_SPEED_Y * dt;
                if (e.x > WIDTH + DESPAWN_MARGIN) e.active = false;
                continue;
            }
            e.y += e.vy * dt;
            if (near(e, RESCUE_RADIUS)) {
                e.progress += dt;
                if (e.progress >= RESCUE_SECONDS) {
                    e.friendly = true; rescueCount++; burst(e.x, e.y, 1);
                    events.emit(TURTLE_RESCUED, e.x, e.y, RESCUE_SCORE);
                }
            } else e.progress = 0; // A continuous short stay is required.
            if (e.y < -DESPAWN_MARGIN) e.active = false;
        }
    }
    private void updateSalvage(float dt) {
        for (int i = 0; i < salvage.capacity(); i++) {
            Entity e = salvage.at(i);
            if (!e.active) continue;
            if (near(e, PICKUP_RADIUS)) {
                e.active = false; salvageCount += e.value;
                events.emit(SALVAGE_COLLECTED, e.x, e.y, e.value);
            } else if (near(e, SALVAGE_RADIUS)) {
                float dx = player.x - e.x, dy = player.y - e.y;
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                float ratio = Math.min(1, SALVAGE_SPEED * dt / length);
                e.x += dx * ratio; e.y += dy * ratio;
            } else e.y -= PLASTIC_SPEED * dt;
            if (e.y < -DESPAWN_MARGIN) e.active = false;
        }
    }
    private void burst(float x, float y, int color) {
        for (int i = 0; i < PARTICLES_PER_BURST; i++) {
            Entity e = particles.obtain();
            if (e == null) return;
            double angle = effects.nextFloat() * Math.PI * 2;
            float speed = PARTICLE_SPEED * (.4f + effects.nextFloat());
            e.x = x; e.y = y; e.vx = (float) Math.cos(angle) * speed;
            e.vy = (float) Math.sin(angle) * speed; e.timer = PARTICLE_LIFE; e.value = color;
        }
    }
    private void updateParticles(float dt) {
        for (int i = 0; i < particles.capacity(); i++) {
            Entity e = particles.at(i);
            if (!e.active) continue;
            e.x += e.vx * dt; e.y += e.vy * dt; e.timer -= dt;
            if (e.timer <= 0) e.active = false;
        }
    }
    public float elapsed() { return elapsed; }
    public float progress() { return elapsed / LEVEL_SECONDS; }
    public float restoration() { return (Rules.cleanup(plasticCount) * CLEANUP_RESTORE_WEIGHT + Rules.rescue(rescueCount) * RESCUE_RESTORE_WEIGHT) / 100f; }
    public int kills() { return kills; }
    public int plasticCount() { return plasticCount; }
    public int rescueCount() { return rescueCount; }
    public int salvageCount() { return salvageCount; }
    public int score() { return Rules.score(kills, plasticCount, rescueCount, salvageCount, player.health, false); }
    public boolean invulnerable() { return invulnerability > 0; }
    public boolean finished() { return finished; }
    public LevelResult result() { return result; }
}
