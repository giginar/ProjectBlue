package com.projectblue.game.save;
import java.io.IOException;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.logic.LevelResult;
import java.util.function.Predicate;
import com.projectblue.game.config.ContentCatalog;

public final class SaveService {
    private final SaveStore store;
    private final ProfileCodec codec;
    private final Profile profile;
    private boolean recovered, recoveredBackup, writeFailed;
    public SaveService(SaveStore store) {
        this(store, ContentCatalog.DEFAULT);
    }
    public SaveService(SaveStore store, ContentCatalog content) {
        this.store = store;
        codec = new ProfileCodec(content);
        Profile loaded;
        try {
            String input = store.read();
            loaded = input == null ? new Profile(content) : codec.decode(input);
        } catch (IOException | RuntimeException e) {
            loaded = new Profile(content);
            recovered = true;
            try {
                String backup = store.readBackup();
                if (backup != null) { loaded = codec.decode(backup); recoveredBackup = true; }
            } catch (IOException | RuntimeException ignored) { /* Defaults remain usable. */ }
        }
        profile = loaded;
    }
    public Profile profile() { return profile; }
    public boolean recovered() { return recovered; }
    public boolean recoveredBackup() { return recoveredBackup; }
    public boolean resetForDevelopment(boolean developmentBuild) {
        if (!developmentBuild) return false;
        profile.reset();
        recovered = recoveredBackup = false;
        // Refresh the backup as well so recovery cannot resurrect pre-reset progress.
        return save() && save();
    }
    public boolean writeFailed() { return writeFailed; }
    public synchronized boolean save() {
        try { store.write(codec.encode(profile)); writeFailed = false; return true; }
        catch (IOException | RuntimeException e) { writeFailed = true; return false; }
    }
    public enum PurchaseResult { PURCHASED, MAX_LEVEL, INSUFFICIENT_SALVAGE, SAVE_FAILED }
    public enum RecordResult { RECORDED, REJECTED, SAVE_FAILED }
    /**
     * Results keep their candidate in memory when storage is temporarily unavailable. A later
     * lifecycle or Settings save can retry without making the player repeat a completed dive.
     */
    public synchronized RecordResult record(LevelResult result) {
        Profile candidate;
        try {
            candidate = codec.decode(codec.encode(profile));
            if (!candidate.record(result)) return RecordResult.REJECTED;
            candidate.normalize();
        } catch (IOException | RuntimeException e) {
            return RecordResult.REJECTED;
        }
        try {
            store.write(codec.encode(candidate));
            profile.copyFrom(candidate);
            writeFailed = false;
            return RecordResult.RECORDED;
        } catch (IOException | RuntimeException e) {
            profile.copyFrom(candidate);
            writeFailed = true;
            return RecordResult.SAVE_FAILED;
        }
    }
    public synchronized PurchaseResult purchase(Upgrade upgrade) {
        var definition = profile.content().upgrade(upgrade.name());
        int current = profile.upgradeLevel(upgrade);
        if (current >= definition.maxLevel()) return PurchaseResult.MAX_LEVEL;
        if (profile.totalSalvage < definition.cost(current)) return PurchaseResult.INSUFFICIENT_SALVAGE;
        return commit(candidate -> candidate.purchase(upgrade)) ? PurchaseResult.PURCHASED : PurchaseResult.SAVE_FAILED;
    }
    public synchronized boolean select(Pilot pilot) {
        return profile.unlocked(pilot) && commit(p -> { p.selectedPilot = pilot; return true; });
    }
    public synchronized boolean select(Submarine sub) {
        return profile.unlocked(sub) && commit(p -> { p.selectedSubmarine = sub; return true; });
    }
    public synchronized boolean select(Weapon weapon) {
        return profile.unlocked(weapon) && commit(p -> { p.selectedWeapon = weapon; return true; });
    }
    public synchronized boolean acknowledge(Achievement achievement) {
        return commit(p -> { p.acknowledge(achievement); return true; });
    }
    private boolean commit(Predicate<Profile> change) {
        try {
            Profile candidate = codec.decode(codec.encode(profile));
            if (!change.test(candidate)) return false;
            candidate.normalize();
            store.write(codec.encode(candidate));
            profile.copyFrom(candidate);
            writeFailed = false;
            return true;
        } catch (IOException | RuntimeException e) { writeFailed = true; return false; }
    }
}
