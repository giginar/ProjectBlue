package com.projectblue.game.platform;

import com.badlogic.gdx.files.FileHandle;
import com.projectblue.game.config.*;
import com.projectblue.game.logic.*;
import com.projectblue.game.save.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class AdsIntegrationTest {
    @TempDir Path directory;
    private SaveService open() { return new SaveService(new GdxSaveStore(new FileHandle(directory.toFile()))); }
    private LevelResult result(boolean completed, int salvage) {
        return new LevelResult(RunSpec.original(), completed, 3, 4, 1, salvage, completed ? 100 : 0);
    }
    private static final class FakeAds implements AdsService {
        boolean available = true;
        Consumer<Boolean> callback;
        int shown;
        public boolean isRewardedAvailable() { return available; }
        public void showRewarded(Consumer<Boolean> completion) { shown++; callback = completion; }
    }
    @Test void earnedThenDismissedDeliversOnceAndCloseBeforeEarnDeliversNothing() {
        List<Boolean> results = new ArrayList<>();
        RewardedCompletion earned = new RewardedCompletion(results::add);
        earned.earned(); earned.earned(); earned.dismiss(); earned.dismiss(); earned.fail();
        assertEquals(List.of(true), results);
        results.clear();
        RewardedCompletion interrupted = new RewardedCompletion(results::add);
        interrupted.dismiss(); interrupted.earned(); interrupted.dismiss();
        assertEquals(List.of(false), results);
        results.clear();
        RewardedCompletion failed = new RewardedCompletion(results::add);
        failed.earned(); failed.fail(); failed.dismiss();
        assertEquals(List.of(false), results);
    }
    @Test void duplicateRewardCannotDoubleSalvageTwiceOrReplayAfterRestart() {
        SaveService saves = open(); FakeAds ads = new FakeAds();
        RunRewards rewards = new RunRewards(saves, ads);
        assertEquals(SaveService.RecordResult.RECORDED, saves.recordRun(rewards.runId(), result(true, 50)));
        AtomicInteger successes = new AtomicInteger();
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> { if (ok) successes.incrementAndGet(); });
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> assertFalse(ok));
        assertEquals(1, ads.shown);
        ads.callback.accept(true); ads.callback.accept(true);
        assertEquals(1, successes.get()); assertEquals(100, saves.profile().totalSalvage);
        assertFalse(saves.doubleSalvage(rewards.runId()));
        SaveService reopened = open();
        assertEquals(100, reopened.profile().totalSalvage);
        assertFalse(reopened.doubleSalvage(rewards.runId()));
        assertEquals(SaveService.RecordResult.REJECTED, reopened.recordRun(rewards.runId(), result(true, 50)));
    }
    @Test void interruptedUnavailableOrStaleRewardDoesNotGrantAnything() {
        SaveService saves = open(); FakeAds ads = new FakeAds(); RunRewards rewards = new RunRewards(saves, ads);
        saves.recordRun(rewards.runId(), result(true, 25));
        ads.available = false;
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> assertFalse(ok));
        assertEquals(0, ads.shown);
        ads.available = true;
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> assertFalse(ok));
        ads.callback.accept(false); ads.callback.accept(true);
        assertEquals(25, saves.profile().totalSalvage);
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> fail("Closed result received a callback"));
        rewards.close(); ads.callback.accept(true);
        assertEquals(25, open().profile().totalSalvage);
    }
    @Test void continueOnlyOnceAndFinalResultCreditsOnlyNewSalvageAndCounters() {
        SaveService saves = open(); FakeAds ads = new FakeAds(); RunRewards rewards = new RunRewards(saves, ads);
        saves.recordRun(rewards.runId(), result(false, 25));
        rewards.request(RunRewards.Reward.CONTINUE, ok -> assertTrue(ok));
        ads.callback.accept(true); ads.callback.accept(true);
        assertFalse(saves.canContinue(rewards.runId()));
        assertFalse(open().useContinue(rewards.runId()));
        assertEquals(SaveService.RecordResult.RECORDED, saves.recordRun(rewards.runId(), result(true, 70)));
        assertEquals(70, saves.profile().totalSalvage);
        assertEquals(4, saves.profile().totalPlastic); assertEquals(3, saves.profile().totalEnemies);
        assertEquals(1, saves.profile().completedRuns); assertEquals(1, saves.profile().adCompletions);
        assertEquals(SaveService.RecordResult.REJECTED, saves.recordRun(rewards.runId(), result(true, 70)));
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> assertTrue(ok)); ads.callback.accept(true);
        assertEquals(140, open().profile().totalSalvage);
    }
    @Test void failedClaimWriteCannotGrantReward() {
        class FailingStore implements SaveStore {
            String text; boolean fail;
            public String read() { return text; }
            public void write(String value) throws IOException { if (fail) throw new IOException(); text = value; }
        }
        FailingStore store = new FailingStore(); SaveService saves = new SaveService(store);
        FakeAds ads = new FakeAds(); RunRewards rewards = new RunRewards(saves, ads);
        saves.recordRun(rewards.runId(), result(true, 15));
        rewards.request(RunRewards.Reward.DOUBLE_SALVAGE, ok -> assertFalse(ok));
        store.fail = true; ads.callback.accept(true);
        assertEquals(15, saves.profile().totalSalvage); assertFalse(saves.profile().salvageDoubled);
        assertEquals(15, new SaveService(store).profile().totalSalvage);
    }
    @Test void worldRestoresHullAndContinuesOnlyOnce() {
        GameWorld world = new GameWorld(RandomProvider.seeded(1));
        assertFalse(world.continueAfterFailure());
        world.player.health = 0; world.update(GameConfig.STEP, false, 0, 0);
        assertTrue(world.finished()); assertTrue(world.continueAfterFailure());
        assertFalse(world.finished()); assertNull(world.result()); assertTrue(world.invulnerable());
        assertEquals(world.player.maxHealth, world.player.health);
        world.player.health = 0; world.update(GameConfig.STEP, false, 0, 0);
        assertTrue(world.finished()); assertFalse(world.continueAfterFailure());
    }
    @Test void interstitialRequiresLaterSessionTimeAndThreeWinsAndPersistsReservation() {
        SaveService saves = open(); AtomicLong clock = new AtomicLong(1_000_000);
        InterstitialPolicy first = new InterstitialPolicy(saves, clock::get);
        saves.profile().adCompletions = 9; clock.addAndGet(900_000);
        assertFalse(first.eligible(true, true));
        InterstitialPolicy second = new InterstitialPolicy(saves, clock::get);
        assertFalse(second.eligible(true, true)); clock.addAndGet(InterstitialPolicy.INTERVAL_MS);
        assertFalse(second.eligible(false, true)); assertFalse(second.eligible(true, false));
        assertTrue(second.reserve(true, true)); assertFalse(second.reserve(true, true));
        saves.profile().adCompletions = 2; clock.addAndGet(InterstitialPolicy.INTERVAL_MS);
        assertFalse(second.eligible(true, true));
        saves.profile().adCompletions = 3; assertTrue(second.eligible(true, true));
        saves.save(); SaveService reopened = open();
        assertEquals(1_900_000 + InterstitialPolicy.INTERVAL_MS, reopened.profile().lastInterstitialAt);
        clock.set(1); assertFalse(new InterstitialPolicy(reopened, clock::get).eligible(true, true));
    }
    @Test void unknownFailedOrRevokedConsentNeverBecomesPermission() {
        ConsentGate gate = new ConsentGate(); assertFalse(gate.canRequestAds());
        gate.begin(); assertFalse(gate.canRequestAds());
        gate.update(false); assertFalse(gate.canRequestAds());
        // An offline update may use UMP's previous-session permission, never an app guess.
        gate.update(true); assertTrue(gate.canRequestAds());
        gate.formOpen(true); assertFalse(gate.canRequestAds());
        gate.update(false); gate.formOpen(false); assertFalse(gate.canRequestAds());
        gate.begin(); assertFalse(gate.canRequestAds());
    }
    @Test void desktopNoOpAndOfflineBootNeedNoNetworkOrConsent() {
        NoOpPlatformService platform = new NoOpPlatformService(directory.toString());
        assertFalse(platform.ads().isRewardedAvailable()); assertFalse(platform.ads().isInterstitialAvailable());
        assertFalse(platform.consent().canRequestAds()); assertFalse(platform.consent().isPrivacyOptionsRequired());
        AtomicInteger callbacks = new AtomicInteger();
        platform.ads().showRewarded(ok -> { assertFalse(ok); callbacks.incrementAndGet(); });
        platform.ads().showInterstitial(callbacks::incrementAndGet);
        platform.consent().requestConsent(callbacks::incrementAndGet);
        platform.consent().showPrivacyOptions(callbacks::incrementAndGet);
        assertEquals(4, callbacks.get());
        SaveService saves = open(); assertTrue(saves.profile().canPlay(1, Difficulty.NORMAL));
        GameWorld world = new GameWorld(RandomProvider.seeded(1));
        world.update(GameConfig.STEP, false, 0, 0); assertFalse(world.finished());
    }
}
