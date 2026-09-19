package com.projectblue.game.platform;

import java.util.function.Consumer;

/** SDK events may repeat. Only an earned event followed by dismissal grants a reward. */
public final class RewardedCompletion {
    private final Consumer<Boolean> completion;
    private boolean earned, finished;
    public RewardedCompletion(Consumer<Boolean> completion) { this.completion = completion; }
    public synchronized void earned() { if (!finished) earned = true; }
    public synchronized void dismiss() { finish(earned); }
    public synchronized void fail() { finish(false); }
    private void finish(boolean reward) {
        if (finished) return;
        finished = true;
        completion.accept(reward);
    }
}
