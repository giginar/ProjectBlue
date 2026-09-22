package com.projectblue.game.save;
import static com.projectblue.game.config.GameConfig.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.logic.Rules;
import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.i18n.GameLanguage;
import java.util.*;

/** Profile owns local facts; catalog definitions decide their gameplay meaning. */
public final class Profile {
    private final ContentCatalog content;
    public int version = PROFILE_VERSION;
    public boolean soundEnabled = true, musicEnabled = true, muted;
    public float soundVolume = DEFAULT_SOUND_VOLUME, musicVolume = DEFAULT_MUSIC_VOLUME;
    public int bestScore, bestStars, totalSalvage, completedRuns, totalPlastic, totalEnemies;
    public String rewardRun = "";
    /** Stable language id. Empty means first-launch choice is still required. */
    public String language = "";
    public boolean runResultRecorded, runCompleted, continueUsed, salvageDoubled;
    public int runSalvage, runPlastic, runEnemies, adSessions, adCompletions;
    public long lastInterstitialAt;
    // Preserve the paid v2 magnet benefit without adding a seventh purchasable upgrade.
    public int legacyMagnetLevel, legacyRescuerProgress, legacyExplorerProgress;
    public boolean reducedMotion, hapticEnabled = true, screenShakeEnabled = true;
    public boolean highContrastTelegraphs, largeUi, reducedFlashes;
    public Pilot selectedPilot = Pilot.KAIA;
    public Submarine selectedSubmarine = Submarine.TIDE;
    public Weapon selectedWeapon = Weapon.PULSE_CANNON;
    private final LevelRecord[] levels = new LevelRecord[CampaignConfig.LEVEL_COUNT];
    private final EnumMap<Upgrade,Integer> upgrades = new EnumMap<>(Upgrade.class);
    private final EnumMap<Achievement,Integer> achievements = new EnumMap<>(Achievement.class);
    private final EnumSet<Achievement> unlockedAchievements = EnumSet.noneOf(Achievement.class);
    private final EnumSet<Achievement> pendingNotifications = EnumSet.noneOf(Achievement.class);
    private final EnumSet<Pilot> unlockedPilots = EnumSet.of(Pilot.KAIA);
    private final EnumSet<Submarine> unlockedSubmarines = EnumSet.of(Submarine.TIDE);
    private final EnumSet<Weapon> unlockedWeapons = EnumSet.of(Weapon.PULSE_CANNON);

