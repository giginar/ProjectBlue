package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig.*;
import java.util.EnumMap;
import static com.projectblue.game.config.GameConfig.*;

/** Small reusable movement and weapon components shared by every enemy definition. */
public final class EnemySystems {
    @FunctionalInterface private interface Move { void update(Entity e,float dt); }
    @FunctionalInterface private interface Fire { void fire(GameWorld w,Entity e,float speed); }
    private static final EnumMap<Movement,Move> MOVEMENT = new EnumMap<>(Movement.class);
    private static final EnumMap<WeaponPattern,Fire> WEAPONS = new EnumMap<>(WeaponPattern.class);
    static {
        MOVEMENT.put(Movement.DESCEND,(e,dt) -> e.y-=e.enemy.stats().speed()*dt);
        MOVEMENT.put(Movement.SWEEP,(e,dt) -> {
            e.y-=e.enemy.stats().speed()*dt;
            e.x=Rules.clamp(e.originX+(float)Math.sin(e.age*1.3f)*95,55,WIDTH-55);
        });
        MOVEMENT.put(Movement.HOLD,(e,dt) -> e.y=Math.max(680,e.y-e.enemy.stats().speed()*dt*2));
        MOVEMENT.put(Movement.HUNTER,(e,dt) -> {
            e.y-=e.enemy.stats().speed()*dt;
            e.x=Rules.clamp(e.x+(e.aimX-e.x)*Math.min(1,dt*2.4f),45,WIDTH-45);
        });
        MOVEMENT.put(Movement.BURROW,(e,dt) -> {
            e.y-=e.enemy.stats().speed()*dt*(e.age<1.1f?.35f:1);
            e.x=Rules.clamp(e.originX+(float)Math.sin(e.age*2.2f)*72,50,WIDTH-50);
        });
        WEAPONS.put(WeaponPattern.SINGLE,(w,e,speed) -> w.hostileProjectile(e.x,e.y-20,0,-speed,e.enemy.stats().damage(),0));
        WEAPONS.put(WeaponPattern.TRIPLE,(w,e,speed) -> {
            for (int i=-1;i<=1;i++) w.hostileProjectile(e.x,e.y-20,(float)Math.sin(i*.28f)*speed,
                -(float)Math.cos(i*.28f)*speed,e.enemy.stats().damage(),0);
        });
        WEAPONS.put(WeaponPattern.AIMED,(w,e,speed) -> aimed(w,e,speed,0));
        WEAPONS.put(WeaponPattern.NET,(w,e,speed) -> aimed(w,e,speed,w.mission().netSeconds));
        WEAPONS.put(WeaponPattern.NONE,(w,e,speed) -> { });
    }
    private EnemySystems() {}
    public static void update(GameWorld world,Entity e,float dt) {
        e.age+=dt; e.effectTime=Math.max(0,e.effectTime-dt); e.shieldTime=Math.max(0,e.shieldTime-dt);
        e.hiddenTime=Math.max(0,e.hiddenTime-dt); e.revealTime=Math.max(0,e.revealTime-dt);
        if (e.enemy.movement()==Movement.HUNTER) e.aimX=world.player.x;
        MOVEMENT.get(e.enemy.movement()).update(e,dt);
        if (e.y< -DESPAWN_MARGIN || e.x< -DESPAWN_MARGIN || e.x>WIDTH+DESPAWN_MARGIN || e.age>=e.enemy.stats().lifetime()) {
            e.active=false; return;
        }
        if (e.y>PLAY_MAX_Y) return;
        if (e.enemy.stats().repairAmount()>0) {
            e.repairTimer-=dt;
            if (e.repairTimer<=0) {
                e.repairTimer=2;
                for (int i=0;i<world.drones.capacity();i++) {
                    Entity ally=world.drones.at(i);
                    if (ally!=e && ally.active && Rules.overlaps(e.x,e.y,160,ally.x,ally.y,0)
                        && (e.enemy.ability()==EnemyAbility.SHIELD_CARRIER
                            || e.enemy.ability()==EnemyAbility.PIPELINE_GUARD || ally.health<ally.maxHealth)) {
                        if (e.enemy.ability()==EnemyAbility.SHIELD_CARRIER || e.enemy.ability()==EnemyAbility.PIPELINE_GUARD)
                            ally.shieldTime=Math.max(ally.shieldTime,1.6f);
                        else ally.health=Math.min(ally.maxHealth,ally.health+e.enemy.stats().repairAmount());
                        e.aimX=ally.x; e.aimY=ally.y; e.effectTime=.35f;
                    }
                }
            }
        }
        if (e.enemy.weapon()==WeaponPattern.NONE || e.hiddenTime>0) return;
        e.timer-=dt;
        if (!e.warned && e.timer<=.6f) { e.warned=true; e.aimX=world.player.x; e.aimY=world.player.y; }
        if (e.timer<=0) {
            WEAPONS.get(e.enemy.weapon()).fire(world,e,e.enemy.stats().bulletSpeed()*world.spec().tuning().bulletSpeed());
            e.timer=e.enemy.stats().shotInterval()/world.spec().tuning().fireRate(); e.warned=false;
        }
    }
    private static void aimed(GameWorld w,Entity e,float speed,float slow) {
        float dx=e.aimX-e.x,dy=e.aimY-e.y,length=Math.max(1,(float)Math.sqrt(dx*dx+dy*dy));
        w.hostileProjectile(e.x,e.y-20,dx/length*speed,dy/length*speed,e.enemy.stats().damage(),slow);
    }
    public static boolean armoredHit(Entity enemy,float impactX,float sourceY) {
        return enemy.enemy!=null && enemy.enemy.stats().frontArmor() && sourceY<enemy.y
            && Math.abs(impactX-enemy.x)<enemy.radius*.65f;
    }
}
