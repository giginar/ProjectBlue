package com.projectblue.game.save;

import com.projectblue.game.config.CampaignConfig;
import com.projectblue.game.config.ContentCatalog;
import com.projectblue.game.config.Loadout.*;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.TreeSet;
import java.util.zip.CRC32;
import static com.projectblue.game.config.GameConfig.*;

/** Explicit v0/v1/v2 migrations. Verify integrity before normalizing values. */
public final class ProfileCodec {
    private final ContentCatalog content;
    public ProfileCodec() { this(ContentCatalog.DEFAULT); }
    public ProfileCodec(ContentCatalog content) { this.content = content; }
    public String encode(Profile p) {
        p.normalize();
        Properties fields = new Properties();
        put(fields, "version", PROFILE_VERSION);
        put(fields, "soundEnabled", p.soundEnabled); put(fields, "musicEnabled", p.musicEnabled);
        put(fields, "soundVolume", p.soundVolume); put(fields, "musicVolume", p.musicVolume);
        put(fields, "bestScore", p.bestScore); put(fields, "bestStars", p.bestStars);
        put(fields, "totalSalvage", p.totalSalvage); put(fields, "completedRuns", p.completedRuns);
        put(fields, "reducedMotion", p.reducedMotion);
        put(fields, "selectedPilot", p.selectedPilot); put(fields, "selectedSubmarine", p.selectedSubmarine);
        put(fields, "selectedWeapon", p.selectedWeapon);
        put(fields, "totalPlastic", p.totalPlastic); put(fields, "totalEnemies", p.totalEnemies);
        put(fields, "legacyMagnetLevel", p.legacyMagnetLevel);
        put(fields, "legacyRescuerProgress", p.legacyRescuerProgress); put(fields, "legacyExplorerProgress", p.legacyExplorerProgress);
        for (Pilot pilot : Pilot.values()) put(fields, "pilotUnlocked." + pilot, p.unlocked(pilot));
        for (Submarine sub : Submarine.values()) put(fields, "submarineUnlocked." + sub, p.unlocked(sub));
        for (Weapon weapon : Weapon.values()) put(fields, "weaponUnlocked." + weapon, p.unlocked(weapon));
        for (int id = 1; id <= CampaignConfig.LEVEL_COUNT; id++) {
            LevelRecord r = p.level(id);
            String key = "level." + id + ".";
            put(fields, key + "unlocked", r.unlocked);
            put(fields, key + "stars", r.bestStars); put(fields, key + "score", r.bestScore);
            put(fields, key + "cleanup", r.bestCleanup); put(fields, key + "rescue", r.bestRescue);
            put(fields, key + "difficulties", r.completedDifficulties);
        }
        for (Upgrade u : Upgrade.values()) put(fields, "upgrade." + u, p.upgradeLevel(u));
        for (Achievement a : Achievement.values()) {
            put(fields, "achievement." + a, p.achievementProgress(a));
            put(fields, "achievementUnlocked." + a, p.achievementUnlocked(a));
            put(fields, "notificationPending." + a, p.notificationPending(a));
        }
        String body = canonical(fields);
        return body + "checksum=" + checksum(body) + "\n";
    }
    public Profile decode(String input) throws IOException {
        if (input == null || input.length() > MAX_PROFILE_LENGTH) throw new IOException("Missing or oversized profile");
        try {
            Properties fields = new Properties();
            fields.load(new StringReader(input));
            int version = integer(fields, "version");
            if (version < 0 || version > PROFILE_VERSION) throw new IOException("Unsupported profile version");
            Profile p = new Profile(content);
            p.soundEnabled = bool(fields, "soundEnabled"); p.musicEnabled = bool(fields, "musicEnabled");
            p.bestScore = integer(fields, "bestScore");
            if (version == 0) { ProfileMigrations.fromV0(p); p.normalize(); return p; }
            p.soundVolume = decimal(fields, "soundVolume"); p.musicVolume = decimal(fields, "musicVolume");
            p.bestStars = integer(fields, "bestStars"); p.totalSalvage = integer(fields, "totalSalvage");
            p.completedRuns = integer(fields, "completedRuns");
            String body = version == 1 ? legacyBody(p) : canonical(fields);
            if (!Long.toString(checksum(body)).equals(required(fields, "checksum"))) throw new IOException("Checksum mismatch");
            if (version == 1) {
                ProfileMigrations.fromV1(p);
            } else {
                p.reducedMotion = bool(fields, "reducedMotion");
                for (int id = 1; id <= CampaignConfig.LEVEL_COUNT; id++) {
                    LevelRecord r = p.level(id);
                    String key = "level." + id + ".";
                    r.unlocked = bool(fields, key + "unlocked");
                    r.bestStars = integer(fields, key + "stars"); r.bestScore = integer(fields, key + "score");
                    r.bestCleanup = decimal(fields, key + "cleanup"); r.bestRescue = decimal(fields, key + "rescue");
                    r.completedDifficulties = integer(fields, key + "difficulties");
                }
                if (version == 2) ProfileMigrations.fromV2(p, fields);
                else {
                    p.selectedPilot = Pilot.valueOf(required(fields, "selectedPilot"));
                    p.selectedSubmarine = Submarine.valueOf(required(fields, "selectedSubmarine"));
                    p.selectedWeapon = Weapon.valueOf(required(fields, "selectedWeapon"));
                    p.totalPlastic = integer(fields, "totalPlastic"); p.totalEnemies = integer(fields, "totalEnemies");
                    p.legacyMagnetLevel = integer(fields, "legacyMagnetLevel");
                    p.legacyRescuerProgress = integer(fields, "legacyRescuerProgress"); p.legacyExplorerProgress = integer(fields, "legacyExplorerProgress");
                    for (Pilot pilot : Pilot.values()) if (bool(fields, "pilotUnlocked." + pilot)) p.restoreUnlocked(pilot);
                    for (Submarine sub : Submarine.values()) if (bool(fields, "submarineUnlocked." + sub)) p.restoreUnlocked(sub);
                    for (Weapon weapon : Weapon.values()) if (bool(fields, "weaponUnlocked." + weapon)) p.restoreUnlocked(weapon);
                    for (Upgrade u : Upgrade.values()) p.restoreUpgrade(u, integer(fields, "upgrade." + u));
                    for (Achievement a : Achievement.values()) {
                        p.restoreAchievement(a, integer(fields, "achievement." + a));
                        p.restoreAchievementState(a, bool(fields, "achievementUnlocked." + a), bool(fields, "notificationPending." + a));
                    }
                }
            }
            p.normalize();
            if (version < PROFILE_VERSION) p.evaluateAchievements(null);
            return p;
        } catch (IllegalArgumentException e) { throw new IOException("Malformed profile", e); }
    }
    private static void put(Properties p, String key, Object value) { p.setProperty(key, value.toString()); }
    private static int integer(Properties p, String key) throws IOException { return Integer.parseInt(required(p, key)); }
    private static float decimal(Properties p, String key) throws IOException {
        float value = Float.parseFloat(required(p, key));
        if (!Float.isFinite(value)) throw new IOException("Non-finite field: " + key);
        return value;
    }
    private static boolean bool(Properties p, String key) throws IOException {
        String value = required(p, key);
        if (!value.equals("true") && !value.equals("false")) throw new IOException("Invalid boolean");
        return Boolean.parseBoolean(value);
    }
    private static String required(Properties p, String key) throws IOException {
        String value = p.getProperty(key);
        if (value == null) throw new IOException("Missing profile field: " + key);
        return value;
    }
    private static String canonical(Properties p) {
        StringBuilder body = new StringBuilder("version=" + p.getProperty("version") + "\n");
        for (String key : new TreeSet<>(p.stringPropertyNames())) {
            if (!key.equals("checksum") && !key.equals("version")) body.append(key).append('=').append(p.getProperty(key)).append('\n');
        }
        return body.toString();
    }
    private static String legacyBody(Profile p) {
        return "version=1\nsoundEnabled=" + p.soundEnabled
            + "\nmusicEnabled=" + p.musicEnabled + "\nsoundVolume=" + p.soundVolume
            + "\nmusicVolume=" + p.musicVolume + "\nbestScore=" + p.bestScore
            + "\nbestStars=" + p.bestStars + "\ntotalSalvage=" + p.totalSalvage
            + "\ncompletedRuns=" + p.completedRuns + "\n";
    }
    private static long checksum(String body) {
        CRC32 crc = new CRC32();
        crc.update(body.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
