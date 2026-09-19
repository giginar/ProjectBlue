package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;
import static com.projectblue.game.config.GameConfig.*;

/** Fixed-step current field. It uses only simulation time and coordinates, so devices produce identical motion. */
public final class VortexSystem {
    private final MissionConfig.Vortex config;
    private final float difficultyScale;
    private float time,phaseOffset,disrupted;
    private boolean escaping;

    public VortexSystem(MissionConfig.Vortex config,Difficulty difficulty) {
        if (config==null || difficulty==null) throw new IllegalArgumentException("Missing vortex tuning");
        this.config=config;
        difficultyScale=1+difficulty.ordinal()*.12f;
    }
    public void update(float dt) {
        if (dt<=0 || !Float.isFinite(dt)) return;
        time+=dt; disrupted=Math.max(0,disrupted-dt);
    }
    public void shiftDirection() { phaseOffset+=(float)(Math.PI*.5); }
    public void disrupt(float seconds) { disrupted=Math.max(disrupted,Math.max(0,seconds)); }
    public void beginEscape() { escaping=true; }
    public float forceX(float x,float y) {
        float dx=x-WIDTH*.5f,dy=y-HEIGHT*.46f;
        float length=Math.max(90,(float)Math.sqrt(dx*dx+dy*dy));
        float direction=direction();
        float global=(float)Math.cos(direction)*config.currentStrength()*.38f;
        float swirl=-dy/length*config.currentStrength();
        float value=(global+swirl)*difficultyScale*(disrupted>0?-1:1);
        if (escaping) value+=Math.signum(dx)*config.escapeBoost()*.18f;
        return value;
    }
    public float forceY(float x,float y) {
        float dx=x-WIDTH*.5f,dy=y-HEIGHT*.46f;
        float length=Math.max(90,(float)Math.sqrt(dx*dx+dy*dy));
        float direction=direction();
        float global=(float)Math.sin(direction)*config.currentStrength()*.28f;
        float swirl=dx/length*config.currentStrength();
        float value=(global+swirl)*difficultyScale*(disrupted>0?-1:1);
        return escaping?Math.max(value,config.escapeBoost()):value;
    }
    public float direction() {
        return time*(float)(Math.PI*2/config.directionSeconds())*(1+difficultyScale*.08f)+phaseOffset;
    }
    public float debrisAngle(float age,int index) { return age*config.debrisAngularSpeed()*difficultyScale+index*1.618f+phaseOffset; }
    public boolean escaping() { return escaping; }
}
