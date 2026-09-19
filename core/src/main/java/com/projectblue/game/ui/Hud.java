package com.projectblue.game.ui;

import com.projectblue.game.logic.GameWorld;
import com.projectblue.game.events.GameEvents;
import static com.projectblue.game.config.GameConfig.*;

/** UI observes simulation, it never awards points or applies damage. */
public final class Hud implements GameEvents.Listener {
    private final UiPainter ui;
    private final StringBuilder score = new StringBuilder(32), stats = new StringBuilder(64), time = new StringBuilder(24);
    private final StringBuilder health = new StringBuilder(24), ecology = new StringBuilder(32);
    private float refresh, noticeTime;
    private String notice = "DRAG TO MOVE / AUTO FIRE";
    private String mission;
    public Hud(UiPainter ui) { this.ui = ui; }
    public void update(float dt, GameWorld w) {
        noticeTime = Math.max(0, noticeTime - dt);
        refresh -= dt;
        if (refresh > 0) return;
        refresh = .1f; // Avoid formatting fresh strings in the render loop.
        score.setLength(0); score.append("SCORE ").append(w.score());
        stats.setLength(0); stats.append("CLEAN ").append(w.cleanedCount()).append('/').append(w.wasteTotal())
            .append("   RESCUE ").append(w.rescueCount()).append('/').append(w.turtleTotal());
        health.setLength(0); health.append("HULL ").append(w.player.health).append('/').append(w.player.maxHealth);
        if (w.spec().loadout().shieldCapacity() > 0) health.append(" S").append(w.shield());
        if (mission == null) mission = String.format(java.util.Locale.ROOT, "%02d / %s / %s", w.spec().level().id(), w.spec().level().name(), w.spec().difficulty());
        float limit = w.mission() == null ? LEVEL_SECONDS : w.mission().deadlineSeconds;
        int seconds = Math.max(0, (int) Math.ceil(limit - w.elapsed()));
        time.setLength(0); time.append(seconds / 60).append(':');
        if (seconds % 60 < 10) time.append('0');
        time.append(seconds % 60);
        ecology.setLength(0); ecology.append("REEF +").append(Math.round(w.restoration() * 100)).append("%   SALVAGE ").append(w.salvageCount());
    }
    public void draw(GameWorld w) {
        ui.beginShapes();
        ui.rect(0, 838, WIDTH, 122, Palette.INK);
        ui.bar(24, 868, 160, 6, (float) w.player.health / w.player.maxHealth, w.player.health > 30 ? Palette.AQUA : Palette.RED);
        ui.bar(0, 838, WIDTH, 4, w.progress(), Palette.AQUA);
        ui.button(450, 874, 64, 58, false);
        ui.rect(474, 892, 5, 22, Palette.TEXT); ui.rect(485, 892, 5, 22, Palette.TEXT);
        ui.rect(0, 0, WIDTH, 44, Palette.INK);
        ui.endShapes();
        ui.beginText();
        ui.text(mission, 24, 939, .65f, Palette.MUTED);
        ui.text(health, 24, 907, .72f, Palette.TEXT);
        ui.text(score, 218, 907, .67f, Palette.GOLD);
        ui.text(time, 349, 939, .66f, Palette.AQUA);
        ui.text(stats, 24, 862, .64f, Palette.TEXT);
        ui.centered(ecology, 29, .65f, Palette.AQUA);
        if (w.elapsed()<8 && w.mission()!=null) ui.centered(w.mission().introMessage,793,.62f,Palette.TEXT);
        else if (noticeTime > 0) ui.centered(notice, 793, .62f, Palette.TEXT);
        else if (w.midpointActive()) ui.centered(w.mission().midpointMessage,812,.58f,Palette.GOLD);
        else if (w.recovering()) ui.centered("HABITAT RECOVERING / COLOR RETURNING", 812, .58f, Palette.AQUA);
        else if (w.boss.active && w.mission() != null) {
            String objective = bossObjective(w);
            ui.centered(objective, 812, .58f, Palette.GOLD);
        } else if (w.boss.active) ui.centered("WARDEN / DISABLE BEFORE SURFACING", 812, .58f, Palette.GOLD);
        else if (w.slowed()) ui.centered("NETTED / THRUST REDUCED", 812, .58f, Palette.GOLD);
        else if (w.cleaning()) ui.centered("CLEANUP BEAM / THRUST REDUCED", 812, .58f, Palette.AQUA);
        ui.endText();
    }
    private static String bossObjective(GameWorld w) {
        return switch (w.mission().boss.kind()) {
            case SHORELINE_COMPACTOR -> switch (w.compactor().state()) {
                case PRESS_WARNING -> "PRESS ARMS OPENING / MOVE TO THE CENTER";
                case PRESS_ACTIVE -> "PRESS ARMS ACTIVE / STRIKE THE CORE";
                case PIPE_WARNING -> "DISCHARGE PIPES OPENING";
                case PIPES -> "CLOSE BOTH DISCHARGE PIPES";
                case CORE_EXPOSED -> "CORE EXPOSED / SHUT IT DOWN";
                default -> "SHORELINE COMPACTOR / WATCH THE TELEGRAPH";
            };
            case REEF_BREAKER -> switch (w.reefBreaker().state()) {
                case CUTTER_WARNING -> "CUTTER ARMS OPENING / MOVE TO CENTER";
                case CUTTER_SWEEP -> "CUTTER ARMS ACTIVE / STRIKE THE CORE";
                case GENERATOR_WARNING -> "SHIELD GENERATORS OPENING";
                case GENERATORS -> "DESTROY BOTH SHIELD GENERATORS";
                case CORAL_WARNING -> "CORAL STRIKE INCOMING / INTERCEPT";
                case CORE_EXPOSED -> "MAIN CORE EXPOSED / END EXTRACTION";
                default -> "REEF BREAKER / WATCH THE TELEGRAPH";
            };
            case GHOST_NET_HARVESTER -> switch (w.harvester().state()) {
                case NET_WARNING -> "LARGE NETS INCOMING";
                case NET_BARRAGE -> "CUT THE NETS / STRIKE THE CORE";
                case WALL_WARNING -> "NET WALLS SHIFTING / FIND THE CHANNEL";
                case NET_WALLS -> "STAY INSIDE THE MOVING SAFE CHANNEL";
                case GENERATOR_WARNING -> "NET GENERATORS OPENING";
                case GENERATORS -> "DISABLE BOTH NET GENERATORS";
                case CORE_EXPOSED -> "CENTER CORE EXPOSED / SHUT IT DOWN";
                default -> "GHOST NET HARVESTER / WATCH THE TELEGRAPH";
            };
            case URBAN_SALVAGER -> switch (w.urbanSalvager().state()) {
                case SCRAP_VOLLEY -> "URBAN SALVAGER / EVADE THE SCRAP VOLLEY";
                case ARMOR_WARNING -> "METAL ARMOR ASSEMBLING / STAND CLEAR";
                case ARMOR_PLATES -> "BREAK BOTH ARMOR PLATES";
                case CORE_EXPOSED -> "ENERGY CORE EXPOSED / SHUT IT DOWN";
                default -> "URBAN SALVAGER / WATCH THE RUINS";
            };
            case OIL_KRAKEN -> switch (w.oilKraken().state()) {
                case PIPE_ARMS -> "OIL KRAKEN / CLEAR THE SPRAY";
                case VALVE_WARNING -> "PRESSURE VALVES OPENING";
                case VALVES -> w.oilKraken().oilClearance()<.6f
                    ? "CLOSE BOTH VALVES / CLEAN THE BOSS OIL"
                    : "CLOSE BOTH PRESSURE VALVES";
                case CORE_EXPOSED -> "CORE VISIBLE / ATTACKS ACCELERATING";
                default -> "OIL KRAKEN / TRACK THE PIPE ARMS";
            };
        };
    }
    public void onEvent(GameEvents.Type type, float x, float y, int value) {
        switch (type) {
            case PLASTIC_COLLECTED -> { notice = "PLASTIC RECOVERED / WATER RESTORED"; noticeTime = 2; }
            case TURTLE_RESCUED -> { notice = "WILDLIFE FREE / LIFE RETURNS"; noticeTime = 3; }
            case PLAYER_HIT -> { notice = "HULL HIT / KEEP MOVING"; noticeTime = 1.5f; }
            default -> { }
        }
    }
}
