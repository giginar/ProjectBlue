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
    public final EntityPool environments = new EntityPool(24);
    public final EntityPool hazards = new EntityPool(24);
    public final GameEvents events = new GameEvents();
    private final RandomProvider random;
    private final RunSpec spec;
    private final MissionConfig mission;
    private final SpawnTimeline timeline;
    private final ShorelineCompactor compactor;
    private final ReefBreaker reefBreaker;
    private final GhostNetHarvester harvester;
    private final UrbanSalvager urbanSalvager;
    private final OilKraken oilKraken;
    // Separate stream: changing a visual effect must never change gameplay spawns.
    private final RandomProvider effects = RandomProvider.seeded(7);
    private float elapsed, invulnerability, slowTimer, recoveryTimer, visibilityExposure, hazardDamageTimer;
    private final WeaponController weapons;
    private int shield, damageTaken, cleanedCount, coralDamage, combatScore, enemiesEncountered;
    private int spawnedDrones, spawnedPlastic, spawnedTurtles, kills, plasticCount, rescueCount, salvageCount;
    private int oilSpawned, oilCleaned, valvesClosed, bossOilTotal;
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
        compactor = mission != null && mission.boss.kind()==MissionConfig.BossKind.SHORELINE_COMPACTOR
            ? new ShorelineCompactor(mission.boss, spec.tuning().health(), spec.tuning().bossCadence()) : null;
        reefBreaker = mission != null && mission.boss.kind()==MissionConfig.BossKind.REEF_BREAKER
            ? new ReefBreaker(mission.boss,spec.tuning().health(),spec.tuning().bossCadence()) : null;
        harvester = mission != null && mission.boss.kind()==MissionConfig.BossKind.GHOST_NET_HARVESTER
            ? new GhostNetHarvester(mission.boss,spec.tuning().health(),spec.tuning().bossCadence()) : null;
        urbanSalvager = mission != null && mission.boss.kind()==MissionConfig.BossKind.URBAN_SALVAGER
            ? new UrbanSalvager(mission.boss,spec.tuning().health(),spec.tuning().bossCadence()) : null;
        oilKraken = mission != null && mission.boss.kind()==MissionConfig.BossKind.OIL_KRAKEN
            ? new OilKraken(mission.boss,spec.tuning().health(),spec.tuning().bossCadence()) : null;
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
        hazardDamageTimer=Math.max(0,hazardDamageTimer-dt);
        visibilityExposure=mission!=null && mission.type==MissionConfig.MissionType.BLACK_TIDE ? .18f : 0;
        cleaning = mission != null && cleaningInRange();
        EnvironmentSystems.Route activeRoute=route();
        if (moving) {
            float x = Rules.clamp(targetX,activeRoute.left()+PLAYER_RADIUS,activeRoute.right()-PLAYER_RADIUS);
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
        player.x=Rules.clamp(player.x,activeRoute.left()+PLAYER_RADIUS,activeRoute.right()-PLAYER_RADIUS);
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
        if (mission != null) { updateEnvironments(dt); updateHazards(dt); }
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
            case TURTLE, CREATURE -> {
                Entity e=turtles.obtain();
                if (e!=null) {
                    MissionConfig.Creature creature=event.creature()==null?mission.creature(MissionConfig.CreatureKind.TURTLE):event.creature();
                    e.x=event.x(); e.y=SPAWN_Y; e.radius=creature.radius(); e.vy=-creature.drift(); e.creature=creature;
                    e.lifetime=creature.timeoutSeconds(); spawnedTurtles++;
                }
            }
            case CORAL -> {
                Entity e=corals.obtain();
                if (e!=null) { e.x=event.x(); e.y=SPAWN_Y; e.radius=42; e.health=e.maxHealth=30; e.vy=-27; }
            }
            case MECHANIC -> spawnEnvironment(event.environment(),event.x(),SPAWN_Y);
        }
    }
    private void spawnEnemy(MissionConfig.Enemy definition,float x,float y) {
        Entity e=drones.obtain();
        if (e==null || definition==null) return;
        e.enemy=definition; e.x=e.originX=x; e.y=y; e.radius=definition.stats().radius();
        if (definition.movement()==MissionConfig.Movement.BURROW) {
            Entity coral=nearestCoral(x,y);
            if (coral!=null) { e.x=e.originX=coral.x; e.y=coral.y+18; }
        }
        e.health=e.maxHealth=Math.round(definition.stats().health()*spec.tuning().health());
        e.timer=.8f+random.nextFloat(); e.repairTimer=1; spawnedDrones++; enemiesEncountered++;
        if (definition.ability()==MissionConfig.EnemyAbility.AMBUSH_DRONE
            || mission.type==MissionConfig.MissionType.BLACK_TIDE)
            e.hiddenTime=EnvironmentSystems.hiddenSeconds(spec.difficulty());
    }

    private void spawnEnvironment(MissionConfig.EnvironmentKind kind,float x,float y) {
        if (kind==null) return;
        if (kind==MissionConfig.EnvironmentKind.OIL_FIELD || kind==MissionConfig.EnvironmentKind.TOXIC_FIELD) {
            spawnHazard(kind,x,y,false); return;
        }
        Entity e=environments.obtain(); if (e==null) return;
        e.environment=kind; e.x=x; e.y=y;
        switch (kind) {
            case CHEMICAL_BARREL -> { e.radius=24; e.health=e.maxHealth=32; e.vy=-31; }
            case RUIN -> { e.radius=43; e.health=e.maxHealth=75; e.vy=-24; }
            case COLLAPSIBLE -> { e.radius=48; e.health=e.maxHealth=55; e.vy=-25; }
            case CLEANUP_CAPSULE -> { e.radius=19; e.vy=-34; }
            case VALVE -> { e.radius=31; e.health=e.maxHealth=1; e.vy=-27; }
            default -> { e.radius=25; e.vy=-28; }
        }
    }

    private Entity spawnHazard(MissionConfig.EnvironmentKind kind,float x,float y,boolean bossOwned) {
        Entity e=hazards.obtain(); if (e==null) return null;
        e.environment=kind; e.x=Rules.clamp(x,55,WIDTH-55); e.y=y;
        e.radius=kind==MissionConfig.EnvironmentKind.OIL_FIELD?82:68;
        e.vy=bossOwned?0:-18; e.value=bossOwned?1:0;
        if (kind==MissionConfig.EnvironmentKind.OIL_FIELD) { oilSpawned++; if (bossOwned) bossOilTotal++; }
        return e;
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
                if (e.active) updateEnemyAbility(e,dt);
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
    private void updateEnemyAbility(Entity enemy,float dt) {
        switch (enemy.enemy.ability()) {
            case CORAL_CUTTER -> {
                Entity coral=nearestCoral(enemy.x,enemy.y);
                if (coral==null) return;
                enemy.aimX=coral.x; enemy.aimY=coral.y;
                float dx=coral.x-enemy.x,dy=coral.y-enemy.y,length=Math.max(1,(float)Math.sqrt(dx*dx+dy*dy));
                float step=Math.min(length,enemy.enemy.stats().speed()*.65f*dt);
                enemy.x+=dx/length*step; enemy.y+=dy/length*step;
                enemy.repairTimer-=dt;
                if (length<=enemy.radius+coral.radius+18 && enemy.repairTimer<=0) {
                    damageCoral(coral,Math.max(2,enemy.enemy.stats().damage())); enemy.repairTimer=1.1f; enemy.effectTime=.35f;
                }
            }
            case NET_RECYCLER -> {
                enemy.repairTimer-=dt;
                if (enemy.repairTimer>0) return;
                Entity net=nearestNet(enemy.x,enemy.y);
                if (net!=null && distanceSquared(net,enemy.x,enemy.y)<180*180) {
                    net.progress=Math.max(0,net.progress-.2f); enemy.aimX=net.x; enemy.aimY=net.y; enemy.effectTime=.4f;
                }
                enemy.repairTimer=1.8f;
            }
            case CHEMICAL_BOMBER -> {
                enemy.repairTimer-=dt;
                if (enemy.repairTimer<=0 && enemy.y<PLAY_MAX_Y) {
                    spawnHazard(MissionConfig.EnvironmentKind.TOXIC_FIELD,enemy.x,enemy.y,false);
                    enemy.repairTimer=5.5f/spec.tuning().fireRate();
                }
            }
            case OIL_SPREADER -> {
                enemy.repairTimer-=dt;
                if (enemy.repairTimer<=0 && enemy.y<PLAY_MAX_Y) {
                    spawnHazard(MissionConfig.EnvironmentKind.OIL_FIELD,enemy.x,enemy.y,false);
                    enemy.repairTimer=6f/spec.tuning().fireRate();
                }
            }
            case IGNITION_DRONE -> {
                enemy.repairTimer-=dt;
                if (enemy.repairTimer<=0) {
                    Entity oil=nearestHazard(MissionConfig.EnvironmentKind.OIL_FIELD,enemy.x,enemy.y);
                    if (oil!=null && distanceSquared(oil,enemy.x,enemy.y)<190*190) {
                        oil.effectTime=2.4f; enemy.aimX=oil.x; enemy.aimY=oil.y; enemy.effectTime=.45f;
                    }
                    enemy.repairTimer=2.5f;
                }
            }
            default -> { }
        }
    }

    private Entity nearestHazard(MissionConfig.EnvironmentKind kind,float x,float y) {
        Entity closest=null; float distance=Float.MAX_VALUE;
        for (int i=0;i<hazards.capacity();i++) {
            Entity field=hazards.at(i); float candidate=distanceSquared(field,x,y);
            if (field.active && field.environment==kind && candidate<distance) { closest=field; distance=candidate; }
        }
        return closest;
    }
    private Entity nearestCoral(float x,float y) {
        Entity closest=null; float distance=Float.MAX_VALUE;
        for (int i=0;i<corals.capacity();i++) {
            Entity coral=corals.at(i); float candidate=distanceSquared(coral,x,y);
            if (coral.active && candidate<distance) { closest=coral; distance=candidate; }
        }
        return closest;
    }
    private Entity nearestNet(float x,float y) {
        Entity closest=null; float distance=Float.MAX_VALUE;
        for (int i=0;i<plastics.capacity();i++) {
            Entity net=plastics.at(i); float candidate=distanceSquared(net,x,y);
            if (net.active && net.waste!=null && net.waste.kind()==MissionConfig.WasteKind.NET && candidate<distance) {
                closest=net; distance=candidate;
            }
        }
        return closest;
    }
    private void damageMostIntactCoral(int damage) {
        Entity target=null;
        for (int i=0;i<corals.capacity();i++) {
            Entity coral=corals.at(i);
            if (coral.active && (target==null || coral.health>target.health)) target=coral;
        }
        if (target!=null) { target.effectTime=mission.boss.telegraphSeconds(); damageCoral(target,damage); }
    }
    private void damageCoral(Entity coral,int damage) {
        if (coral==null || !coral.active || damage<=0) return;
        int before=coral.health; coral.health=Rules.damage(coral.health,damage); coralDamage+=before-coral.health;
        if (coral.health==0) coral.active=false;
    }
    private void updateBoss(float dt) {
        if (mission != null) {
            switch (mission.boss.kind()) {
                case SHORELINE_COMPACTOR -> updateCompactor(dt);
                case REEF_BREAKER -> updateReefBreaker(dt);
                case GHOST_NET_HARVESTER -> updateHarvester(dt);
                case URBAN_SALVAGER -> updateUrbanSalvager(dt);
                case OIL_KRAKEN -> updateOilKraken(dt);
            }
            return;
        }
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
    private void updateReefBreaker(float dt) {
        if (!bossSpawned && elapsed>=mission.boss.start()) {
            bossSpawned=true; timeline.stop(); reefBreaker.start(); boss.reset();
            boss.x=WIDTH/2f; boss.y=830; boss.radius=66; boss.health=boss.maxHealth=reefBreaker.maxHealth(); enemiesEncountered++;
        }
        if (!bossSpawned || recovering) return;
        reefBreaker.update(dt);
        boss.x=WIDTH/2f+(float)Math.sin(reefBreaker.stateTime()*.45f*spec.tuning().bossMovement())*42;
        boss.y=Math.max(748,830-reefBreaker.stateTime()*30);
        boss.health=reefBreaker.health();
        boolean generators=reefBreaker.state()==ReefBreaker.State.GENERATORS;
        configureBossPart(bossLeftPipe,145,boss.y-8,reefBreaker.generatorHealth(true),reefBreaker.maxGeneratorHealth(),generators);
        configureBossPart(bossRightPipe,395,boss.y-8,reefBreaker.generatorHealth(false),reefBreaker.maxGeneratorHealth(),generators);
        if (reefBreaker.cuttersActive() && (player.x<mission.boss.pressInset() || player.x>WIDTH-mission.boss.pressInset())) hitPlayer(CONTACT_DAMAGE);
        if (reefBreaker.consumeVolley()) fireBossVolley();
        if (reefBreaker.consumeDrone()) spawnEnemy(mission.enemy("CORAL_CUTTER"),boss.x,boss.y-45);
        if (reefBreaker.consumeCoralStrike()) damageMostIntactCoral(12);
        if (reefBreaker.defeated()) beginRecovery();
    }
    private void updateHarvester(float dt) {
        if (!bossSpawned && elapsed>=mission.boss.start()) {
            bossSpawned=true; timeline.stop(); harvester.start(); boss.reset();
            boss.x=WIDTH/2f; boss.y=830; boss.radius=68; boss.health=boss.maxHealth=harvester.maxHealth(); enemiesEncountered++;
        }
        if (!bossSpawned || recovering) return;
        harvester.update(dt);
        boss.x=WIDTH/2f+(float)Math.sin(harvester.stateTime()*.52f*spec.tuning().bossMovement())*55;
        boss.y=Math.max(746,830-harvester.stateTime()*30);
        boss.health=harvester.health();
        boolean generators=harvester.state()==GhostNetHarvester.State.GENERATORS;
        configureBossPart(bossLeftPipe,145,boss.y-8,harvester.generatorHealth(true),harvester.maxGeneratorHealth(),generators);
        configureBossPart(bossRightPipe,395,boss.y-8,harvester.generatorHealth(false),harvester.maxGeneratorHealth(),generators);
        if (harvester.wallsActive() && Math.abs(player.x-harvester.safeLaneX())>72) slowTimer=Math.max(slowTimer,.2f);
        if (harvester.consumeVolley()) fireBossVolley();
        if (harvester.consumeLargeNet()) { spawnBossNet(boss.x-75); spawnBossNet(boss.x+75); }
        if (harvester.consumeDrone()) spawnEnemy(mission.enemy("NET_LAUNCHER"),boss.x,boss.y-45);
        if (harvester.defeated()) beginRecovery();
    }
    private void updateUrbanSalvager(float dt) {
        if (!bossSpawned && elapsed>=mission.boss.start()) {
            bossSpawned=true; timeline.stop(); urbanSalvager.start(); boss.reset();
            boss.x=WIDTH/2f; boss.y=830; boss.radius=70;
            boss.health=boss.maxHealth=urbanSalvager.maxHealth(); enemiesEncountered++;
        }
        if (!bossSpawned || recovering) return;
        urbanSalvager.update(dt);
        boss.x=WIDTH/2f+(float)Math.sin(urbanSalvager.stateTime()*.48f*spec.tuning().bossMovement())*58;
        boss.y=Math.max(746,830-urbanSalvager.stateTime()*30); boss.health=urbanSalvager.health();
        boolean plates=urbanSalvager.state()==UrbanSalvager.State.ARMOR_PLATES;
        configureBossPart(bossLeftPipe,145,boss.y-6,urbanSalvager.plateHealth(true),urbanSalvager.maxPlateHealth(),plates);
        configureBossPart(bossRightPipe,395,boss.y-6,urbanSalvager.plateHealth(false),urbanSalvager.maxPlateHealth(),plates);
        if (urbanSalvager.consumeScrap()) fireScrapVolley();
        if (urbanSalvager.consumeTurret()) spawnEnemy(mission.enemy("RUIN_TURRET"),boss.x,boss.y-45);
        if (urbanSalvager.defeated()) beginRecovery();
    }
    private void updateOilKraken(float dt) {
        if (!bossSpawned && elapsed>=mission.boss.start()) {
            bossSpawned=true; timeline.stop(); oilKraken.start(); boss.reset();
            boss.x=WIDTH/2f; boss.y=830; boss.radius=74;
            boss.health=boss.maxHealth=oilKraken.maxHealth(); enemiesEncountered++;
            spawnHazard(MissionConfig.EnvironmentKind.OIL_FIELD,105,560,true);
            spawnHazard(MissionConfig.EnvironmentKind.OIL_FIELD,225,390,true);
            spawnHazard(MissionConfig.EnvironmentKind.OIL_FIELD,345,560,true);
            spawnHazard(MissionConfig.EnvironmentKind.OIL_FIELD,455,390,true);
        }
        if (!bossSpawned || recovering) return;
        oilKraken.update(dt,bossOilClearance());
        boss.x=WIDTH/2f+(float)Math.sin(oilKraken.stateTime()*.42f*spec.tuning().bossMovement())*46;
        boss.y=Math.max(744,830-oilKraken.stateTime()*29); boss.health=oilKraken.health();
        boolean valves=oilKraken.state()==OilKraken.State.VALVES;
        configureBossPart(bossLeftPipe,145,boss.y-9,oilKraken.valveHealth(true),oilKraken.maxValveHealth(),valves);
        configureBossPart(bossRightPipe,395,boss.y-9,oilKraken.valveHealth(false),oilKraken.maxValveHealth(),valves);
        if (valves) { serviceBossValve(bossLeftPipe,true,dt); serviceBossValve(bossRightPipe,false,dt); }
        if (oilKraken.consumeVolley()) fireBossVolley();
        if (oilKraken.consumeOilSpray() && bossOilTotal<8)
            spawnHazard(MissionConfig.EnvironmentKind.OIL_FIELD,80+random.nextFloat()*(WIDTH-160),430+random.nextFloat()*220,true);
        if (oilKraken.defeated()) beginRecovery();
    }
    private void serviceBossValve(Entity valve,boolean left,float dt) {
        if (!valve.active) return;
        if (near(valve,spec.loadout().cleanupRadius())) {
            valve.progress+=dt;
            if (valve.progress>=EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.VALVE,spec.difficulty())) {
                oilKraken.closeValve(left); valve.active=false; valvesClosed++; salvageCount+=5;
            }
        } else valve.progress=0;
    }
    private void fireScrapVolley() {
        int count=Math.max(3,spec.tuning().bossProjectiles());
        for (int i=0;i<count;i++) {
            float x=70+i*(WIDTH-140f)/Math.max(1,count-1);
            hostileProjectile(x,boss.y-35,(boss.x-x)*.16f,-spec.tuning().shotSpeed(),9,0);
        }
    }
    private float bossOilClearance() {
        if (bossOilTotal<=0) return 0;
        int active=0;
        for (int i=0;i<hazards.capacity();i++) {
            Entity e=hazards.at(i);
            if (e.active && e.value==1 && e.environment==MissionConfig.EnvironmentKind.OIL_FIELD) active++;
        }
        return Rules.clamp(1-(float)active/bossOilTotal,0,1);
    }
    private void configurePipe(Entity pipe,float x,float y,int health,boolean active) {
        pipe.active=active && health>0; pipe.x=x; pipe.y=y; pipe.radius=31; pipe.health=health; pipe.maxHealth=compactor.maxPipeHealth();
    }
    private void configureBossPart(Entity part,float x,float y,int health,int maxHealth,boolean active) {
        part.active=active&&health>0; part.x=x; part.y=y; part.radius=31; part.health=health; part.maxHealth=maxHealth;
    }
    private void fireBossVolley() {
        int count=Math.max(2+bossPhase(),spec.tuning().bossProjectiles());
        double aim=Math.atan2(player.y-boss.y,player.x-boss.x);
        for (int i=0;i<count;i++) {
            double angle=aim+(i-(count-1)/2f)*.24;
            hostileProjectile(boss.x,boss.y-48,(float)Math.cos(angle)*spec.tuning().shotSpeed(),
                (float)Math.sin(angle)*spec.tuning().shotSpeed(),8,0);
        }
        if (compactor!=null && compactor.phase()==1) spawnBossWaste();
    }
    private void spawnBossNet(float x) {
        Entity e=plastics.obtain(); if (e==null) return;
        MissionConfig.Waste waste=mission.waste(MissionConfig.WasteKind.NET);
        e.x=Rules.clamp(x,65,475); e.y=boss.y-38; e.radius=waste.radius(); e.vy=-waste.drift(); e.waste=waste;
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
        if (compactor!=null) return compactor.phase();
        if (reefBreaker!=null) return reefBreaker.phase();
        if (harvester!=null) return harvester.phase();
        if (urbanSalvager!=null) return urbanSalvager.phase();
        if (oilKraken!=null) return oilKraken.phase();
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
                    if (!enemyTargetable(e) || !Rules.overlaps(b.x, b.y, b.radius, e.x, e.y,e.radius)) continue;
                    b.active = false;
                    damageEnemy(e, b.damage, b.x, b.y-b.vy*dt);
                    break;
                }
                if (mission != null && b.active && hitEnvironment(b)) continue;
                if (mission != null && b.active && hitNet(b)) continue;
                if (mission != null && b.active && hitCoral(b)) continue;
                if (mission != null && b.active && hitMissionBoss(b)) continue;
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
    private boolean hitNet(Entity bullet) {
        if (mission.type!=MissionConfig.MissionType.GHOST_NETS) return false;
        for (int i=0;i<plastics.capacity();i++) {
            Entity net=plastics.at(i);
            if (!net.active || net.waste==null || net.waste.kind()!=MissionConfig.WasteKind.NET
                || !Rules.overlaps(bullet.x,bullet.y,bullet.radius,net.x,net.y,net.radius)) continue;
            bullet.active=false; net.progress=NetCuttingSystem.standardShot(net.progress);
            if (NetCuttingSystem.opened(net.progress)) collectWaste(net);
            return true;
        }
        return false;
    }
    private boolean hitEnvironment(Entity bullet) {
        for (int i=0;i<environments.capacity();i++) {
            Entity e=environments.at(i);
            if (!e.active || e.environment==MissionConfig.EnvironmentKind.VALVE
                || e.environment==MissionConfig.EnvironmentKind.CLEANUP_CAPSULE
                || !Rules.overlaps(bullet.x,bullet.y,bullet.radius,e.x,e.y,e.radius)) continue;
            bullet.active=false; damageEnvironment(e,bullet.damage); return true;
        }
        return false;
    }
    private void damageEnvironment(Entity e,int damage) {
        if (!e.active || damage<=0) return;
        if (e.environment==MissionConfig.EnvironmentKind.CHEMICAL_BARREL && !e.warned) {
            e.warned=true; spawnHazard(MissionConfig.EnvironmentKind.TOXIC_FIELD,e.x,e.y,false);
        }
        e.health=Rules.damage(e.health,damage);
        if (e.health>0) return;
        e.active=false; cleanedCount++; salvageCount+=e.environment==MissionConfig.EnvironmentKind.RUIN?5:3;
        burst(e.x,e.y,0);
    }
    private boolean hitCoral(Entity bullet) {
        for (int i=0;i<corals.capacity();i++) {
            Entity coral=corals.at(i);
            if (!coral.active || !Rules.overlaps(bullet.x,bullet.y,bullet.radius,coral.x,coral.y,coral.radius)) continue;
            bullet.active=false;
            damageCoral(coral,bullet.damage);
            return true;
        }
        return false;
    }
    private boolean hitMissionBoss(Entity bullet) {
        if (!bossSpawned || !boss.active) return false;
        if (bossLeftPipe.active && Rules.overlaps(bullet.x,bullet.y,bullet.radius,bossLeftPipe.x,bossLeftPipe.y,bossLeftPipe.radius)) {
            bullet.active=false; hitBossPart(true,bullet.damage); return true;
        }
        if (bossRightPipe.active && Rules.overlaps(bullet.x,bullet.y,bullet.radius,bossRightPipe.x,bossRightPipe.y,bossRightPipe.radius)) {
            bullet.active=false; hitBossPart(false,bullet.damage); return true;
        }
        if (Rules.overlaps(bullet.x,bullet.y,bullet.radius,boss.x,boss.y,boss.radius)) {
            bullet.active=false; hitBossCore(bullet.damage); return true;
        }
        return false;
    }
    private void hitBossPart(boolean left,int damage) {
        if (compactor!=null) { compactor.hitPipe(left,damage); (left?bossLeftPipe:bossRightPipe).health=compactor.pipeHealth(left); }
        else if (reefBreaker!=null) { reefBreaker.hitGenerator(left,damage); (left?bossLeftPipe:bossRightPipe).health=reefBreaker.generatorHealth(left); }
        else if (harvester!=null) { harvester.hitGenerator(left,damage); (left?bossLeftPipe:bossRightPipe).health=harvester.generatorHealth(left); }
        else if (urbanSalvager!=null) { urbanSalvager.hitPlate(left,damage); (left?bossLeftPipe:bossRightPipe).health=urbanSalvager.plateHealth(left); }
    }
    private void hitBossCore(int damage) {
        if (compactor!=null) { compactor.hitCore(damage); boss.health=compactor.health(); }
        else if (reefBreaker!=null) { reefBreaker.hitCore(damage); boss.health=reefBreaker.health(); }
        else if (harvester!=null) { harvester.hitCore(damage); boss.health=harvester.health(); }
        else if (urbanSalvager!=null) { urbanSalvager.hitCore(damage); boss.health=urbanSalvager.health(); }
        else if (oilKraken!=null) { oilKraken.hitCore(damage); boss.health=oilKraken.health(); }
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
        for (int i=0;i<hazards.capacity();i++) if (hazards.at(i).active && near(hazards.at(i),spec.loadout().cleanupRadius())) return true;
        for (int i=0;i<environments.capacity();i++) {
            Entity e=environments.at(i);
            if (e.active && (e.environment==MissionConfig.EnvironmentKind.VALVE
                || e.environment==MissionConfig.EnvironmentKind.CLEANUP_CAPSULE)
                && near(e,spec.loadout().cleanupRadius())) return true;
        }
        if (oilKraken!=null && oilKraken.state()==OilKraken.State.VALVES
            && (near(bossLeftPipe,spec.loadout().cleanupRadius()) || near(bossRightPipe,spec.loadout().cleanupRadius()))) return true;
        return false;
    }
    private void updatePlastic(float dt) {
        for (int i = 0; i < plastics.capacity(); i++) {
            Entity e = plastics.at(i);
            if (!e.active) continue;
            e.age+=dt; e.y += e.vy * dt;
            if (mission!=null && mission.currentStrength>0) {
                float current=(float)Math.sin(elapsed*.7f+e.age*.35f+i*.83f)*mission.currentStrength;
                if (e.waste!=null && e.waste.kind()==MissionConfig.WasteKind.NET) current*=1.35f;
                e.x=Rules.clamp(e.x+current*dt,45,WIDTH-45);
            }
            boolean net=e.waste!=null && e.waste.kind()==MissionConfig.WasteKind.NET;
            if (net && Rules.overlaps(e.x,e.y,e.radius,player.x,player.y,player.radius)) slowTimer=Math.max(slowTimer,mission.netSeconds);
            if (near(e, spec.loadout().cleanupRadius())) {
                float multiplier=e.waste==null?1:e.waste.cleanMultiplier();
                if (net && mission.type==MissionConfig.MissionType.GHOST_NETS) {
                    float power=CLEAN_SECONDS/spec.loadout().cleanupSeconds()/multiplier;
                    e.progress=NetCuttingSystem.cutter(e.progress,dt,power);
                    if (NetCuttingSystem.opened(e.progress)) collectWaste(e);
                } else {
                    e.progress += dt;
                    if (e.progress >= spec.loadout().cleanupSeconds()*multiplier) collectWaste(e);
                }
            } else if (!(net && mission.type==MissionConfig.MissionType.GHOST_NETS)) e.progress = 0;
            if (e.y < -DESPAWN_MARGIN) e.active = false;
        }
    }
    private void collectWaste(Entity e) {
        if (!e.active) return;
        e.active=false; cleanedCount++;
        if (e.waste==null || e.waste.plastic()) plasticCount++;
        if (e.waste!=null) salvageCount+=e.waste.salvage();
        burst(e.x,e.y,1); events.emit(PLASTIC_COLLECTED,e.x,e.y,PLASTIC_SCORE);
    }
    private void updateCorals(float dt) {
        for (int i=0;i<corals.capacity();i++) {
            Entity e=corals.at(i);
            if (!e.active) continue;
            e.y+=e.vy*dt; e.effectTime=Math.max(0,e.effectTime-dt);
            e.progress=Math.max(e.progress,Rules.percentage(cleanedCount,Math.max(1,mission.wasteCount))/100f);
            if (e.y < -e.radius) e.active=false;
        }
    }
    private void updateEnvironments(float dt) {
        for (int i=0;i<environments.capacity();i++) {
            Entity e=environments.at(i); if (!e.active) continue;
            e.age+=dt; e.y+=e.vy*dt; e.effectTime=Math.max(0,e.effectTime-dt);
            if (e.environment==MissionConfig.EnvironmentKind.COLLAPSIBLE && e.y<720) {
                if (!e.warned) { e.warned=true; e.timer=EnvironmentSystems.collapseWarning(spec.difficulty()); }
                if (!e.friendly) {
                    e.timer-=dt;
                    if (e.timer<=0) { e.friendly=true; e.radius=72; e.vy=-72; }
                }
            }
            if (e.environment==MissionConfig.EnvironmentKind.CLEANUP_CAPSULE
                && near(e,PICKUP_RADIUS+e.radius)) {
                e.active=false; cleanedCount++; salvageCount+=2; absorbNearestOil(); burst(e.x,e.y,1); continue;
            }
            if (e.environment==MissionConfig.EnvironmentKind.VALVE) {
                if (near(e,spec.loadout().cleanupRadius())) {
                    e.progress+=dt;
                    if (e.progress>=EnvironmentSystems.interactionSeconds(e.environment,spec.difficulty())) {
                        e.active=false; valvesClosed++; cleanedCount++; salvageCount+=4; burst(e.x,e.y,1); continue;
                    }
                } else e.progress=0;
            } else if (e.environment!=MissionConfig.EnvironmentKind.CLEANUP_CAPSULE
                && Rules.overlaps(e.x,e.y,e.radius,player.x,player.y,player.radius)) hitPlayer(CONTACT_DAMAGE);
            if (e.y<-e.radius) e.active=false;
        }
    }
    private void updateHazards(float dt) {
        if (mission.type==MissionConfig.MissionType.SUNKEN_CITY && midpointActive()) visibilityExposure=Math.max(visibilityExposure,.5f);
        for (int i=0;i<hazards.capacity();i++) {
            Entity e=hazards.at(i); if (!e.active) continue;
            e.age+=dt; e.effectTime=Math.max(0,e.effectTime-dt); e.y+=e.vy*dt;
            boolean inside=Rules.overlaps(e.x,e.y,e.radius,player.x,player.y,player.radius);
            if (inside) {
                visibilityExposure=Math.max(visibilityExposure,e.environment==MissionConfig.EnvironmentKind.OIL_FIELD?.9f:.68f);
                if ((e.environment==MissionConfig.EnvironmentKind.TOXIC_FIELD || e.effectTime>0) && hazardDamageTimer<=0) {
                    hitPlayer(e.effectTime>0?10:6);
                    hazardDamageTimer=EnvironmentSystems.hazardDamageInterval(spec.difficulty());
                }
            }
            if (near(e,spec.loadout().cleanupRadius())) {
                e.progress+=dt;
                if (e.progress>=EnvironmentSystems.interactionSeconds(e.environment,spec.difficulty())) clearHazard(e);
            } else e.progress=Math.max(0,e.progress-dt*.25f);
            if (e.value==0 && e.y<-e.radius) e.active=false;
        }
    }
    private void clearHazard(Entity e) {
        if (!e.active) return;
        e.active=false; cleanedCount++; salvageCount+=e.environment==MissionConfig.EnvironmentKind.OIL_FIELD?4:2;
        if (e.environment==MissionConfig.EnvironmentKind.OIL_FIELD) oilCleaned++;
        burst(e.x,e.y,1);
    }
    private void absorbNearestOil() {
        Entity oil=nearestHazard(MissionConfig.EnvironmentKind.OIL_FIELD,player.x,player.y);
        if (oil!=null) clearHazard(oil);
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
            e.age+=dt; e.y += e.vy * dt;
            if (mission!=null && mission.currentStrength>0)
                e.x=Rules.clamp(e.x+(float)Math.sin(elapsed*.65f+i)*mission.currentStrength*.55f*dt,40,WIDTH-40);
            if (near(e, RESCUE_RADIUS)) {
                e.progress += dt;
                float multiplier=e.creature==null?1:e.creature.rescueMultiplier();
                if (e.progress >= spec.loadout().rescueSeconds()*multiplier) {
                    e.friendly = true; rescueCount++; burst(e.x, e.y, 1);
                    events.emit(TURTLE_RESCUED, e.x, e.y, RESCUE_SCORE);
                }
            } else e.progress = 0; // A continuous short stay is required.
            if (!e.friendly && e.lifetime>0 && e.age>=e.lifetime) e.active=false;
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
        Entity closest = bossLeftPipe.active ? bossLeftPipe : bossRightPipe.active ? bossRightPipe
            : boss.active && (mission==null || bossCoreVulnerable()) ? boss : null;
        float distance = closest == null ? Float.MAX_VALUE : distanceSquared(closest, x, y);
        for (int i = 0; i < drones.capacity(); i++) {
            Entity e = drones.at(i);
            if (enemyTargetable(e) && distanceSquared(e, x, y) < distance) { closest = e; distance = distanceSquared(e, x, y); }
        }
        return closest;
    }
    private float distanceSquared(Entity e, float x, float y) { return (e.x-x)*(e.x-x) + (e.y-y)*(e.y-y); }
    public void fireLaser(int damage) {
        Entity target = null;
        float y = SPAWN_Y;
        if (mission!=null && mission.type==MissionConfig.MissionType.GHOST_NETS) {
            for (int i=0;i<plastics.capacity();i++) {
                Entity net=plastics.at(i);
                if (net.active && net.waste!=null && net.waste.kind()==MissionConfig.WasteKind.NET
                    && net.y>player.y && net.y<y && Math.abs(net.x-player.x)<=net.radius+3) { target=net; y=net.y; }
            }
        }
        if (mission!=null) {
            for (int i=0;i<environments.capacity();i++) {
                Entity e=environments.at(i);
                if (e.active && e.environment!=MissionConfig.EnvironmentKind.VALVE
                    && e.environment!=MissionConfig.EnvironmentKind.CLEANUP_CAPSULE
                    && e.y>player.y && e.y<y && Math.abs(e.x-player.x)<=e.radius+3) { target=e; y=e.y; }
            }
        }
        for (int i = 0; i <= drones.capacity() + 2; i++) {
            Entity e = i < drones.capacity() ? drones.at(i) : i == drones.capacity() ? boss
                : i == drones.capacity() + 1 ? bossLeftPipe : bossRightPipe;
            if (e.active && (e.enemy==null || enemyTargetable(e)) && e.y > player.y && e.y < y
                && Math.abs(e.x-player.x) <= e.radius + 3) { target = e; y = e.y; }
        }
        laser.x = player.x; laser.y = player.y + SHOT_OFFSET_Y; laser.vy = y; laser.timer = .09f;
        if (target != null) {
            if (target.environment!=null) damageEnvironment(target,damage);
            else if (target.waste!=null && target.waste.kind()==MissionConfig.WasteKind.NET) {
                target.progress=NetCuttingSystem.standardShot(target.progress);
                if (NetCuttingSystem.opened(target.progress)) collectWaste(target);
            } else if (mission!=null && (target==boss || target==bossLeftPipe || target==bossRightPipe)) {
                if (target==bossLeftPipe) hitBossPart(true,damage);
                else if (target==bossRightPipe) hitBossPart(false,damage);
                else hitBossCore(damage);
            } else damageEnemy(target,damage,player.x,player.y);
        }
    }
    private void damageEnemy(Entity e, int damage) {
        damageEnemy(e,damage,e.x,e.y-1);
    }
    private void damageEnemy(Entity e,int damage,float impactX,float sourceY) {
        if (!e.active) return;
        if (mission!=null && e.enemy!=null && EnemySystems.armoredHit(e,impactX,sourceY)) { e.effectTime=.18f; return; }
        if (mission!=null && e.enemy!=null && e.shieldTime>0
            && e.enemy.ability()!=MissionConfig.EnemyAbility.SHIELD_CARRIER) damage=Math.max(1,damage/3);
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
    public ReefBreaker reefBreaker() { return reefBreaker; }
    public GhostNetHarvester harvester() { return harvester; }
    public UrbanSalvager urbanSalvager() { return urbanSalvager; }
    public OilKraken oilKraken() { return oilKraken; }
    public int spawnedDrones() { return spawnedDrones; }
    public float progress() { return elapsed / (mission==null?LEVEL_SECONDS:mission.durationSeconds); }
    public float restoration() {
        if (mission==null) return (Rules.cleanup(plasticCount)*CLEANUP_RESTORE_WEIGHT+Rules.rescue(rescueCount)*RESCUE_RESTORE_WEIGHT)/100f;
        float cleanup=Rules.percentage(cleanedCount,mission.cleanupCount())/100f;
        float rescue=Rules.percentage(rescueCount,mission.turtleCount)/100f;
        float coral=1-Rules.percentage(coralDamage,Math.max(1,mission.coralCount*30))/100f;
        float base=Rules.clamp(cleanup*.55f+rescue*.3f+coral*.15f,0,1);
        return recovering ? base+(1-base)*Rules.clamp(recoveryTimer/mission.recoverySeconds,0,1) : base;
    }
    public int kills() { return kills; }
    public int plasticCount() { return plasticCount; }
    public int rescueCount() { return rescueCount; }
    public int salvageCount() { return salvageCount; }
    public int score() { return Rules.score(0,cleanedCount,rescueCount,salvageCount,player.health,false)+combatScore; }
    public int cleanedCount() { return mission==null?plasticCount:cleanedCount; }
    public int wasteTotal() { return mission==null?PLASTIC_COUNT:mission.cleanupCount(); }
    public int turtleTotal() { return mission==null?TURTLE_COUNT:mission.turtleCount; }
    public int valvesClosed() { return valvesClosed; }
    public int oilCleaned() { return oilCleaned; }
    public EnvironmentSystems.Route route() {
        MissionConfig.MissionType type=mission==null?MissionConfig.MissionType.BLUE_COAST:mission.type;
        return EnvironmentSystems.route(type,elapsed,spec.difficulty());
    }
    public float visibilityRadius() {
        MissionConfig.MissionType type=mission==null?MissionConfig.MissionType.BLUE_COAST:mission.type;
        return EnvironmentSystems.visibilityRadius(type,spec.difficulty(),visibilityExposure);
    }
    public boolean enemyVisible(Entity enemy) { return enemy!=null && enemy.hiddenTime<=0; }
    private boolean enemyTargetable(Entity enemy) { return enemy!=null && enemy.active && enemy.hiddenTime<=0; }
    public int coralDamage() { return coralDamage; }
    public boolean midpointActive() { return mission!=null && elapsed>=mission.midpointStart && elapsed<mission.midpointStart+mission.midpointDuration; }
    public boolean bossCoreVulnerable() {
        if (mission==null) return true;
        if (compactor!=null) return compactor.coreVulnerable();
        if (reefBreaker!=null) return reefBreaker.coreVulnerable();
        if (harvester!=null) return harvester.coreVulnerable();
        if (urbanSalvager!=null) return urbanSalvager.coreVulnerable();
        return oilKraken!=null && oilKraken.coreVulnerable();
    }
    public boolean cleaning() { return cleaning; }
    public boolean slowed() { return slowTimer>0; }
    public boolean recovering() { return recovering; }
    public float recoveryProgress() { return mission==null?0:Rules.clamp(recoveryTimer/mission.recoverySeconds,0,1); }
    public int hostileBullets() { return hostileBulletCount(); }
    public boolean invulnerable() { return invulnerability > 0; }
    public boolean finished() { return finished; }
    public LevelResult result() { return result; }
}
