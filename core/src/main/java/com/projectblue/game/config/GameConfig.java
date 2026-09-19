package com.projectblue.game.config;

/** Gameplay tuning lives here; distances are logical world units, time is seconds. */
public final class GameConfig {
    private GameConfig() {}
    public static final int WIDTH = 540, HEIGHT = 960;
    public static final float STEP = 1f / 60f, MAX_FRAME_TIME = .1f;
    public static final float LEVEL_SECONDS = 180f;
    public static final long LEVEL_SEED = 20260916L;
    public static final float PLAYER_RADIUS = 22f, PLAYER_START_Y = 170f;
    public static final float PLAYER_SPEED = 1100f, PLAY_MIN_Y = 52f, PLAY_MAX_Y = 824f;
    public static final int PLAYER_HEALTH = 100, DRONE_HEALTH = 30;
    public static final float SHOT_INTERVAL = .18f, PLAYER_BULLET_SPEED = 680f;
    public static final float ENEMY_SHOT_INTERVAL = 2.2f, ENEMY_BULLET_SPEED = 175f;
    public static final int PLAYER_DAMAGE = 10, ENEMY_DAMAGE = 8, CONTACT_DAMAGE = 15;
    public static final float INVULNERABILITY = .65f, BULLET_RADIUS = 5f;
    public static final float DRONE_RADIUS = 27f, DRONE_SPEED = 66f;
    public static final float DRONE_DRIFT = 16f, SHOT_OFFSET_Y = 34f;
    public static final float CLEAN_RADIUS = 112f, CLEAN_SECONDS = .42f, PLASTIC_SPEED = 43f;
    public static final float RESCUE_RADIUS = 96f, RESCUE_SECONDS = 1.5f, TURTLE_SPEED = 27f;
    public static final float PLASTIC_RADIUS = 12f, TURTLE_RADIUS = 24f;
    public static final float FREED_TURTLE_SPEED_X = 130f, FREED_TURTLE_SPEED_Y = 65f;
    public static final float SALVAGE_RADIUS = 125f, SALVAGE_SPEED = 380f, PICKUP_RADIUS = 25f;
    public static final int DRONE_COUNT = 40, PLASTIC_COUNT = 36, TURTLE_COUNT = 5;
    public static final float DRONE_FIRST = 2f, DRONE_INTERVAL = 4f;
    public static final float PLASTIC_FIRST = 1f, PLASTIC_INTERVAL = 4.4f;
    public static final float TURTLE_FIRST = 6f, TURTLE_INTERVAL = 32f;
    public static final float SPAWN_Y = 1000f, SPAWN_MARGIN = 65f;
    public static final float DESPAWN_MARGIN = 60f;
    public static final float CLEANUP_RESTORE_WEIGHT = .55f, RESCUE_RESTORE_WEIGHT = .45f;
    public static final int KILL_SCORE = 100, PLASTIC_SCORE = 40, RESCUE_SCORE = 300;
    public static final int SALVAGE_SCORE = 20, SALVAGE_PER_KILL = 5;
    public static final int COMPLETION_SCORE = 500, INTEGRITY_SCORE = 5;
    public static final float TWO_STAR_AVERAGE = 45f, THREE_STAR_AVERAGE = 75f;
    public static final float THREE_STAR_MIN_CATEGORY = 50f;
    public static final int BULLET_CAPACITY = 160, DRONE_CAPACITY = 16;
    public static final int ITEM_CAPACITY = 48, TURTLE_CAPACITY = 6, PARTICLE_CAPACITY = 192;
    public static final int PARTICLES_PER_BURST = 12;
    public static final float PARTICLE_LIFE = .7f, PARTICLE_SPEED = 105f;
    public static final int PROFILE_VERSION = 3;
    public static final int MAX_PROFILE_LENGTH = 16384;
    public static final float DEFAULT_SOUND_VOLUME = .45f, DEFAULT_MUSIC_VOLUME = .25f;
    public static final String SAVE_FILE = "profile.properties";
}
