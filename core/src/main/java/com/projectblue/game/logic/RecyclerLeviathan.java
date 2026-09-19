package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;

/** Plastic Vortex boss: break recycled armor, survive current reversals, then return collected waste. */
public final class RecyclerLeviathan {
    public enum State { DORMANT, ARRIVAL, PLASTIC_ARMOR, CURRENT_REVERSAL, WASTE_WEAPON, CORE_EXPOSED, DEFEATED }
    private final MissionConfig.Boss config;
    private final int maxHealth,maxArmorHealth,wasteRequired;
    private final float cadence;
    private State state=State.DORMANT;
    private int health,leftArmor,rightArmor,wasteDelivered;
    private float stateTime,attackTimer;
    private boolean volley,trash,currentShift;

    public RecyclerLeviathan(MissionConfig.Boss config,int wasteRequired,float healthScale,float cadence) {
        this.config=config; this.wasteRequired=wasteRequired; this.cadence=cadence;
        maxHealth=Math.round(config.coreHealth()*healthScale);
        maxArmorHealth=Math.round(config.pipeHealth()*healthScale);
        health=maxHealth;
    }
    public void start() { if (state==State.DORMANT) enter(State.ARRIVAL); }
    public void update(float dt) {
        if (state==State.DORMANT || state==State.DEFEATED || dt<=0) return;
        stateTime+=dt;
        if (state==State.ARRIVAL && stateTime>=config.arrivalSeconds()) enter(State.PLASTIC_ARMOR);
        if (state==State.PLASTIC_ARMOR || state==State.CURRENT_REVERSAL || state==State.WASTE_WEAPON || state==State.CORE_EXPOSED) {
            attackTimer-=dt;
            if (attackTimer<=0) {
                volley=true; trash=state==State.PLASTIC_ARMOR || state==State.WASTE_WEAPON;
                currentShift=state==State.CURRENT_REVERSAL;
                attackTimer=config.attackInterval()/(cadence*(state==State.CORE_EXPOSED?1.4f:1));
            }
        }
    }
    public int hitArmor(boolean left,int damage) {
        if (state!=State.PLASTIC_ARMOR || damage<=0) return 0;
        int before=left?leftArmor:rightArmor;
        if (left) leftArmor=Rules.damage(leftArmor,damage); else rightArmor=Rules.damage(rightArmor,damage);
        if (leftArmor==0 && rightArmor==0) enter(State.CURRENT_REVERSAL);
        return before-(left?leftArmor:rightArmor);
    }
    public int hitCore(int damage) {
        if (!coreVulnerable() || damage<=0) return 0;
        int before=health;
        if (state==State.CURRENT_REVERSAL) {
            int threshold=Math.max(1,Math.round(maxHealth*.55f));
            health=Math.max(threshold,Rules.damage(health,damage));
            if (health<=threshold) enter(State.WASTE_WEAPON);
        } else {
            health=Rules.damage(health,damage);
            if (health==0) enter(State.DEFEATED);
        }
        return before-health;
    }
    public boolean deliverWaste() {
        if (state!=State.WASTE_WEAPON) return false;
        wasteDelivered=Math.min(wasteRequired,wasteDelivered+1);
        if (wasteDelivered>=wasteRequired) enter(State.CORE_EXPOSED);
        return true;
    }
    private void enter(State next) {
        state=next; stateTime=0; attackTimer=.35f;
        if (next==State.PLASTIC_ARMOR) leftArmor=rightArmor=maxArmorHealth;
    }
    public boolean consumeVolley() { boolean value=volley; volley=false; return value; }
    public boolean consumeTrash() { boolean value=trash; trash=false; return value; }
    public boolean consumeCurrentShift() { boolean value=currentShift; currentShift=false; return value; }
    public boolean coreVulnerable() { return state==State.CURRENT_REVERSAL || state==State.CORE_EXPOSED; }
    public boolean defeated() { return state==State.DEFEATED; }
    public int phase() { return state==State.PLASTIC_ARMOR?1:state==State.CURRENT_REVERSAL?2:
        state==State.WASTE_WEAPON||state==State.CORE_EXPOSED?3:0; }
    public State state() { return state; }
    public float stateTime() { return stateTime; }
    public int health() { return health; }
    public int maxHealth() { return maxHealth; }
    public int armorHealth(boolean left) { return left?leftArmor:rightArmor; }
    public int maxArmorHealth() { return maxArmorHealth; }
    public int wasteDelivered() { return wasteDelivered; }
    public int wasteRequired() { return wasteRequired; }
}
