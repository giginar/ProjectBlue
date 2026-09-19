package com.projectblue.game.platform;
public interface PlatformService {
    AdsService ads();
    ConsentService consent();
    AchievementService achievements();
    AnalyticsService analytics();
    String saveDirectory();
    default boolean developmentBuild() { return false; }
}
