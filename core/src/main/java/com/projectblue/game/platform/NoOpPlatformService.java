package com.projectblue.game.platform;
import java.util.function.Consumer;
/** No SDK, networking, ad rewards, analytics collection or account requirements. */
public class NoOpPlatformService implements PlatformService {
    private final String saveDirectory;
    private final AdsService ads = new AdsService() {
        public boolean isRewardedAvailable() { return false; }
        public void showRewarded(Consumer<Boolean> completion) { completion.accept(false); }
    };
    private final ConsentService consent = new ConsentService() {
        public boolean canRequestAds() { return false; }
        public void requestConsent(Runnable completion) { completion.run(); }
    };
    private final AchievementService achievements = id -> {};
    private final AnalyticsService analytics = event -> {};
    public NoOpPlatformService(String saveDirectory) { this.saveDirectory = saveDirectory; }
    public AdsService ads() { return ads; }
    public ConsentService consent() { return consent; }
    public AchievementService achievements() { return achievements; }
    public AnalyticsService analytics() { return analytics; }
    public String saveDirectory() { return saveDirectory; }
}

