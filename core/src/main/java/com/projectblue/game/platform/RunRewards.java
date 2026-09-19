package com.projectblue.game.platform;

import com.projectblue.game.save.SaveService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Lives as long as the in-memory dive. Saved claims and callback guards are independent. */
public final class RunRewards {
    public enum Reward { DOUBLE_SALVAGE, CONTINUE }
    private final SaveService saves;
    private final AdsService ads;
    private final String runId;
    private boolean busy, closed;
    public RunRewards(SaveService saves, AdsService ads) {
        this.saves = saves; this.ads = ads; runId = saves.beginRun();
    }
    public String runId() { return runId; }
    public boolean busy() { return busy; }
    public boolean eligible(Reward reward) {
        return !closed && !busy && (reward == Reward.CONTINUE ? saves.canContinue(runId) : saves.canDouble(runId));
    }
    public boolean available(Reward reward) { return eligible(reward) && ads.isRewardedAvailable(); }
    public void request(Reward reward, Consumer<Boolean> completion) {
        if (!available(reward) || !saves.save()) { completion.accept(false); return; }
        busy = true;
        AtomicBoolean once = new AtomicBoolean();
        Consumer<Boolean> callback = earned -> {
            if (!once.compareAndSet(false, true)) return;
            busy = false;
            if (closed) return;
            boolean applied = Boolean.TRUE.equals(earned) && (reward == Reward.CONTINUE
                ? saves.useContinue(runId) : saves.doubleSalvage(runId));
            completion.accept(applied);
        };
        try { ads.showRewarded(callback); } catch (RuntimeException failure) { callback.accept(false); }
    }
    public void close() { closed = true; saves.endRun(); }
}
