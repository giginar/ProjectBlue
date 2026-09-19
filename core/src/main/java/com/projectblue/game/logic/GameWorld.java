package com.projectblue.game.logic;

import com.projectblue.game.events.GameEvents;
import com.projectblue.game.config.MissionConfig;
import com.projectblue.game.config.RunSpec;
import com.projectblue.game.logic.weapons.WeaponController;
import static com.projectblue.game.events.GameEvents.Type.*;
import static com.projectblue.game.config.GameConfig.*;

/** Pure Java simulation. No rendering, device input, platform or libGDX dependencies. */
public final class GameWorld {
    public final Entity player = new Entity();
    public final Entity boss = new Entity();
    public final Entity bossLeftPipe = new Entity();
    public final Entity bossRightPipe = new Entity();
    public final Entity laser = new Entity();
    public final EntityPool bullets = new EntityPool(BULLET_CAPACITY);
    public final EntityPool drones = new EntityPool(DRONE_CAPACITY);
    public final EntityPool plastics = new EntityPool(ITEM_CAPACITY);
    public final EntityPool turtles = new EntityPool(TURTLE_CAPACITY);
    public final EntityPool salvage = new EntityPool(ITEM_CAPACITY);
    public final EntityPool particles = new EntityPool(PARTICLE_CAPACITY);
    public final EntityPool corals = new EntityPool(8);
    public final GameEvents events = new GameEvents();
    private final RandomProvider random;
    private final RunSpec spec;
    private final MissionConfig mission;
    private final SpawnTimeline timeline;
    private final ShorelineCompactor compactor;
    // Separate stream: changing a visual effect must never change gameplay spawns.
    private final RandomProvider effects = RandomProvider.seeded(7);
    private float elapsed, invulnerability, slowTimer, recoveryTimer;
    private final WeaponController weapons;
    private int shield, damageTaken, cleanedCount, coralDamage, combatScore, enemiesEncountered;
    private int spawnedDrones, spawnedPlastic, spawnedTurtles, kills, plasticCount, rescueCount, salvageCount;
    private boolean finished, bossSpawned, cleaning, recovering;
    private LevelResult result;

