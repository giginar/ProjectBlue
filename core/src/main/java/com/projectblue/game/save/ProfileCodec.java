package com.projectblue.game.save;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.zip.CRC32;
import static com.projectblue.game.config.GameConfig.*;

/** Version 0 migration is explicit. Unknown future schemas are never silently interpreted. */
public final class ProfileCodec {
    public String encode(Profile p) {
        p.normalize();
        String body = body(p);
        return body + "checksum=" + checksum(body) + "\n";
    }
    public Profile decode(String input) throws IOException {
        if (input == null || input.length() > MAX_PROFILE_LENGTH) throw new IOException("Missing or oversized profile");
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(input));
            int version = Integer.parseInt(required(properties, "version"));
            if (version != 0 && version != PROFILE_VERSION) throw new IOException("Unsupported profile version");
            Profile p = new Profile();
            p.soundEnabled = bool(properties, "soundEnabled");
            p.musicEnabled = bool(properties, "musicEnabled");
            p.bestScore = Integer.parseInt(required(properties, "bestScore"));
            if (version == 0) { p.normalize(); return p; }
            p.soundVolume = Float.parseFloat(required(properties, "soundVolume"));
            p.musicVolume = Float.parseFloat(required(properties, "musicVolume"));
            p.bestStars = Integer.parseInt(required(properties, "bestStars"));
            p.totalSalvage = Integer.parseInt(required(properties, "totalSalvage"));
            p.completedRuns = Integer.parseInt(required(properties, "completedRuns"));
            if (!Long.toString(checksum(body(p))).equals(required(properties, "checksum"))) throw new IOException("Checksum mismatch");
            if (!Float.isFinite(p.soundVolume) || !Float.isFinite(p.musicVolume)) throw new IOException("Non-finite volume");
            p.normalize();
            return p;
        } catch (IllegalArgumentException e) { throw new IOException("Malformed profile", e); }
    }
    private boolean bool(Properties p, String key) throws IOException {
        String value = required(p, key);
        if (!value.equals("true") && !value.equals("false")) throw new IOException("Invalid boolean");
        return Boolean.parseBoolean(value);
    }
    private String required(Properties p, String key) throws IOException {
        String value = p.getProperty(key);
        if (value == null) throw new IOException("Missing profile field: " + key);
        return value;
    }
    private String body(Profile p) {
        return "version=" + PROFILE_VERSION + "\nsoundEnabled=" + p.soundEnabled
            + "\nmusicEnabled=" + p.musicEnabled + "\nsoundVolume=" + p.soundVolume
            + "\nmusicVolume=" + p.musicVolume + "\nbestScore=" + p.bestScore
            + "\nbestStars=" + p.bestStars + "\ntotalSalvage=" + p.totalSalvage
            + "\ncompletedRuns=" + p.completedRuns + "\n";
    }
    private long checksum(String body) {
        CRC32 crc = new CRC32();
        crc.update(body.getBytes(StandardCharsets.UTF_8));
        return crc.getValue();
    }
}
