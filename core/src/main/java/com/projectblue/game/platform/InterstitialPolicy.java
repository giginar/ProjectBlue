package com.projectblue.game.platform;

import com.projectblue.game.save.SaveService;
import java.util.function.LongSupplier;

/** Product limits, not SDK defaults: no first-session ads, three wins, three minutes. */
public final class InterstitialPolicy {
    public static final int COMPLETIONS = 3;
    public static final long INTERVAL_MS = 180_000;
    private final SaveService saves;
    private final LongSupplier clock;
    private final long sessionStart;
    private final boolean firstSession;
    public InterstitialPolicy(SaveService saves, LongSupplier clock) {
        this.saves = saves; this.clock = clock; sessionStart = clock.getAsLong();
        firstSession = saves.profile().adSessions == 0;
        saves.profile().adSessions = Math.min(Integer.MAX_VALUE - 1, saves.profile().adSessions) + 1;
        saves.save();
    }
    public boolean eligible(boolean successfulResult, boolean adReady) {
        long now = clock.getAsLong();
        return successfulResult && adReady && !firstSession
            && saves.profile().adCompletions >= COMPLETIONS
            && now >= sessionStart && now - sessionStart >= INTERVAL_MS
            && now >= saves.profile().lastInterstitialAt
            && now - saves.profile().lastInterstitialAt >= INTERVAL_MS;
    }
    /** Reserve before showing; a failed show conservatively consumes the slot. */
    public boolean reserve(boolean successfulResult, boolean adReady) {
        return eligible(successfulResult, adReady) && saves.reserveInterstitial(clock.getAsLong());
    }
}
