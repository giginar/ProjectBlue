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
    // Not restored: a process restart cannot replay an old result or reward callback.
    private String activeRun = "";
    public synchronized String beginRun() {
        activeRun = java.util.UUID.randomUUID().toString();
        profile.rewardRun = activeRun;
        profile.runResultRecorded = profile.runCompleted = profile.continueUsed = profile.salvageDoubled = false;
        profile.runSalvage = profile.runPlastic = profile.runEnemies = 0;
        save();
        return activeRun;
    }
    public synchronized void endRun() { activeRun = ""; }
    private boolean active(String id) { return !activeRun.isEmpty() && activeRun.equals(id) && id.equals(profile.rewardRun); }
    public synchronized boolean canDouble(String id) {
        return active(id) && profile.runResultRecorded && profile.runCompleted && !profile.salvageDoubled && profile.runSalvage > 0;
    }
    public synchronized boolean canContinue(String id) {
        return active(id) && profile.runResultRecorded && !profile.runCompleted && !profile.continueUsed;
    }
    public synchronized boolean doubleSalvage(String id) {
        return canDouble(id) && commit(p -> {
            p.salvageDoubled = true;
            p.totalSalvage = (int)Math.min(Integer.MAX_VALUE, (long)p.totalSalvage + p.runSalvage);
            return true;
        });
    }
    public synchronized boolean useContinue(String id) {
        return canContinue(id) && commit(p -> { p.continueUsed = true; p.runResultRecorded = false; return true; });
    }
    public synchronized boolean reserveInterstitial(long now) {
        return commit(p -> { p.lastInterstitialAt = now; p.adCompletions = 0; return true; });
    }
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
        return recordInternal(null, result);
    }
    public synchronized RecordResult recordRun(String id, LevelResult result) {
        if (!active(id) || profile.runResultRecorded) return RecordResult.REJECTED;
        return recordInternal(id, result);
    }
    private RecordResult recordInternal(String id, LevelResult result) {
        Profile candidate;
        try {
            candidate = codec.decode(codec.encode(profile));
            if (!candidate.record(result, id == null ? 0 : candidate.runSalvage,
                id == null ? 0 : candidate.runPlastic, id == null ? 0 : candidate.runEnemies)) return RecordResult.REJECTED;
            if (id != null) {
                candidate.runResultRecorded = true; candidate.runCompleted = result.completed;
                candidate.runSalvage = result.salvage; candidate.runPlastic = result.plasticCollected;
                candidate.runEnemies = result.enemiesDestroyed;
                if (result.completed) candidate.adCompletions = Math.min(Integer.MAX_VALUE - 1, candidate.adCompletions) + 1;
            }
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