    public Profile() { this(ContentCatalog.DEFAULT); }
    public Profile(ContentCatalog content) {
        this.content = Objects.requireNonNull(content);
        for (int i = 0; i < levels.length; i++) levels[i] = new LevelRecord();
        levels[0].unlocked = true;
    }
    public ContentCatalog content() { return content; }
    public LevelRecord level(int id) {
        if (id < 1 || id > levels.length) throw new IllegalArgumentException("Invalid level: " + id);
        return levels[id - 1];
    }
    public boolean canPlay(int id,Difficulty difficulty) {
        return id >= 1 && id <= levels.length && level(id).canPlay(difficulty);
    }
    public boolean campaignCompleted() { return level(CampaignConfig.LEVEL_COUNT).completed(Difficulty.NORMAL); }
    public boolean meets(ContentCatalog.Unlock condition) {
        return switch (condition.metric()) {
            case ALWAYS -> true;
            case LEVEL_UNLOCKED -> canPlay(condition.target(),Difficulty.NORMAL);
            case PLASTIC -> totalPlastic >= condition.target();
            case ENEMIES -> totalEnemies >= condition.target();
            case COMPLETED_RUNS -> completedRuns >= condition.target();
        };
    }
    public boolean unlocked(Pilot p) { return p != null && (unlockedPilots.contains(p) || meets(content.pilot(p.name()).unlockCondition())); }
    public boolean unlocked(Submarine s) { return s != null && (unlockedSubmarines.contains(s) || meets(content.submarine(s.name()).unlockCondition())); }
    public boolean unlocked(Weapon w) { return w != null && (unlockedWeapons.contains(w) || meets(content.weapon(w.name()).unlockCondition())); }
    void restoreUnlocked(Pilot p) { unlockedPilots.add(p); }
    void restoreUnlocked(Submarine s) { unlockedSubmarines.add(s); }
    void restoreUnlocked(Weapon w) { unlockedWeapons.add(w); }
    public int upgradeLevel(Upgrade upgrade) { return upgrades.getOrDefault(upgrade,0); }
    void restoreUpgrade(Upgrade upgrade,int value) {
        upgrades.put(upgrade,Math.max(0,Math.min(content.upgrade(upgrade.name()).maxLevel(),value)));
    }
    /** Candidate-only mutation. Production purchases must use SaveService.purchase. */
    boolean purchase(Upgrade upgrade) {
        if (upgrade == null) return false;
        var def = content.upgrade(upgrade.name());
        int level = upgradeLevel(upgrade), cost = def.cost(level);
        if (level >= def.maxLevel() || totalSalvage < cost) return false;
        totalSalvage -= cost; upgrades.put(upgrade,level + 1);
        evaluateAchievements(null);
        return true;
    }
    public int achievementProgress(Achievement a) { return achievements.getOrDefault(a,0); }
    public boolean achievementUnlocked(Achievement a) { return unlockedAchievements.contains(a); }
    public boolean notificationPending(Achievement a) { return pendingNotifications.contains(a); }
    void acknowledge(Achievement a) { pendingNotifications.remove(a); }
    void restoreAchievement(Achievement a,int value) {
        achievements.put(a,Math.max(0,Math.min(content.achievement(a.name()).target(),value)));
    }
    void restoreAchievementState(Achievement a,boolean unlocked,boolean pending) {
        if (unlocked) unlockedAchievements.add(a);
        if (unlocked && pending) pendingNotifications.add(a);
    }
    void evaluateAchievements(LevelResult result) {
        for (Achievement a : Achievement.values()) {
            var def = content.achievement(a.name());
            int value = switch (def.metric()) {
                case PLASTIC -> totalPlastic;
                case ENEMIES -> totalEnemies;
                case LEVEL_UNLOCKED -> canPlay(def.threshold(),Difficulty.NORMAL) ? 1 : 0;
                case LEVEL_COMPLETED -> level(def.threshold()).bestStars > 0 ? 1 : 0;
                case CLEANUP -> result != null && result.cleanup >= def.threshold() ? 1 : 0;
                case RESCUE -> result != null && result.rescue >= def.threshold() ? 1 : 0;
                case UNTOUCHED -> result != null && result.completed && result.damageTaken == 0 ? 1 : 0;
                case ABYSS -> result != null && result.completed && result.difficulty == Difficulty.ABYSS ? 1 : 0;
                case MAX_UPGRADE -> Arrays.stream(Upgrade.values()).anyMatch(u -> upgradeLevel(u) == content.upgrade(u.name()).maxLevel()) ? 1 : 0;
            };
            restoreAchievement(a,Math.max(achievementProgress(a),value));
            if (achievementProgress(a) >= def.target() && unlockedAchievements.add(a)) pendingNotifications.add(a);
        }
    }
    private static int add(int a,int b) { return (int)Math.min(Integer.MAX_VALUE,(long)a + b); }
    public void normalize() {
        version = PROFILE_VERSION;
        if (GameLanguage.fromId(language) == null) language = "";
        soundVolume = Rules.clamp(soundVolume,0,1); musicVolume = Rules.clamp(musicVolume,0,1);
        bestScore = Math.max(0,bestScore); bestStars = Math.max(0,Math.min(3,bestStars));
        totalSalvage = Math.max(0,totalSalvage); completedRuns = Math.max(0,completedRuns);
        totalPlastic = Math.max(0,totalPlastic); totalEnemies = Math.max(0,totalEnemies);
        legacyMagnetLevel = Math.max(0,Math.min(5,legacyMagnetLevel));
        for (int i = 0; i < levels.length; i++) {
            levels[i].normalize();
            levels[i].unlocked = i == 0 || (levels[i-1].unlocked && levels[i-1].bestStars > 0);
        }
        for (Pilot p : Pilot.values()) if (unlocked(p)) unlockedPilots.add(p);
        for (Submarine s : Submarine.values()) if (unlocked(s)) unlockedSubmarines.add(s);
        for (Weapon w : Weapon.values()) if (unlocked(w)) unlockedWeapons.add(w);
        if (!unlocked(selectedPilot)) selectedPilot = Pilot.KAIA;
        if (!unlocked(selectedSubmarine)) selectedSubmarine = Submarine.TIDE;
        if (!unlocked(selectedWeapon)) selectedWeapon = Weapon.PULSE_CANNON;
    }
    public boolean record(LevelResult result) {
        return record(result, 0, 0, 0);
    }
    boolean record(LevelResult result, int creditedSalvage, int creditedPlastic, int creditedEnemies) {
        if (result == null || !canPlay(result.levelId,result.difficulty)) return false;
        level(result.levelId).record(result);
        bestScore = Math.max(bestScore,result.score); bestStars = Math.max(bestStars,result.stars);
        totalSalvage = add(totalSalvage,Math.max(0,result.salvage - creditedSalvage));
        totalPlastic = add(totalPlastic,Math.max(0,result.plasticCollected - creditedPlastic));
        totalEnemies = add(totalEnemies,Math.max(0,result.enemiesDestroyed - creditedEnemies));
        if (result.completed && result.stars > 0) completedRuns = add(completedRuns,1);
        normalize(); evaluateAchievements(result);
        return true;
    }
    /** Keep the instance because audio and screens retain a reference to it. */
    void copyFrom(Profile p) {
        rewardRun = p.rewardRun; runResultRecorded = p.runResultRecorded; runCompleted = p.runCompleted;
        continueUsed = p.continueUsed; salvageDoubled = p.salvageDoubled;
        runSalvage = p.runSalvage; runPlastic = p.runPlastic; runEnemies = p.runEnemies;
        adSessions = p.adSessions; adCompletions = p.adCompletions; lastInterstitialAt = p.lastInterstitialAt;
        version = p.version; soundEnabled = p.soundEnabled; musicEnabled = p.musicEnabled; muted = p.muted;
        language = p.language;
        soundVolume = p.soundVolume; musicVolume = p.musicVolume; reducedMotion = p.reducedMotion;
        hapticEnabled = p.hapticEnabled; screenShakeEnabled = p.screenShakeEnabled;
        highContrastTelegraphs = p.highContrastTelegraphs; largeUi = p.largeUi; reducedFlashes = p.reducedFlashes;
        bestScore = p.bestScore; bestStars = p.bestStars; totalSalvage = p.totalSalvage; completedRuns = p.completedRuns;
        totalPlastic = p.totalPlastic; totalEnemies = p.totalEnemies;
        legacyMagnetLevel = p.legacyMagnetLevel; legacyRescuerProgress = p.legacyRescuerProgress; legacyExplorerProgress = p.legacyExplorerProgress;
        selectedPilot = p.selectedPilot; selectedSubmarine = p.selectedSubmarine; selectedWeapon = p.selectedWeapon;
        for (int i = 0; i < levels.length; i++) {
            LevelRecord to = levels[i], from = p.levels[i];
            to.unlocked = from.unlocked; to.bestStars = from.bestStars; to.bestScore = from.bestScore;
            to.bestCleanup = from.bestCleanup; to.bestRescue = from.bestRescue; to.completedDifficulties = from.completedDifficulties;
        }
        upgrades.clear(); upgrades.putAll(p.upgrades); achievements.clear(); achievements.putAll(p.achievements);
        unlockedAchievements.clear(); unlockedAchievements.addAll(p.unlockedAchievements);
        pendingNotifications.clear(); pendingNotifications.addAll(p.pendingNotifications);
        unlockedPilots.clear(); unlockedPilots.addAll(p.unlockedPilots);
        unlockedSubmarines.clear(); unlockedSubmarines.addAll(p.unlockedSubmarines);
        unlockedWeapons.clear(); unlockedWeapons.addAll(p.unlockedWeapons);
    }
    public void reset() { copyFrom(new Profile(content)); }
}
