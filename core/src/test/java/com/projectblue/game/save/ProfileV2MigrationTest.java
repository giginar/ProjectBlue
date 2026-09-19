package com.projectblue.game.save;
import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.CRC32;
import static org.junit.jupiter.api.Assertions.*;

class ProfileV2MigrationTest {
    private String fixture() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/profiles/v2.properties")) {
            return new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    @Test void v2MigrationPreservesEverySectorRecordEconomyAndPaidBenefit() throws IOException {
        ProfileCodec codec = new ProfileCodec();
        Profile migrated = codec.decode(fixture());
        assertEquals(3,migrated.version); assertEquals(444,migrated.totalSalvage); assertEquals(8,migrated.completedRuns);
        assertEquals(9000,migrated.bestScore); assertEquals(3,migrated.bestStars);
        assertFalse(migrated.soundEnabled); assertTrue(migrated.reducedMotion);
        assertEquals(Pilot.NERI,migrated.selectedPilot); assertEquals(Submarine.LEVIATHAN,migrated.selectedSubmarine);
        assertEquals(Weapon.PULSE_CANNON,migrated.selectedWeapon);
        assertEquals(5,migrated.upgradeLevel(Upgrade.HULL)); assertEquals(3,migrated.upgradeLevel(Upgrade.PRIMARY_WEAPON));
        assertEquals(2,migrated.legacyMagnetLevel); assertEquals(149,Loadout.from(migrated).salvageRadius());
        assertEquals(25,migrated.legacyRescuerProgress); assertEquals(4,migrated.legacyExplorerProgress);
        assertEquals(100,migrated.totalPlastic); assertEquals(100,migrated.achievementProgress(Achievement.RECYCLER_II));
        assertEquals(0,migrated.totalEnemies,"Old saves have no historical kill count");
        assertTrue(migrated.achievementUnlocked(Achievement.FIRST_DIVE));
        assertFalse(migrated.notificationPending(Achievement.FIRST_DIVE),"Migrated unlocks must not announce again");
        assertTrue(migrated.achievementUnlocked(Achievement.FULLY_EQUIPPED));
        assertTrue(migrated.achievementUnlocked(Achievement.DEEP_EXPLORER));
        assertFalse(migrated.achievementUnlocked(Achievement.UNTOUCHED),"Old integrity cannot prove no damage");
        for (int id = 1; id <= 10; id++) {
            LevelRecord r = migrated.level(id);
            assertEquals(id <= 5,r.unlocked); assertEquals(id <= 4 ? (id == 1 ? 3 : 1) : 0,r.bestStars);
            assertEquals(id <= 4 ? (id == 1 ? 9000 : 1000) : 0,r.bestScore);
            assertEquals(id == 1 ? 100 : 0,r.bestCleanup); assertEquals(id == 1 ? 100 : 0,r.bestRescue);
            assertEquals(id <= 4 ? (id == 1 ? 3 : 1) : 0,r.completedDifficulties);
        }
        assertTrue(migrated.canPlay(1,Difficulty.EXPERT)); assertFalse(migrated.canPlay(2,Difficulty.EXPERT));
        Profile reopened = codec.decode(codec.encode(migrated));
        assertEquals(Submarine.LEVIATHAN,reopened.selectedSubmarine); assertEquals(444,reopened.totalSalvage);
        assertEquals(149,Loadout.from(reopened).salvageRadius());
        assertTrue(reopened.canPlay(5,Difficulty.NORMAL)); assertFalse(reopened.canPlay(6,Difficulty.NORMAL));
    }
    @Test void oldFreeSelectionsStayAvailableEvenBeforeNewUnlockRequirements() throws IOException {
        Properties fields = new Properties(); fields.load(new StringReader(fixture()));
        fields.setProperty("selectedPilot","SOL"); fields.setProperty("selectedSubmarine","NEEDLE");
        for (int id = 1; id <= 10; id++) {
            fields.setProperty("level."+id+".stars","0"); fields.setProperty("level."+id+".difficulties","0");
        }
        StringBuilder body = new StringBuilder("version=2\n");
        for (String key : new TreeSet<>(fields.stringPropertyNames())) {
            if (!key.equals("checksum") && !key.equals("version")) body.append(key).append('=').append(fields.getProperty(key)).append('\n');
        }
        CRC32 crc = new CRC32(); crc.update(body.toString().getBytes(StandardCharsets.UTF_8));
        Profile p = new ProfileCodec().decode(body + "checksum=" + crc.getValue() + "\n");
        assertEquals(Pilot.KAIA,p.selectedPilot); assertEquals(Submarine.MANTA,p.selectedSubmarine);
        assertFalse(p.canPlay(2,Difficulty.NORMAL));
        assertTrue(p.unlocked(Submarine.LEVIATHAN)); assertTrue(p.unlocked(Pilot.NERI));
        assertFalse(p.unlocked(Pilot.ATLAS)); assertFalse(p.unlocked(Weapon.SPREAD_CANNON));
    }
}