    public GameWorld(RandomProvider random) {
        this(random, RunSpec.original());
    }
    public GameWorld(RandomProvider random, RunSpec spec) {
        this.random = random;
        this.spec = spec;
        mission = spec.mission();
        timeline = mission == null ? null : new SpawnTimeline(mission, spec.tuning().spawnDensity());
        compactor = mission == null ? null : new ShorelineCompactor(mission.boss, spec.tuning().health(), spec.tuning().bossCadence());
        weapons = new WeaponController(spec.loadout());
        shield = spec.loadout().shieldCapacity();
        player.reset();
        player.x = WIDTH / 2f;
        player.y = PLAYER_START_Y;
        player.radius = PLAYER_RADIUS;
        player.health = player.maxHealth = spec.loadout().health();
    }
    public void update(float dt, boolean moving, float targetX, float targetY) {
        if (finished || dt <= 0 || !Float.isFinite(dt)) return;
        dt = Math.min(dt, STEP); // Callers use a fixed-step accumulator; never simulate a resume-time jump.
        elapsed = Math.min(levelDeadline(), elapsed + dt);
        invulnerability = Math.max(0, invulnerability - dt);
        slowTimer = Math.max(0, slowTimer - dt);
        cleaning = mission != null && cleaningInRange();
        if (moving) {
            float x = Rules.boundCenter(targetX, PLAYER_RADIUS, WIDTH);
            float y = Rules.clamp(targetY, PLAY_MIN_Y, PLAY_MAX_Y);
            float dx = x - player.x, dy = y - player.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            float speed = spec.loadout().speed();
            if (cleaning) speed *= mission.cleaningSpeedMultiplier;
            if (slowTimer > 0) speed *= mission.netSpeedMultiplier;
            float fraction = distance > 0 ? Math.min(1, speed * dt / distance) : 0;
            player.x += dx * fraction;
            player.y += dy * fraction;
        }
        if (mission == null) spawnScheduled();
        else if (!recovering) timeline.advance(elapsed, this::spawnMission);
        laser.timer = Math.max(0, laser.timer - dt);
        weapons.update(this, dt);
        updateDrones(dt);
        updateBoss(dt);
        updateBullets(dt);
        updatePlastic(dt);
        updateTurtles(dt);
        if (mission != null) updateCorals(dt);
        updateSalvage(dt);
        updateParticles(dt);
        if (mission != null) updateMissionEnd(dt);
        else if (player.health <= 0 || elapsed >= LEVEL_SECONDS)
            finishLegacy(player.health > 0 && !boss.active);
    }
    private float levelDeadline() { return mission == null ? LEVEL_SECONDS : mission.deadlineSeconds; }
    private void finishLegacy(boolean completed) {
        if (finished) return;
        finished = true;
        result = new LevelResult(spec, completed, kills, plasticCount, rescueCount, salvageCount, player.health, damageTaken);
        events.emit(FINISHED, player.x, player.y, result.score);
    }
    private void updateMissionEnd(float dt) {
        if (recovering) {
            recoveryTimer += dt;
            if (recoveryTimer >= mission.recoverySeconds) finishMission(true);
        } else if (player.health <= 0 || elapsed >= mission.deadlineSeconds) finishMission(false);
    }
    private void finishMission(boolean completed) {
        if (finished) return;
        finished = true;
        MissionOutcome outcome = new MissionOutcome(kills, Math.max(1, enemiesEncountered), plasticCount, cleanedCount,
            rescueCount, salvageCount, player.health, damageTaken, coralDamage, combatScore, restoration());
        result = new LevelResult(spec, completed, outcome);
        events.emit(FINISHED, player.x, player.y, result.score);
    }
    private float spawnX() { return SPAWN_MARGIN + random.nextFloat() * (WIDTH - 2 * SPAWN_MARGIN); }
    private void spawnMission(SpawnTimeline.Event event) {
        switch (event.kind()) {
            case ENEMY -> spawnEnemy(event.enemy(), event.x(), SPAWN_Y);
            case WASTE -> {
                Entity e = plastics.obtain();
                if (e != null) {
                    e.x=event.x(); e.y=SPAWN_Y; e.radius=event.waste().radius(); e.vy=-event.waste().drift();
                    e.waste=event.waste(); spawnedPlastic++;
                }
            }
            case TURTLE -> {
                Entity e=turtles.obtain();
                if (e!=null) { e.x=event.x(); e.y=SPAWN_Y; e.radius=TURTLE_RADIUS; e.vy=-TURTLE_SPEED; spawnedTurtles++; }
            }
            case CORAL -> {
                Entity e=corals.obtain();
                if (e!=null) { e.x=event.x(); e.y=SPAWN_Y; e.radius=42; e.health=e.maxHealth=30; e.vy=-27; }
            }
        }
    }
    private void spawnEnemy(MissionConfig.Enemy definition,float x,float y) {
        Entity e=drones.obtain();
        if (e==null || definition==null) return;
        e.enemy=definition; e.x=e.originX=x; e.y=y; e.radius=definition.stats().radius();
        e.health=e.maxHealth=Math.round(definition.stats().health()*spec.tuning().health());
        e.timer=.8f+random.nextFloat(); e.repairTimer=1; spawnedDrones++; enemiesEncountered++;
    }
    private void spawnScheduled() {
        if (spawnedDrones < spec.tuning().droneCount() && elapsed >= DRONE_FIRST + spawnedDrones * spec.tuning().droneInterval()) {
            Entity e = drones.obtain();
            if (e != null) {
                e.x = spawnX(); e.y = SPAWN_Y; e.vy = -DRONE_SPEED;
                e.radius = DRONE_RADIUS; e.health = e.maxHealth = spec.tuning().droneHealth();
                e.timer = spec.tuning().shotInterval(); e.vx = (random.nextFloat() * 2 - 1) * DRONE_DRIFT;
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
            if (mission != null && e.enemy != null) {
                EnemySystems.update(this,e,dt);
                if (e.active && Rules.overlaps(e.x,e.y,e.radius,player.x,player.y,player.radius)) hitPlayer(CONTACT_DAMAGE);
                continue;
            }
            e.y += e.vy * dt;
            e.x += e.vx * dt;
            if (e.x < SPAWN_MARGIN || e.x > WIDTH - SPAWN_MARGIN) e.vx = -e.vx;
            e.timer -= dt;
            if (e.timer <= 0 && e.y < PLAY_MAX_Y + DRONE_RADIUS && e.y > 0) {
                float dx = player.x - e.x, dy = player.y - e.y;
                float length = Math.max(1, (float) Math.sqrt(dx * dx + dy * dy));
                spawnBullet(e.x, e.y, dx / length * spec.tuning().shotSpeed(), dy / length * spec.tuning().shotSpeed(), false);
                e.timer = spec.tuning().shotInterval();
            }
            if (Rules.overlaps(e.x, e.y, e.radius, player.x, player.y, player.radius)) hitPlayer(CONTACT_DAMAGE);
            if (e.y < -e.radius) e.active = false;
        }
    }
    private void updateBoss(float dt) {
        if (mission != null) { updateCompactor(dt); return; }
        if (spec.hasBoss() && !bossSpawned && elapsed >= 130) {
            bossSpawned = true; boss.reset();
            boss.x = WIDTH / 2f; boss.y = 810; boss.radius = 48;
            boss.health = boss.maxHealth = spec.tuning().bossHealth();
            boss.timer = 3; // Entry warning before the first attack.
        }
        if (!boss.active) return;
        boss.y = Math.max(720, boss.y - 45 * dt);
        boss.x = WIDTH / 2f + (float) Math.sin((elapsed - 130) * .55f * spec.tuning().bossMovement()) * 165;
        boss.timer -= dt;
        if (boss.timer <= 0) {
            int phase = bossPhase();
            int shots = spec.tuning().bossProjectiles();
            double aim = Math.atan2(player.y - boss.y, player.x - boss.x);
            // Later phases alternate aimed fans and sweeping curtains. No per-shot allocation.
            if (phase >= 2 && boss.value % 2 == 1) aim = -Math.PI / 2 + Math.sin(elapsed * 1.8) * .65;
            for (int i = 0; i < shots; i++) {
                double angle = aim + (i - (shots - 1) / 2f) * .16;
                float speed = spec.tuning().shotSpeed();
                spawnBullet(boss.x, boss.y - 42, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, false);
            }
            if (phase >= 3) {
                // Flanking shots force movement through different lanes in the final phase.
                spawnBullet(boss.x - 65, boss.y, -65, -spec.tuning().shotSpeed(), false);
                spawnBullet(boss.x + 65, boss.y, 65, -spec.tuning().shotSpeed(), false);
            }
            boss.value++;
            boss.timer = spec.tuning().bossInterval() / (1 + (phase - 1) * .15f);
        }
        if (Rules.overlaps(boss.x, boss.y, boss.radius, player.x, player.y, player.radius)) hitPlayer(CONTACT_DAMAGE);
    }
    private void updateCompactor(float dt) {
        if (!bossSpawned && elapsed >= mission.boss.start()) {
            bossSpawned=true; timeline.stop(); compactor.start(); boss.reset();
            boss.x=WIDTH/2f; boss.y=830; boss.radius=62; boss.health=boss.maxHealth=compactor.maxHealth();
            enemiesEncountered++;
        }
        if (!bossSpawned || recovering) return;
        compactor.update(dt);
        boss.x=WIDTH/2f; boss.y=Math.max(748,830-compactor.stateTime()*32); boss.health=compactor.health();
        boolean pipes=compactor.state()==ShorelineCompactor.State.PIPES;
        configurePipe(bossLeftPipe,145,boss.y-12,compactor.pipeHealth(true),pipes);
        configurePipe(bossRightPipe,395,boss.y-12,compactor.pipeHealth(false),pipes);
        if (compactor.pressesActive() && (player.x<mission.boss.pressInset() || player.x>WIDTH-mission.boss.pressInset())) hitPlayer(CONTACT_DAMAGE);
        if (compactor.consumeVolley()) fireBossVolley();
        if (compactor.consumeDrone()) spawnEnemy(mission.enemy("SCOUT"),boss.x,boss.y-45);
        if (compactor.defeated()) beginRecovery();
    }
    private void configurePipe(Entity pipe,float x,float y,int health,boolean active) {
        pipe.active=active && health>0; pipe.x=x; pipe.y=y; pipe.radius=31; pipe.health=health; pipe.maxHealth=compactor.maxPipeHealth();
    }
    private void fireBossVolley() {
        int count=2+compactor.phase();
        double aim=Math.atan2(player.y-boss.y,player.x-boss.x);
        for (int i=0;i<count;i++) {
            double angle=aim+(i-(count-1)/2f)*.24;
            hostileProjectile(boss.x,boss.y-48,(float)Math.cos(angle)*spec.tuning().shotSpeed(),
                (float)Math.sin(angle)*spec.tuning().shotSpeed(),8,0);
        }
        if (compactor.phase()==1) spawnBossWaste();
    }
    private void spawnBossWaste() {
        Entity e=plastics.obtain();
        if (e==null) return;
        MissionConfig.Waste waste=mission.waste(compactor.dronesLaunched()%2==0 ? MissionConfig.WasteKind.METAL : MissionConfig.WasteKind.BAG);
        e.x=boss.x+(random.nextFloat()*2-1)*80; e.y=boss.y-35; e.radius=waste.radius(); e.vy=-waste.drift(); e.waste=waste;
    }
    private void beginRecovery() {
        if (recovering) return;
        recovering=true; boss.active=false; bossLeftPipe.active=bossRightPipe.active=false; timeline.stop();
        kills++; combatScore+=500; salvageCount+=mission.boss.salvage();
        for (int i=0;i<bullets.capacity();i++) if (!bullets.at(i).friendly) bullets.at(i).active=false;
        for (int i=0;i<drones.capacity();i++) drones.at(i).active=false;
        burst(boss.x,boss.y,1);
    }
    public int bossPhase() {
        if (mission != null) return compactor.phase();
        if (!boss.active) return 0;
        float remaining = (float) boss.health / boss.maxHealth;
        return Math.min(spec.tuning().bossPhases(), remaining > .66f ? 1 : remaining > .33f ? 2 : 3);
    }
    private void spawnBullet(float x, float y, float vx, float vy, boolean friendly) {
        if (friendly) playerProjectile(x,y,vx,vy,spec.loadout().damage(),0);
        else hostileProjectile(x,y,vx,vy,ENEMY_DAMAGE,0);
    }
    public void hostileProjectile(float x,float y,float vx,float vy,int damage,float slowSeconds) {
        if (mission != null && hostileBulletCount() >= mission.hostileBulletLimit) return;
        Entity b=bullets.obtain();
        if (b==null) return;
        b.x=x; b.y=y; b.vx=vx; b.vy=vy; b.friendly=false; b.radius=slowSeconds>0?8:BULLET_RADIUS;
        b.damage=Math.max(0,damage); b.slowSeconds=Math.max(0,slowSeconds);
    }
    private int hostileBulletCount() {
        int count=0;
        for (int i=0;i<bullets.capacity();i++) if (bullets.at(i).active&&!bullets.at(i).friendly) count++;
        return count;
    }
    private void updateBullets(float dt) {
        for (int i = 0; i < bullets.capacity(); i++) {
            Entity b = bullets.at(i);
            if (!b.active) continue;
            if (b.tracking > 0) {
                Entity target = nearestEnemy(b.x, b.y);
                if (target != null) {
                    float dx = target.x - b.x, dy = target.y - b.y;
                    float length = Math.max(.001f, (float)Math.sqrt(dx * dx + dy * dy));
                    float speed = (float)Math.sqrt(b.vx * b.vx + b.vy * b.vy);
                    float blend = Math.min(1, b.tracking * dt);
                    b.vx += (dx / length * speed - b.vx) * blend;
                    b.vy += (dy / length * speed - b.vy) * blend;
                    float adjusted = Math.max(.001f, (float)Math.sqrt(b.vx * b.vx + b.vy * b.vy));
                    b.vx *= speed / adjusted; b.vy *= speed / adjusted;
                }
            }
            b.x += b.vx * dt; b.y += b.vy * dt;
            if (b.y < -DESPAWN_MARGIN || b.y > SPAWN_Y || b.x < -DESPAWN_MARGIN || b.x > WIDTH + DESPAWN_MARGIN) { b.active = false; continue; }
            if (b.friendly) {
                for (int j = 0; j < drones.capacity(); j++) {
                    Entity e = drones.at(j);
                    if (!e.active || !Rules.overlaps(b.x, b.y, b.radius, e.x, e.y, e.radius)) continue;
                    b.active = false;
                    damageEnemy(e, b.damage, b.x, b.y-b.vy*dt);
                    break;
                }
                if (mission != null && b.active && hitCoral(b)) continue;
                if (mission != null && b.active && hitCompactor(b)) continue;
                if (b.active && boss.active && Rules.overlaps(b.x, b.y, b.radius, boss.x, boss.y, boss.radius)) {
                    b.active = false;
                    damageEnemy(boss, b.damage);
                }
            } else if (Rules.overlaps(b.x, b.y, b.radius, player.x, player.y, player.radius)) {
                b.active = false;
                if (b.slowSeconds>0) slowTimer=Math.max(slowTimer,b.slowSeconds);
                // Tests and legacy callers may create a hostile pooled projectile directly.
                // Keep the pre-mission default damage for those projectiles.
                hitPlayer(b.damage > 0 ? b.damage : ENEMY_DAMAGE);
            }
        }
    }
    private boolean hitCoral(Entity bullet) {
        for (int i=0;i<corals.capacity();i++) {
            Entity coral=corals.at(i);
            if (!coral.active || !Rules.overlaps(bullet.x,bullet.y,bullet.radius,coral.x,coral.y,coral.radius)) continue;
            bullet.active=false;
            int before=coral.health; coral.health=Rules.damage(coral.health,bullet.damage);
            coralDamage+=before-coral.health;
            if (coral.health==0) coral.active=false;
            return true;
        }
        return false;
    }
    private boolean hitCompactor(Entity bullet) {
        if (!bossSpawned || !boss.active) return false;
        if (bossLeftPipe.active && Rules.overlaps(bullet.x,bullet.y,bullet.radius,bossLeftPipe.x,bossLeftPipe.y,bossLeftPipe.radius)) {
            bullet.active=false; compactor.hitPipe(true,bullet.damage); bossLeftPipe.health=compactor.pipeHealth(true); return true;
        }
        if (bossRightPipe.active && Rules.overlaps(bullet.x,bullet.y,bullet.radius,bossRightPipe.x,bossRightPipe.y,bossRightPipe.radius)) {
            bullet.active=false; compactor.hitPipe(false,bullet.damage); bossRightPipe.health=compactor.pipeHealth(false); return true;
        }
        if (Rules.overlaps(bullet.x,bullet.y,bullet.radius,boss.x,boss.y,boss.radius)) {
            bullet.active=false; compactor.hitCore(bullet.damage); boss.health=compactor.health(); return true;
        }
        return false;
    }
    private void hitPlayer(int damage) {
        if (invulnerability > 0) return;
        int absorbed = Math.min(shield, damage);
        shield -= absorbed;
        damageTaken += Math.min(player.health, damage - absorbed) + absorbed;
        player.health = Rules.damage(player.health, damage - absorbed);
        invulnerability = INVULNERABILITY;
        events.emit(PLAYER_HIT, player.x, player.y, damage);
    }
    private boolean near(Entity e, float radius) {
        return Rules.overlaps(e.x, e.y, 0, player.x, player.y, radius);
    }
    private boolean cleaningInRange() {
        for (int i=0;i<plastics.capacity();i++) {
            Entity e=plastics.at(i);
            if (e.active && near(e,spec.loadout().cleanupRadius())) return true;
        }
        return false;
    }
    private void updatePlastic(float dt) {
        for (int i = 0; i < plastics.capacity(); i++) {
            Entity e = plastics.at(i);
            if (!e.active) continue;
            e.y += e.vy * dt;
            if (near(e, spec.loadout().cleanupRadius())) {
                e.progress += dt;
                float multiplier=e.waste==null?1:e.waste.cleanMultiplier();
                if (e.progress >= spec.loadout().cleanupSeconds()*multiplier) {
                    e.active = false; cleanedCount++;
                    if (e.waste==null || e.waste.plastic()) plasticCount++;
                    if (e.waste!=null) salvageCount+=e.waste.salvage();
                    burst(e.x, e.y, 1);
                    events.emit(PLASTIC_COLLECTED, e.x, e.y, PLASTIC_SCORE);
                }
            } else e.progress = 0;
            if (e.y < -DESPAWN_MARGIN) e.active = false;
        }
    }
    private void updateCorals(float dt) {
        for (int i=0;i<corals.capacity();i++) {
            Entity e=corals.at(i);
            if (!e.active) continue;
            e.y+=e.vy*dt;
            if (e.y < -e.radius) e.active=false;
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
                if (e.progress >= spec.loadout().rescueSeconds()) {
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
            } else if (near(e, spec.loadout().salvageRadius())) {
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
    public int shield() { return shield; }
    public int damageTaken() { return damageTaken; }
    public float supportX() { return Rules.clamp(player.x + (float)Math.cos(elapsed * 2) * 48, 12, WIDTH - 12); }
    public float supportY() { return player.y + (float)Math.sin(elapsed * 2) * 28; }
    public void playerProjectile(float x, float y, float vx, float vy, int damage, float tracking) {
        Entity b = bullets.obtain();
        if (b == null) return;
        b.x = x; b.y = y; b.vx = vx; b.vy = vy; b.friendly = true; b.radius = BULLET_RADIUS;
        b.damage = damage; b.tracking = tracking;
    }
    public Entity nearestEnemy(float x, float y) {
        Entity closest = bossLeftPipe.active ? bossLeftPipe : bossRightPipe.active ? bossRightPipe : boss.active && (mission==null || compactor.coreVulnerable()) ? boss : null;
        float distance = closest == null ? Float.MAX_VALUE : distanceSquared(closest, x, y);
        for (int i = 0; i < drones.capacity(); i++) {
            Entity e = drones.at(i);
            if (e.active && distanceSquared(e, x, y) < distance) { closest = e; distance = distanceSquared(e, x, y); }
        }
        return closest;
    }
    private float distanceSquared(Entity e, float x, float y) { return (e.x-x)*(e.x-x) + (e.y-y)*(e.y-y); }
    public void fireLaser(int damage) {
        Entity target = null;
        float y = SPAWN_Y;
        for (int i = 0; i <= drones.capacity() + 2; i++) {
            Entity e = i < drones.capacity() ? drones.at(i) : i == drones.capacity() ? boss
                : i == drones.capacity() + 1 ? bossLeftPipe : bossRightPipe;
            if (e.active && e.y > player.y && e.y < y && Math.abs(e.x-player.x) <= e.radius + 3) { target = e; y = e.y; }
        }
        laser.x = player.x; laser.y = player.y + SHOT_OFFSET_Y; laser.vy = y; laser.timer = .09f;
        if (target != null) {
            if (mission!=null && (target==boss || target==bossLeftPipe || target==bossRightPipe)) {
                if (target==bossLeftPipe) compactor.hitPipe(true,damage);
                else if (target==bossRightPipe) compactor.hitPipe(false,damage);
                else compactor.hitCore(damage);
            } else damageEnemy(target,damage,player.x,player.y);
        }
    }
    private void damageEnemy(Entity e, int damage) {
        damageEnemy(e,damage,e.x,e.y-1);
    }
    private void damageEnemy(Entity e,int damage,float impactX,float sourceY) {
        if (!e.active) return;
        if (mission!=null && e.enemy!=null && EnemySystems.armoredHit(e,impactX,sourceY)) { e.effectTime=.18f; return; }
        e.health = Rules.damage(e.health, damage);
        if (e.health != 0) return;
        e.active = false; kills++; burst(e.x, e.y, 0);
        Entity drop = salvage.obtain();
        int reward=e.enemy==null?SALVAGE_PER_KILL*(e==boss?5:1):e.enemy.reward().salvage();
        int points=e.enemy==null?KILL_SCORE:e.enemy.reward().score();
        if (drop != null) { drop.x = e.x; drop.y = e.y; drop.value = reward; }
        combatScore+=points;
        events.emit(DRONE_DESTROYED, e.x, e.y, points);
    }
    public RunSpec spec() { return spec; }
    public MissionConfig mission() { return mission; }
    public ShorelineCompactor compactor() { return compactor; }
    public int spawnedDrones() { return spawnedDrones; }
    public float progress() { return elapsed / (mission==null?LEVEL_SECONDS:mission.durationSeconds); }
    public float restoration() {
        if (mission==null) return (Rules.cleanup(plasticCount)*CLEANUP_RESTORE_WEIGHT+Rules.rescue(rescueCount)*RESCUE_RESTORE_WEIGHT)/100f;
        float cleanup=Rules.percentage(cleanedCount,mission.wasteCount)/100f;
        float rescue=Rules.percentage(rescueCount,mission.turtleCount)/100f;
        float coral=1-Rules.percentage(coralDamage,mission.coralCount*30)/100f;
        float base=Rules.clamp(cleanup*.55f+rescue*.3f+coral*.15f,0,1);
        return recovering ? base+(1-base)*Rules.clamp(recoveryTimer/mission.recoverySeconds,0,1) : base;
    }
    public int kills() { return kills; }
    public int plasticCount() { return plasticCount; }
    public int rescueCount() { return rescueCount; }
    public int salvageCount() { return salvageCount; }
    public int score() { return Rules.score(0,cleanedCount,rescueCount,salvageCount,player.health,false)+combatScore; }
    public int cleanedCount() { return mission==null?plasticCount:cleanedCount; }
    public int wasteTotal() { return mission==null?PLASTIC_COUNT:mission.wasteCount; }
    public int turtleTotal() { return mission==null?TURTLE_COUNT:mission.turtleCount; }
    public int coralDamage() { return coralDamage; }
    public boolean cleaning() { return cleaning; }
    public boolean slowed() { return slowTimer>0; }
    public boolean recovering() { return recovering; }
    public float recoveryProgress() { return mission==null?0:Rules.clamp(recoveryTimer/mission.recoverySeconds,0,1); }
    public int hostileBullets() { return hostileBulletCount(); }
    public boolean invulnerable() { return invulnerability > 0; }
    public boolean finished() { return finished; }
    public LevelResult result() { return result; }
}
