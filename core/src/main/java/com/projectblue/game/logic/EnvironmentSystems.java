package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;
import static com.projectblue.game.config.GameConfig.WIDTH;

/** Reusable route, visibility and interaction tuning for authored environment components. */
public final class EnvironmentSystems {
    public record Route(float left,float right) {}
    private EnvironmentSystems() {}

    public static Route route(MissionConfig.MissionType type,float elapsed,Difficulty difficulty) {
        if (type!=MissionConfig.MissionType.SUNKEN_CITY) return new Route(0,WIDTH);
        float center=WIDTH/2f+(float)Math.sin(elapsed*.19f)*72;
        float half=188-difficulty.ordinal()*9-(float)Math.sin(elapsed*.31f+1.2f)*24;
        return new Route(Rules.clamp(center-half,22,230),Rules.clamp(center+half,310,WIDTH-22));
    }

    public static float visibilityRadius(MissionConfig.MissionType type,Difficulty difficulty,float exposure) {
        float clear=760;
        float obscured=type==MissionConfig.MissionType.BLACK_TIDE ? 118 : 155;
        obscured-=difficulty.ordinal()*9;
        return clear+(obscured-clear)*Rules.clamp(exposure,0,1);
    }

    public static float interactionSeconds(MissionConfig.EnvironmentKind kind,Difficulty difficulty) {
        float base=switch (kind) {
            case OIL_FIELD -> 1.35f;
            case TOXIC_FIELD -> 1.1f;
            case VALVE -> 1.8f;
            default -> .8f;
        };
        return base*(1+difficulty.ordinal()*.12f);
    }

    public static float collapseWarning(Difficulty difficulty) { return Math.max(.65f,1.5f-difficulty.ordinal()*.2f); }
    public static float hiddenSeconds(Difficulty difficulty) { return 1.1f+difficulty.ordinal()*.35f; }
    public static float hazardDamageInterval(Difficulty difficulty) { return Math.max(.45f,1.05f-difficulty.ordinal()*.16f); }
}
