package com.projectblue.game.screens;

/** Pure responsive bounds shared by every Stage-based menu. */
public final class MenuLayoutPolicy {
    private MenuLayoutPolicy() {}
    public static float frameWidth(float worldWidth,float safeLeft,float safeRight) {
        float usable=Math.max(320,worldWidth-Math.max(0,safeLeft)-Math.max(0,safeRight));
        return Math.max(280,Math.min(640,usable-48));
    }
    public static boolean compact(float worldHeight) { return worldHeight<1050; }
    public static float actionHeight(boolean compact) { return compact?72:84; }
}
