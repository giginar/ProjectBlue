package com.projectblue.game.logic.weapons;

import com.projectblue.game.config.ContentCatalog.*;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.logic.*;
import java.util.Map;
import java.util.EnumMap;
import static com.projectblue.game.config.GameConfig.*;

/** Per-run timers plus reusable stateless firing strategies. */
public final class WeaponController {
    private static final Map<Behavior,WeaponBehavior> BEHAVIORS = new EnumMap<>(Behavior.class);
    static {
        BEHAVIORS.put(Behavior.PULSE,new Pulse()); BEHAVIORS.put(Behavior.SPREAD,new Spread());
        BEHAVIORS.put(Behavior.LASER,new Laser()); BEHAVIORS.put(Behavior.HOMING,new Homing()); BEHAVIORS.put(Behavior.DRONE,new Drone());
    }
    private final Loadout loadout;
    private final WeaponBehavior primary;
    private final WeaponDef drone;
    private float primaryTimer, droneTimer;
    public WeaponController(Loadout loadout) {
        this.loadout = loadout; primary = BEHAVIORS.get(loadout.weapon().behavior());
        drone = loadout.supportWeapon();
    }
    public void update(GameWorld world,float dt) {
        primaryTimer -= dt;
        if (primaryTimer <= 0) {
            WeaponDef definition = loadout.weapon();
            primary.fire(world,definition,Math.max(1,Math.round(loadout.damage() * definition.damageMultiplier())));
            primaryTimer += loadout.shotInterval() / definition.rateMultiplier();
            world.events.emit(com.projectblue.game.events.GameEvents.Type.SHOT,world.player.x,world.player.y,0);
        }
        if (loadout.droneDamage() > 0) {
            droneTimer -= dt;
            if (droneTimer <= 0) {
                BEHAVIORS.get(drone.behavior()).fire(world,drone,loadout.droneDamage());
                droneTimer += loadout.shotInterval() / drone.rateMultiplier();
            }
        }
    }
    private static final class Pulse implements WeaponBehavior {
        public void fire(GameWorld w,WeaponDef d,int damage) {
            w.playerProjectile(w.player.x,w.player.y + SHOT_OFFSET_Y,0,d.projectileSpeed(),damage,0);
        }
    }
    private static final class Spread implements WeaponBehavior {
        public void fire(GameWorld w,WeaponDef d,int damage) {
            for (int i = 0; i < d.projectiles(); i++) {
                double angle = Math.PI / 2 + (i - (d.projectiles()-1) / 2f) * d.spreadRadians();
                w.playerProjectile(w.player.x,w.player.y + SHOT_OFFSET_Y,(float)Math.cos(angle)*d.projectileSpeed(),
                    (float)Math.sin(angle)*d.projectileSpeed(),damage,0);
            }
        }
    }
    private static final class Laser implements WeaponBehavior {
        public void fire(GameWorld w,WeaponDef d,int damage) { w.fireLaser(damage); }
    }
    private static final class Homing implements WeaponBehavior {
        public void fire(GameWorld w,WeaponDef d,int damage) {
            w.playerProjectile(w.player.x,w.player.y + SHOT_OFFSET_Y,0,d.projectileSpeed(),damage,d.tracking());
        }
    }
    private static final class Drone implements WeaponBehavior {
        public void fire(GameWorld w,WeaponDef d,int damage) {
            float x = w.supportX(), y = w.supportY();
            Entity target = w.nearestEnemy(x,y);
            float dx = target == null ? 0 : target.x - x, dy = target == null ? 1 : target.y - y;
            float distance = Math.max(.001f,(float)Math.sqrt(dx*dx + dy*dy));
            w.playerProjectile(x,y,dx/distance*d.projectileSpeed(),dy/distance*d.projectileSpeed(),damage,0);
        }
    }
}
