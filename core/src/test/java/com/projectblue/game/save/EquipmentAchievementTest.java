package com.projectblue.game.save;
import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.logic.*;
import com.projectblue.game.platform.AchievementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentAchievementTest {
    static final class Store implements SaveStore {
        String data; boolean fail;
        public String read() { return data; }
        public void write(String value) throws IOException {
            if (fail) throw new IOException("Full storage"); data = value;
        }
    }
    private LevelResult result(int id,Difficulty d,boolean completed,int kills,int plastic,int rescued,int damage) {
        RunSpec spec = RunSpec.create(id,d,Loadout.standard());
        return new LevelResult(spec,completed,kills,plastic,rescued,0,completed ? 100 : 0,damage);
    }
    private void openCampaign(Profile p) {
        for (int id = 1; id <= 10; id++) p.record(result(id,Difficulty.NORMAL,true,0,0,0,1));
    }
    @Test void lockedSelectionsAreRejectedAndEarnedChoicesSurviveReopen() {
        Store store = new Store(); SaveService saves = new SaveService(store); Profile p = saves.profile();
        assertFalse(saves.select(Submarine.MANTA)); assertFalse(saves.select(Pilot.NERI));
        assertFalse(saves.select(Weapon.SPREAD_CANNON));
        assertEquals(Weapon.PULSE_CANNON,p.selectedWeapon);
        openCampaign(p);
        for (Submarine sub : Submarine.values()) { assertTrue(saves.select(sub)); assertEquals(sub,new SaveService(store).profile().selectedSubmarine); }
        for (Pilot pilot : new Pilot[]{Pilot.KAIA,Pilot.ATLAS,Pilot.NERI}) {
            assertTrue(saves.select(pilot)); assertEquals(pilot,new SaveService(store).profile().selectedPilot);
        }
        p.record(result(1,Difficulty.NORMAL,false,25,0,0,1));
        assertTrue(saves.select(Pilot.ROOK)); assertEquals(Pilot.ROOK,new SaveService(store).profile().selectedPilot);
        for (Weapon weapon : Weapon.values()) { assertTrue(saves.select(weapon)); assertEquals(weapon,new SaveService(store).profile().selectedWeapon); }
    }
    @ParameterizedTest @EnumSource(Upgrade.class)
    void purchasesAreDurableCappedAndHaveGameplayEffects(Upgrade upgrade) {
        Store store = new Store(); SaveService saves = new SaveService(store); Profile p = saves.profile();
        assertEquals(SaveService.PurchaseResult.INSUFFICIENT_SALVAGE,saves.purchase(upgrade));
        assertEquals(0,p.totalSalvage); assertEquals(0,p.upgradeLevel(upgrade));
        p.totalSalvage = 10000; Loadout before = Loadout.from(p);
        int expected = 10000;
        for (int level = 0; level < 5; level++) {
            expected -= upgrade.cost(level);
            assertEquals(SaveService.PurchaseResult.PURCHASED,saves.purchase(upgrade));
            Profile disk = new SaveService(store).profile();
            assertEquals(expected,disk.totalSalvage); assertEquals(level+1,disk.upgradeLevel(upgrade));
        }
        assertEquals(SaveService.PurchaseResult.MAX_LEVEL,saves.purchase(upgrade));
        assertEquals(expected,p.totalSalvage);
        Loadout after = Loadout.from(p);
        switch (upgrade) {
            case HULL -> assertTrue(after.health() > before.health());
            case PRIMARY_WEAPON -> assertTrue(after.damage() > before.damage());
            case CLEANUP_BEAM -> assertTrue(after.cleanupSeconds() < before.cleanupSeconds());
            case SHIELD -> assertTrue(after.shieldCapacity() > before.shieldCapacity());
            case RESCUE_SYSTEM -> assertTrue(after.rescueSeconds() < before.rescueSeconds());
            case SUPPORT_DRONE -> assertTrue(after.droneDamage() > before.droneDamage());
        }
        assertTrue(p.achievementUnlocked(Achievement.FULLY_EQUIPPED));
    }
    @Test void failedPurchaseRollsBackMoneyLevelAndAchievementThenRetrySucceeds() {
        Store store = new Store(); SaveService saves = new SaveService(store); Profile p = saves.profile();
        p.totalSalvage = 1000;
        for (int i = 0; i < 4; i++) saves.purchase(Upgrade.HULL);
        int balance = p.totalSalvage; String committed = store.data;
        store.fail = true;
        assertEquals(SaveService.PurchaseResult.SAVE_FAILED,saves.purchase(Upgrade.HULL));
        assertSame(p,saves.profile()); assertEquals(balance,p.totalSalvage); assertEquals(4,p.upgradeLevel(Upgrade.HULL));
        assertFalse(p.achievementUnlocked(Achievement.FULLY_EQUIPPED)); assertEquals(committed,store.data);
        store.fail = false;
        assertEquals(SaveService.PurchaseResult.PURCHASED,saves.purchase(Upgrade.HULL));
        assertTrue(new SaveService(store).profile().achievementUnlocked(Achievement.FULLY_EQUIPPED));
    }
    @Test void failedSelectionKeepsOldEquipmentInMemoryAndOnDisk() {
        Store store = new Store(); SaveService saves = new SaveService(store); openCampaign(saves.profile()); saves.save();
        store.fail = true; assertFalse(saves.select(Submarine.LEVIATHAN));
        assertEquals(Submarine.TIDE,saves.profile().selectedSubmarine);
        assertEquals(Submarine.TIDE,new SaveService(store).profile().selectedSubmarine);
    }
    @Test void competingPurchasesCannotOverspendOnePurchaseBalance() throws Exception {
        Store store = new Store(); SaveService saves = new SaveService(store);
        saves.profile().totalSalvage = Upgrade.HULL.cost(0);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var a = pool.submit(() -> saves.purchase(Upgrade.HULL));
            var b = pool.submit(() -> saves.purchase(Upgrade.HULL));
            List<SaveService.PurchaseResult> outcomes = Arrays.asList(a.get(),b.get());
            assertEquals(1,Collections.frequency(outcomes,SaveService.PurchaseResult.PURCHASED));
            assertEquals(0,saves.profile().totalSalvage); assertEquals(1,saves.profile().upgradeLevel(Upgrade.HULL));
        } finally { pool.shutdownNow(); }
    }
    @Test void allRunAndCampaignAchievementsUseTheirActualCriteria() {
        Profile p = new Profile();
        p.record(result(1,Difficulty.NORMAL,false,0,28,4,1));
        assertFalse(p.achievementUnlocked(Achievement.CLEAN_START));
        assertFalse(p.achievementUnlocked(Achievement.FIRST_DIVE));
        p.record(result(1,Difficulty.NORMAL,false,0,29,4,1));
        assertTrue(p.achievementUnlocked(Achievement.CLEAN_START));
        assertFalse(p.achievementUnlocked(Achievement.PERFECT_BLUE));
        assertFalse(p.achievementUnlocked(Achievement.NO_ONE_LEFT_BEHIND));
        p.record(result(1,Difficulty.NORMAL,false,0,36,5,1));
        assertTrue(p.achievementUnlocked(Achievement.PERFECT_BLUE));
        assertTrue(p.achievementUnlocked(Achievement.NO_ONE_LEFT_BEHIND));
        assertFalse(p.achievementUnlocked(Achievement.UNTOUCHED));
        p.record(result(1,Difficulty.NORMAL,true,0,0,0,8));
        assertTrue(p.achievementUnlocked(Achievement.FIRST_DIVE));
        assertFalse(p.achievementUnlocked(Achievement.UNTOUCHED),"Restored hull does not erase damage history");
        p.record(result(1,Difficulty.NORMAL,true,0,0,0,0));
        assertTrue(p.achievementUnlocked(Achievement.UNTOUCHED));
        for (int id = 2; id <= 4; id++) p.record(result(id,Difficulty.NORMAL,true,0,0,0,1));
        assertTrue(p.achievementUnlocked(Achievement.DEEP_EXPLORER));
        assertFalse(p.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE));
        for (int id = 5; id <= 10; id++) p.record(result(id,Difficulty.NORMAL,true,0,0,0,1));
        assertTrue(p.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE));
        for (Difficulty d : new Difficulty[]{Difficulty.HARD,Difficulty.EXPERT,Difficulty.ABYSS}) p.record(result(1,d,true,0,0,0,1));
        assertTrue(p.achievementUnlocked(Achievement.NIGHTMARE_BELOW));
    }
    @Test void cumulativeCountersPersistSaturateAndUnlockOnlyOnce() {
        Store store = new Store(); SaveService saves = new SaveService(store); Profile p = saves.profile();
        for (int i = 0; i < 28; i++) p.record(result(1,Difficulty.NORMAL,false,4,36,0,1));
        assertEquals(100,p.achievementProgress(Achievement.RECYCLER_I));
        assertEquals(1000,p.achievementProgress(Achievement.RECYCLER_II));
        assertEquals(100,p.achievementProgress(Achievement.DRONE_HUNTER));
        saves.save();
        LocalAchievementService local = new LocalAchievementService(saves);
        Set<Achievement> notices = EnumSet.noneOf(Achievement.class);
        Achievement next;
        while ((next = local.nextNotification()) != null) assertTrue(notices.add(next));
        assertTrue(notices.containsAll(Arrays.asList(Achievement.RECYCLER_I,Achievement.RECYCLER_II,Achievement.DRONE_HUNTER)));
        p.record(result(1,Difficulty.NORMAL,false,4,36,0,1)); saves.save();
        assertNull(new LocalAchievementService(new SaveService(store)).nextNotification());
        assertEquals(1044,new SaveService(store).profile().totalPlastic);
        Map<String,Integer> progress = new HashMap<>(); Set<String> unlocked = new HashSet<>();
        local.synchronize(new AchievementService() {
            public void unlock(String id) { unlocked.add(id); }
            public void setProgress(String id,int value,int target) { progress.put(id,value); }
        });
        assertEquals(1000,progress.get("RECYCLER_II")); assertTrue(unlocked.contains("DRONE_HUNTER"));
        p.totalPlastic = p.totalEnemies = Integer.MAX_VALUE;
        p.record(result(1,Difficulty.NORMAL,false,4,36,0,1));
        assertEquals(Integer.MAX_VALUE,p.totalPlastic); assertEquals(Integer.MAX_VALUE,p.totalEnemies);
    }
    @Test void notificationsWaitForSuccessfulPersistence() {
        Store store = new Store(); SaveService saves = new SaveService(store);
        saves.profile().record(result(1,Difficulty.NORMAL,true,0,0,0,1));
        LocalAchievementService local = new LocalAchievementService(saves);
        store.fail = true; assertNull(local.nextNotification());
        store.fail = false; assertEquals(Achievement.FIRST_DIVE,local.nextNotification());
        assertNull(new LocalAchievementService(new SaveService(store)).nextNotification());
    }
}
