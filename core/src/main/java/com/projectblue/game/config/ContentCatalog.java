package com.projectblue.game.config;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/** Validated immutable content, loaded once. No JSON work occurs in the simulation loop. */
public final class ContentCatalog {
    public enum UnlockMetric { ALWAYS, LEVEL_UNLOCKED, PLASTIC, ENEMIES, COMPLETED_RUNS }
    public enum Stat { HEALTH, DAMAGE, CLEANUP_POWER, CLEANUP_RADIUS, RESCUE_SPEED, SHIELD, DRONE_DAMAGE }
    public enum Behavior { PULSE, SPREAD, LASER, HOMING, DRONE }
    public enum AchievementMetric { LEVEL_COMPLETED, CLEANUP, RESCUE, UNTOUCHED, PLASTIC, ENEMIES, LEVEL_UNLOCKED, ABYSS, MAX_UPGRADE }
    public record Unlock(UnlockMetric metric, int target, String description) {}
    public record Passive(Stat stat, float amount) {}
    public record SubmarineDef(String id, String displayName, String description, int baseHealth,
        float movementSpeed, int primaryDamage, float fireRate, float cleanupPower, float rescueSpeed,
        int shieldCapacity, String specialAbility, Unlock unlockCondition) {}
    public record PilotDef(String id, String displayName, String description, List<Passive> passives, Unlock unlockCondition) {}
    public record UpgradeLevel(int level, int cost, float effect) {}
    public record UpgradeDef(String id, String displayName, String description, Stat stat, List<UpgradeLevel> levels) {
        public int maxLevel() { return levels.size(); }
        public int cost(int current) { return current >= maxLevel() ? 0 : levels.get(current).cost(); }
        public float effect(int level) { return level == 0 ? 0 : levels.get(level - 1).effect(); }
    }
    public record WeaponDef(String id, String displayName, String description, Behavior behavior,
        float damageMultiplier, float rateMultiplier, float projectileSpeed, int projectiles,
        float spreadRadians, float tracking, Unlock unlockCondition) {}
    public record AchievementDef(String id, String displayName, String description, boolean incremental,
        AchievementMetric metric, int target, int threshold) {}
    public static final ContentCatalog DEFAULT = load(ContentCatalog.class.getResourceAsStream("/config/content.json"), System.err::println);
    private final Map<String, SubmarineDef> submarines;
    private final Map<String, PilotDef> pilots;
    private final Map<String, UpgradeDef> upgrades;
    private final Map<String, WeaponDef> weapons;
    private final Map<String, AchievementDef> achievements;
    public final boolean fallback;

