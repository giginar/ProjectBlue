package com.projectblue.game.platform;
import java.util.function.Consumer;
public interface AdsService {
    boolean isRewardedAvailable();
    /** Callback is on the game thread; false grants nothing. */
    void showRewarded(Consumer<Boolean> completion);
}

