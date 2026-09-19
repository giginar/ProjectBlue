package com.projectblue.game.platform;
import java.util.function.Consumer;
public interface AdsService {
    boolean isRewardedAvailable();
    /** Callback is on the game thread; false grants nothing. */
    void showRewarded(Consumer<Boolean> completion);
    default boolean isInterstitialAvailable() { return false; }
    /** Always finishes on the game thread, including unavailable/failed ads. */
    default void showInterstitial(Runnable completion) { completion.run(); }
    /** Called at menu boundaries, never blocks navigation. */
    default void preload() {}
    default boolean isSupported() { return false; }
}