    private ContentCatalog(JsonValue root, boolean fallback) {
        this.fallback = fallback;
        uniqueKeys(root);
        integer(root,"schemaVersion",1,1);
        submarines = entries(root,"submarines", n -> new SubmarineDef(string(n,"id"),string(n,"displayName"),
            string(n,"description"),integer(n,"baseHealth",1,1000),number(n,"movementSpeed",100,2200),
            integer(n,"primaryDamage",1,100),number(n,"fireRate",.2f,20),number(n,"cleanupPower",.1f,5),
            number(n,"rescueSpeed",.1f,5),integer(n,"shieldCapacity",0,500),string(n,"specialAbility"),unlock(n)));
        pilots = entries(root,"pilots", n -> {
            List<Passive> passives = new ArrayList<>();
            for (JsonValue p : array(n,"passives")) {
                Stat stat = Stat.valueOf(string(p,"stat"));
                check(Arrays.asList(Stat.HEALTH,Stat.DAMAGE,Stat.CLEANUP_POWER,Stat.RESCUE_SPEED).contains(stat), "Unsupported pilot passive: " + stat);
                check(passives.stream().noneMatch(old -> old.stat() == stat), "Duplicate passive: " + stat);
                passives.add(new Passive(stat,number(p,"amount",.01f,.25f)));
            }
            check(passives.size() >= 1 && passives.size() <= 2,"Pilots require one or two passives");
            return new PilotDef(string(n,"id"),string(n,"displayName"),string(n,"description"),Collections.unmodifiableList(passives),unlock(n));
        });
        upgrades = entries(root,"upgrades", n -> {
            List<UpgradeLevel> levels = new ArrayList<>();
            int cost = -1; float effect = 0;
            for (JsonValue l : array(n,"levels")) {
                int next = integer(l,"level",1,5), price = integer(l,"cost",0,1000000);
                float value = number(l,"effect",.01f,500);
                check(next == levels.size() + 1,"Upgrade levels must be consecutive from 1");
                check(price > cost && value > effect,"Upgrade costs and cumulative effects must increase");
                levels.add(new UpgradeLevel(next,price,value)); cost = price; effect = value;
            }
            check(!levels.isEmpty() && levels.size() <= 5,"Upgrade must have 1-5 levels");
            return new UpgradeDef(string(n,"id"),string(n,"displayName"),string(n,"description"),Stat.valueOf(string(n,"stat")),Collections.unmodifiableList(levels));
        });
        weapons = entries(root,"weapons", n -> new WeaponDef(string(n,"id"),string(n,"displayName"),string(n,"description"),
            Behavior.valueOf(string(n,"behavior")),number(n,"damageMultiplier",.1f,5),number(n,"rateMultiplier",.1f,3),
            number(n,"projectileSpeed",100,1200),integer(n,"projectiles",1,7),number(n,"spreadRadians",0,1),number(n,"tracking",0,15),unlock(n)));
        Map<String,AchievementDef> parsedAchievements = entries(root,"achievements", n -> {
            JsonValue incremental = required(n,"incremental"); check(incremental.isBoolean(),"incremental must be boolean");
            AchievementMetric metric = AchievementMetric.valueOf(string(n,"metric"));
            int target = integer(n,"target",1,1000000), threshold = integer(n,"threshold",0,100);
            check(incremental.asBoolean() || target == 1,"One-time achievement target must be 1");
            check(incremental.asBoolean() == (metric == AchievementMetric.PLASTIC || metric == AchievementMetric.ENEMIES),"Invalid achievement progress mode");
            if (metric == AchievementMetric.LEVEL_COMPLETED || metric == AchievementMetric.LEVEL_UNLOCKED)
                check(threshold >= 1 && threshold <= CampaignConfig.LEVEL_COUNT,"Invalid achievement sector");
            if (metric == AchievementMetric.MAX_UPGRADE) check(threshold >= 1 && threshold <= 5,"Invalid maximum upgrade level");
            return new AchievementDef(string(n,"id"),string(n,"displayName"),string(n,"description"),incremental.asBoolean(),metric,target,threshold);
        });
        if (fallback) {
            Map<String,AchievementDef> complete=new LinkedHashMap<>(parsedAchievements);
            complete.put("SUNKEN_CITY_RESTORED",new AchievementDef("SUNKEN_CITY_RESTORED","City of Light",
                "Complete Sunken City.",false,AchievementMetric.LEVEL_COMPLETED,1,4));
            complete.put("BLACK_TIDE_CLEARED",new AchievementDef("BLACK_TIDE_CLEARED","Break the Black Tide",
                "Complete Black Tide.",false,AchievementMetric.LEVEL_COMPLETED,1,5));
            achievements=Collections.unmodifiableMap(complete);
        } else achievements=parsedAchievements;
        requireIds(submarines,Loadout.Submarine.values()); requireIds(pilots,Loadout.Pilot.values());
        requireIds(upgrades,Loadout.Upgrade.values()); requireIds(weapons,Loadout.Weapon.values());
        requireIds(achievements,com.projectblue.game.save.Achievement.values());
        check(submarine("TIDE").unlockCondition().metric() == UnlockMetric.ALWAYS,"TIDE must start unlocked");
        check(pilot("KAIA").unlockCondition().metric() == UnlockMetric.ALWAYS,"KAIA must start unlocked");
        check(weapon("PULSE_CANNON").unlockCondition().metric() == UnlockMetric.ALWAYS,"Pulse Cannon must start unlocked");
    }
    public SubmarineDef submarine(String id) { return submarines.get(id); }
    public PilotDef pilot(String id) { return pilots.get(id); }
    public UpgradeDef upgrade(String id) { return upgrades.get(id); }
    public WeaponDef weapon(String id) { return weapons.get(id); }
    public AchievementDef achievement(String id) { return achievements.get(id); }
    public static ContentCatalog parse(String json) { return new ContentCatalog(new JsonReader().parse(json),false); }
    public static ContentCatalog load(InputStream input, Consumer<String> log) {
        try (InputStream source = input) {
            if (source == null) throw new IOException("Missing resource");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (bytes.size() + count > 131072) throw new IOException("Config exceeds 128 KiB");
                bytes.write(buffer,0,count);
            }
            return parse(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            log.accept("Project Blue: config/content.json rejected: " + e.getMessage() + "; using built-in safe content.");
            return new ContentCatalog(new JsonReader().parse(SafeContent.JSON),true);
        }
    }
    private static Unlock unlock(JsonValue n) {
        JsonValue u = required(n,"unlockCondition");
        UnlockMetric metric = UnlockMetric.valueOf(string(u,"metric"));
        int target = integer(u,"target",0,1000000);
        check(metric == UnlockMetric.ALWAYS ? target == 0 : target > 0,"Invalid unlock target");
        if (metric == UnlockMetric.LEVEL_UNLOCKED) check(target <= CampaignConfig.LEVEL_COUNT,"Invalid unlock sector");
        return new Unlock(metric,target,string(u,"description"));
    }
    private static <T> Map<String,T> entries(JsonValue root,String name,java.util.function.Function<JsonValue,T> factory) {
        Map<String,T> values = new LinkedHashMap<>();
        for (JsonValue n : array(root,name)) {
            String id = string(n,"id"); check(id.matches("[A-Z][A-Z0-9_]{0,63}"),"Invalid id: " + id);
            check(!values.containsKey(id),"Duplicate id in " + name + ": " + id);
            try { values.put(id,factory.apply(n)); }
            catch (RuntimeException e) { throw new IllegalArgumentException(name + "/" + id + ": " + e.getMessage(),e); }
        }
        return Collections.unmodifiableMap(values);
    }
    private static void requireIds(Map<String,?> entries,Enum<?>[] ids) {
        check(entries.size() == ids.length,"Unexpected content count for " + ids[0].getDeclaringClass().getSimpleName());
        for (Enum<?> id : ids) check(entries.containsKey(id.name()),"Missing content id: " + id);
    }
    private static void uniqueKeys(JsonValue node) {
        check(node != null,"Empty JSON"); Set<String> keys = new HashSet<>();
        for (JsonValue child : node) {
            if (node.isObject()) check(keys.add(child.name),"Duplicate JSON key: " + child.name);
            uniqueKeys(child);
        }
    }
    private static JsonValue required(JsonValue n,String key) {
        JsonValue v = n.get(key); check(v != null,"Missing field: " + key); return v;
    }
    private static JsonValue array(JsonValue n,String key) {
        JsonValue v = required(n,key); check(v.isArray(),key + " must be an array"); return v;
    }
    private static String string(JsonValue n,String key) {
        JsonValue v = required(n,key); check(v.isString() && !v.asString().trim().isEmpty(),"Invalid text: " + key); return v.asString();
    }
    private static float number(JsonValue n,String key,float min,float max) {
        JsonValue v = required(n,key); check(v.isNumber(),"Invalid number: " + key);
        float value = v.asFloat(); check(Float.isFinite(value) && value >= min && value <= max,key + " outside " + min + ".." + max); return value;
    }
    private static int integer(JsonValue n,String key,int min,int max) {
        JsonValue v = required(n,key); check(v.isNumber(),"Invalid integer: " + key);
        double value = v.asDouble(); check(Double.isFinite(value) && value == Math.rint(value) && value >= min && value <= max,"Invalid integer: " + key); return (int)value;
    }
    private static void check(boolean valid,String message) { if (!valid) throw new IllegalArgumentException(message); }
}
