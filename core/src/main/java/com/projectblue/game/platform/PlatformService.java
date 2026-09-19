package com.projectblue.game.platform;
public interface PlatformService {
    enum Haptic { LIGHT, DAMAGE, SUCCESS }
    AdsService ads();
    ConsentService consent();
    AchievementService achievements();
    AnalyticsService analytics();
    String saveDirectory();
    default void haptic(Haptic kind) { }
    default boolean developmentBuild() { return false; }
}
