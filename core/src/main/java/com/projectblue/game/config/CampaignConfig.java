package com.projectblue.game.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Collections;

/** Authored campaign data shared by menus, progression, and the pure Java simulation. */
public final class CampaignConfig {
    public static final int LEVEL_COUNT = 10;
    public static final CampaignConfig DEFAULT = loadResource();
    public record Level(int id, String name, String region, long seed, boolean boss) {}
    public record Tuning(float health, float bulletSpeed, float spawnDensity, float fireRate,
                         float bossCadence, int bossProjectiles, float bossMovement, int bossPhases) {
        public int droneHealth() { return Math.round(GameConfig.DRONE_HEALTH * health); }
        public int droneCount() { return Math.round(GameConfig.DRONE_COUNT * spawnDensity); }
        public float droneInterval() { return GameConfig.DRONE_INTERVAL / spawnDensity; }
        public float shotSpeed() { return GameConfig.ENEMY_BULLET_SPEED * bulletSpeed; }
        public float shotInterval() { return GameConfig.ENEMY_SHOT_INTERVAL / fireRate; }
        public int bossHealth() { return Math.round(260 * health); }
        public float bossInterval() { return 2.8f / bossCadence; }
    }
    private final List<Level> levels;
    private final EnumMap<Difficulty, Tuning> difficulties;

    private CampaignConfig(List<Level> levels, EnumMap<Difficulty, Tuning> difficulties) {
        this.levels = Collections.unmodifiableList(new ArrayList<>(levels));
        this.difficulties = difficulties;
    }
    public List<Level> levels() { return levels; }
    public Level level(int id) {
        if (id < 1 || id > LEVEL_COUNT) throw new IllegalArgumentException("Invalid level: " + id);
        return levels.get(id - 1);
    }
    public Tuning tuning(Difficulty difficulty) { return difficulties.get(difficulty); }
    public static boolean isAvailable(int id) { return id >= 1 && id <= 5; }

    public static CampaignConfig read(InputStream source) throws IOException {
        if (source == null) throw new IOException("Missing campaign config");
        Properties p = new Properties();
        p.load(source);
        try {
            List<Level> levels = new ArrayList<>();
            for (int id = 1; id <= LEVEL_COUNT; id++) {
                String key = "level." + id + ".";
                String boss = required(p, key + "boss");
                if (!boss.equals("true") && !boss.equals("false")) throw new IllegalArgumentException("Invalid boss flag");
                levels.add(new Level(id, required(p, key + "name"), required(p, key + "region"),
                    Long.parseLong(required(p, key + "seed")), Boolean.parseBoolean(boss)));
            }
            EnumMap<Difficulty, Tuning> difficulties = new EnumMap<>(Difficulty.class);
            for (Difficulty d : Difficulty.values()) {
                String key = "difficulty." + d + ".";
                difficulties.put(d, new Tuning(number(p, key + "health", 1, 4),
                    number(p, key + "bulletSpeed", 1, 3), number(p, key + "spawnDensity", 1, 2.5f),
                    number(p, key + "fireRate", 1, 3), number(p, key + "bossCadence", 1, 3),
                    integer(p, key + "bossProjectiles", 1, 9), number(p, key + "bossMovement", 1, 3),
                    integer(p, key + "bossPhases", 1, 3)));
            }
            return new CampaignConfig(levels, difficulties);
        } catch (IllegalArgumentException e) { throw new IOException("Invalid campaign config", e); }
    }
    private static String required(Properties p, String key) {
        String value = p.getProperty(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
    private static float number(Properties p, String key, float min, float max) {
        float value = Float.parseFloat(required(p, key));
        if (!Float.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Out of range: " + key);
        return value;
    }
    private static int integer(Properties p, String key, int min, int max) {
        float value = number(p, key, min, max);
        if (value != (int) value) throw new IllegalArgumentException("Expected integer: " + key);
        return (int) value;
    }
    private static CampaignConfig loadResource() {
        try (InputStream source = CampaignConfig.class.getResourceAsStream("/config/campaign.properties")) {
            return read(source);
        } catch (IOException e) { throw new IllegalStateException("Cannot load campaign", e); }
    }
}
