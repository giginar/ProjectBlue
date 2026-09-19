package com.projectblue.game.config;
import com.projectblue.game.save.Profile;
import com.projectblue.game.config.ContentCatalog.*;
import static com.projectblue.game.config.GameConfig.*;

/** Immutable run snapshot. Composition occurs before a dive, never during a frame. */
public record Loadout(int health, int damage, float speed, float cleanupRadius, float rescueSeconds,
    float salvageRadius, float shotInterval, float cleanupSeconds, int shieldCapacity,
    int droneDamage, WeaponDef weapon, WeaponDef supportWeapon) {
    public enum Pilot { KAIA, ATLAS, NERI, ROOK;
        public PilotDef definition() { return ContentCatalog.DEFAULT.pilot(name()); }
    }
    public enum Submarine { TIDE, MANTA, LEVIATHAN;
        public SubmarineDef definition() { return ContentCatalog.DEFAULT.submarine(name()); }
    }
    public enum Upgrade { PRIMARY_WEAPON, HULL, CLEANUP_BEAM, SHIELD, RESCUE_SYSTEM, SUPPORT_DRONE;
        public static final int MAX_LEVEL = 5;
        public UpgradeDef definition() { return ContentCatalog.DEFAULT.upgrade(name()); }
        public int cost(int level) { return definition().cost(level); }
    }
    public enum Weapon { PULSE_CANNON, SPREAD_CANNON, FOCUS_LASER, HOMING_MICRO_TORPEDO, SUPPORT_DRONE;
        public WeaponDef definition() { return ContentCatalog.DEFAULT.weapon(name()); }
    }
    /** Original simulation fixture, independent of pilot passives. */
    public static Loadout standard() {
        return new Loadout(PLAYER_HEALTH,PLAYER_DAMAGE,PLAYER_SPEED,CLEAN_RADIUS,RESCUE_SECONDS,SALVAGE_RADIUS,
            SHOT_INTERVAL,CLEAN_SECONDS,0,0,Weapon.PULSE_CANNON.definition(),Weapon.SUPPORT_DRONE.definition());
    }
    public static Loadout from(Profile p) {
        ContentCatalog c = p.content();
        SubmarineDef sub = c.submarine(p.selectedSubmarine.name());
        float[] bonus = new float[Stat.values().length];
        for (Upgrade u : Upgrade.values()) {
            UpgradeDef def = c.upgrade(u.name()); bonus[def.stat().ordinal()] += def.effect(p.upgradeLevel(u));
        }
        float hull = sub.baseHealth() + bonus[Stat.HEALTH.ordinal()];
        float damage = sub.primaryDamage() + bonus[Stat.DAMAGE.ordinal()];
        float cleanup = sub.cleanupPower() + bonus[Stat.CLEANUP_POWER.ordinal()];
        float rescue = sub.rescueSpeed() + bonus[Stat.RESCUE_SPEED.ordinal()];
        for (Passive passive : c.pilot(p.selectedPilot.name()).passives()) {
            switch (passive.stat()) {
                case HEALTH -> hull *= 1 + passive.amount();
                case DAMAGE -> damage *= 1 + passive.amount();
                case CLEANUP_POWER -> cleanup *= 1 + passive.amount();
                case RESCUE_SPEED -> rescue *= 1 + passive.amount();
                default -> throw new IllegalStateException("Unsupported passive");
            }
        }
        return new Loadout(Math.round(hull),Math.round(damage),sub.movementSpeed(),CLEAN_RADIUS + bonus[Stat.CLEANUP_RADIUS.ordinal()],
            RESCUE_SECONDS / rescue,SALVAGE_RADIUS + p.legacyMagnetLevel * 12,1 / sub.fireRate(),CLEAN_SECONDS / cleanup,
            sub.shieldCapacity() + Math.round(bonus[Stat.SHIELD.ordinal()]),Math.round(bonus[Stat.DRONE_DAMAGE.ordinal()]),c.weapon(p.selectedWeapon.name()),c.weapon(Weapon.SUPPORT_DRONE.name()));
    }
}
