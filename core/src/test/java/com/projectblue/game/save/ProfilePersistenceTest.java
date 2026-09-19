package com.projectblue.game.save;

import com.badlogic.gdx.files.FileHandle;
import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.logic.LevelResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class ProfilePersistenceTest {
    @TempDir Path directory;
    private SaveService open() { return new SaveService(new GdxSaveStore(new FileHandle(directory.toFile()))); }
    private void complete(Profile p, int id, Difficulty d) {
        RunSpec spec = RunSpec.create(id, d, Loadout.standard());
        assertTrue(p.record(new LevelResult(spec, true, spec.combatTargets(), 36, 5, 100, 100)));
    }
    @Test void realFileReopenRestoresAllProfileFieldsAndIndependentLevelRecords() {
        SaveService first = open(); Profile p = first.profile();
        complete(p, 1, Difficulty.NORMAL); complete(p, 1, Difficulty.HARD); complete(p, 2, Difficulty.NORMAL);
        p.selectedPilot = Pilot.NERI; p.selectedSubmarine = Submarine.MANTA;
        p.soundEnabled = false; p.musicEnabled = false; p.soundVolume = .25f; p.musicVolume = .75f;
        p.reducedMotion = true;
        assertTrue(p.purchase(Upgrade.HULL)); assertTrue(p.purchase(Upgrade.PRIMARY_WEAPON)); assertTrue(p.purchase(Upgrade.SHIELD));
        assertTrue(first.save());
        Profile restored = open().profile();
        assertEquals(PROFILE_VERSION, restored.version);
        assertTrue(restored.canPlay(3, Difficulty.NORMAL)); assertFalse(restored.canPlay(4, Difficulty.NORMAL));
        assertTrue(restored.canPlay(1, Difficulty.EXPERT)); assertFalse(restored.canPlay(2, Difficulty.EXPERT));
        assertEquals(p.level(1).bestScore, restored.level(1).bestScore);
        assertEquals(3, restored.level(2).bestStars); assertEquals(100, restored.level(2).bestCleanup);
        assertEquals(100, restored.level(2).bestRescue); assertEquals(Difficulty.HARD, restored.level(1).bestDifficulty());
        assertEquals(300-Upgrade.HULL.cost(0)-Upgrade.PRIMARY_WEAPON.cost(0)-Upgrade.SHIELD.cost(0),
            restored.totalSalvage);
        assertEquals(Pilot.NERI, restored.selectedPilot); assertEquals(Submarine.MANTA, restored.selectedSubmarine);
        for (Upgrade u : Upgrade.values()) assertEquals(p.upgradeLevel(u), restored.upgradeLevel(u));
        assertEquals(100, restored.achievementProgress(Achievement.RECYCLER_I));
        assertEquals(108, restored.achievementProgress(Achievement.RECYCLER_II));
        assertTrue(restored.reducedMotion); assertFalse(restored.soundEnabled); assertFalse(restored.musicEnabled);
        assertEquals(.25f, restored.soundVolume); assertEquals(.75f, restored.musicVolume);
    }
    @Test void corruptPrimaryRestoresVerifiedBackupAndSubsequentSaveKeepsGoodBackup() throws IOException {
        SaveService first = open(); complete(first.profile(), 1, Difficulty.NORMAL);
        assertTrue(first.save()); assertTrue(first.save());
        Files.writeString(directory.resolve(SAVE_FILE), "truncated profile");
        SaveService recovered = open();
        assertTrue(recovered.recovered()); assertTrue(recovered.recoveredBackup());
        assertTrue(recovered.profile().canPlay(2, Difficulty.NORMAL));
        assertTrue(recovered.save());
        assertTrue(new ProfileCodec().decode(Files.readString(directory.resolve(SAVE_FILE + ".bak"))).canPlay(2, Difficulty.NORMAL));
    }
    @Test void corruptBothFilesOrOversizedInputSafelyStartsFresh() throws IOException {
        Files.writeString(directory.resolve(SAVE_FILE), "x".repeat(MAX_PROFILE_LENGTH + 1));
        Files.writeString(directory.resolve(SAVE_FILE + ".bak"), "broken");
        SaveService recovered = open();
        assertTrue(recovered.recovered()); assertFalse(recovered.recoveredBackup());
        assertTrue(recovered.profile().canPlay(1, Difficulty.NORMAL));
        assertFalse(recovered.profile().canPlay(2, Difficulty.NORMAL));
        assertTrue(recovered.save()); assertFalse(open().recovered());
    }
    @Test void missingPrimaryRecoversBackupAfterInterruptedReplacement() throws IOException {
        SaveService first = open(); complete(first.profile(), 1, Difficulty.NORMAL);
        first.save(); first.save(); Files.delete(directory.resolve(SAVE_FILE));
        assertTrue(open().profile().canPlay(2, Difficulty.NORMAL));
    }
    @Test void legacyV1PreservesSliceProgressAndMigratesToCurrentSchema() throws IOException {
        String body = "version=1\nsoundEnabled=false\nmusicEnabled=true\nsoundVolume=0.45\nmusicVolume=0.25"
            + "\nbestScore=6000\nbestStars=2\ntotalSalvage=90\ncompletedRuns=3\n";
        Files.writeString(directory.resolve(SAVE_FILE), withChecksum(body));
        SaveService migrated = open(); Profile p = migrated.profile();
        assertFalse(migrated.recovered()); assertEquals(PROFILE_VERSION, p.version);
        assertEquals(6000, p.level(1).bestScore); assertEquals(2, p.level(1).bestStars);
        assertTrue(p.canPlay(2, Difficulty.NORMAL)); assertTrue(p.canPlay(1, Difficulty.HARD));
        assertFalse(p.canPlay(1, Difficulty.EXPERT)); assertEquals(0, p.level(1).bestCleanup);
        assertEquals(90, p.totalSalvage); assertEquals(3, p.completedRuns); assertFalse(p.soundEnabled);
        assertTrue(migrated.save()); assertTrue(Files.readString(directory.resolve(SAVE_FILE)).startsWith("version="+PROFILE_VERSION+"\n"));
        assertTrue(open().profile().canPlay(2, Difficulty.NORMAL));
    }
    @Test void legacyV1WithoutACompletedDiveDoesNotInventUnlocks() throws IOException {
        String body = "version=1\nsoundEnabled=true\nmusicEnabled=true\nsoundVolume=0.45\nmusicVolume=0.25"
            + "\nbestScore=900\nbestStars=0\ntotalSalvage=10\ncompletedRuns=0\n";
        Profile p = new ProfileCodec().decode(withChecksum(body));
        assertFalse(p.canPlay(2, Difficulty.NORMAL)); assertFalse(p.canPlay(1, Difficulty.HARD));
    }
    @Test void currentSchemaDetectsTamperingInEveryProgressionGroup() throws IOException {
        ProfileCodec codec = new ProfileCodec(); String encoded = codec.encode(new Profile());
        for (String[] change : new String[][]{{"level.2.unlocked=false", "level.2.unlocked=true"},
            {"level.1.difficulties=0", "level.1.difficulties=15"}, {"selectedPilot=KAIA", "selectedPilot=NERI"},
            {"upgrade.HULL=0", "upgrade.HULL=5"}, {"achievement.RECYCLER_I=0", "achievement.RECYCLER_I=100"},
            {"soundVolume=0.45", "soundVolume=NaN"}}) {
            assertTrue(encoded.contains(change[0]));
            assertThrows(IOException.class, () -> codec.decode(encoded.replace(change[0], change[1])));
        }
    }
    @Test void resetRequiresDevelopmentFlagKeepsReferencesAndSurvivesReopen() {
        SaveService service = open(); Profile original = service.profile(); complete(original, 1, Difficulty.NORMAL);
        assertFalse(service.resetForDevelopment(false)); assertTrue(original.canPlay(2, Difficulty.NORMAL));
        assertTrue(service.resetForDevelopment(true)); assertSame(original, service.profile());
        assertFalse(original.canPlay(2, Difficulty.NORMAL)); assertEquals(0, original.totalSalvage);
        assertFalse(open().profile().canPlay(2, Difficulty.NORMAL));
    }
    private String withChecksum(String body) {
        CRC32 crc = new CRC32(); crc.update(body.getBytes(StandardCharsets.UTF_8));
        return body + "checksum=" + crc.getValue() + "\n";
    }
}
