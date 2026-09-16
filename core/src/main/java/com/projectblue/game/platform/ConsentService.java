package com.projectblue.game.platform;
public interface ConsentService {
    boolean canRequestAds();
    void requestConsent(Runnable completion);
}

