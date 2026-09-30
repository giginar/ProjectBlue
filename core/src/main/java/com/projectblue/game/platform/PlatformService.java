package com.projectblue.game.platform;
public interface PlatformService {
    enum Haptic { LIGHT, HEAVY, DAMAGE, SUCCESS, BOSS }
    AdsService ads();
    ConsentService consent();
    AchievementService achievements();
    AnalyticsService analytics();
    String saveDirectory();
    default void haptic(Haptic kind) { }
    default boolean developmentBuild() { return false; }
}
