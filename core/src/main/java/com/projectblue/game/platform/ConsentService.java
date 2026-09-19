package com.projectblue.game.platform;
public interface ConsentService {
    boolean canRequestAds();
    void requestConsent(Runnable completion);
    default boolean isPrivacyOptionsRequired() { return false; }
    default void showPrivacyOptions(Runnable completion) { completion.run(); }
}
