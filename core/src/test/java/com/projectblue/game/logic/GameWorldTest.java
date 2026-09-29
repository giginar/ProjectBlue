package com.projectblue.game.logic;

import com.projectblue.game.events.GameEvents;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class GameWorldTest {
    private GameWorld world() { return new GameWorld(RandomProvider.seeded(LEVEL_SEED)); }
    private void steps(GameWorld w,int count) { for(int i=0;i<count;i++) w.update(STEP,false,0,0); }
    @Test void startsLowAndClampsDraggedSubmarine() {
        GameWorld w=world();
        assertEquals(PLAYER_START_Y,w.player.y);
        for(int i=0;i<120;i++) w.update(STEP,true,-10000,10000);
        assertEquals(PLAYER_RADIUS,w.player.x,.01f);
        assertEquals(PLAY_MAX_Y,w.player.y,.01f);
    }
    @Test void bulletsKillDroneAndDropSalvageExactlyOnce() {
        GameWorld w=world();
        Entity drone=w.drones.obtain();
        drone.x=w.player.x; drone.y=w.player.y+220; drone.health=DRONE_HEALTH;
        drone.radius=DRONE_RADIUS; drone.timer=999;
        steps(w,65);
        assertEquals(1,w.kills());
        assertTrue(w.score()>=KILL_SCORE);
        assertEquals(1,w.salvage.activeCount());
        steps(w,30);
        assertEquals(1,w.kills());
    }
    @Test void hostileShotsDamagePlayerButInvulnerabilityPreventsStackedHits() {
        GameWorld w=world();
        for(int i=0;i<2;i++) {
            Entity bullet=w.bullets.obtain(); bullet.x=w.player.x; bullet.y=w.player.y; bullet.radius=BULLET_RADIUS;
        }
        steps(w,1);
        assertEquals(PLAYER_HEALTH-ENEMY_DAMAGE,w.player.health);
    }
    @Test void cleaningRequiresProximityAndAwardsOnlyOnce() {
        GameWorld w=world();
        Entity plastic=w.plastics.obtain(); plastic.x=w.player.x; plastic.y=w.player.y;
        steps(w,20); assertEquals(0,w.plasticCount());
        steps(w,30); assertEquals(1,w.plasticCount());
        steps(w,20); assertEquals(1,w.plasticCount());
    }
    @Test void collectionProgressStartsAtZeroResetsPerEntityAndCompletesRewardsOnce() {
        GameWorld w=world(); AtomicInteger collectedEvents=new AtomicInteger();
        w.events.subscribe((type,x,y,value)->{ if (type==GameEvents.Type.PLASTIC_COLLECTED) collectedEvents.incrementAndGet(); });
        Entity first=w.plastics.obtain(), second=w.plastics.obtain();
        first.x=second.x=w.player.x; first.y=second.y=w.player.y;
        assertEquals(0,first.progress); assertEquals(0,second.progress);
        steps(w,20);
        assertTrue(first.progress>0); assertTrue(second.progress>0);
        assertEquals(0,w.plasticCount()); assertEquals(0,collectedEvents.get());
        float secondBefore=second.progress;
        first.x=0; steps(w,1);
        assertEquals(0,first.progress); assertTrue(second.progress>secondBefore);
        first.x=w.player.x; first.y=w.player.y;
        while (first.active || second.active) steps(w,1);
        assertEquals(2,w.plasticCount()); assertEquals(2,collectedEvents.get());
        int score=w.score(); steps(w,30);
        assertEquals(2,w.plasticCount()); assertEquals(2,collectedEvents.get()); assertEquals(score,w.score());
    }
    @Test void collectionProgressPausesAndClearsOnDeathOrScreenClose() {
        GameWorld paused=world(); Entity item=paused.plastics.obtain();
        item.x=paused.player.x; item.y=paused.player.y; steps(paused,10);
        float partial=item.progress;
        paused.update(0,false,0,0);
        assertEquals(partial,item.progress);
        paused.player.health=0; steps(paused,1);
        assertTrue(paused.finished()); assertEquals(0,item.progress);

        GameWorld closed=world(); Entity other=closed.plastics.obtain();
        other.x=closed.player.x; other.y=closed.player.y; steps(closed,10);
        assertTrue(other.progress>0); closed.close(); assertEquals(0,other.progress);
    }
    @Test void collectionDurationIsStableAcrossFixedAndHalfSteps() {
        assertEquals(COLLECTION_SECONDS,collectionTime(STEP),STEP);
        assertEquals(COLLECTION_SECONDS,collectionTime(STEP/2),STEP);
        assertEquals(collectionTime(STEP),collectionTime(STEP/2),STEP);
    }
    private float collectionTime(float dt) {
        GameWorld w=world(); Entity item=w.plastics.obtain();
        item.x=w.player.x; item.y=w.player.y;
        float time=0;
        while (item.active && time<2) { w.update(dt,false,0,0); time+=dt; }
        assertFalse(item.active); assertEquals(1,w.plasticCount());
        return time;
    }
    @Test void rescueRequiresContinuousStayAndLetsTurtleSwimFree() {
        GameWorld w=world();
        Entity turtle=w.turtles.obtain(); turtle.x=w.player.x; turtle.y=w.player.y;
        steps(w,60); assertEquals(0,w.rescueCount());
        turtle.x=0; steps(w,1); assertEquals(0,turtle.progress);
        turtle.x=w.player.x;
        steps(w,80); assertEquals(0,w.rescueCount());
        steps(w,20); assertEquals(1,w.rescueCount()); assertTrue(turtle.friendly);
        steps(w,100); assertEquals(1,w.rescueCount());
    }
    @Test void salvageMagnetCollectsValue() {
        GameWorld w=world();
        Entity item=w.salvage.obtain(); item.x=w.player.x+50; item.y=w.player.y; item.value=SALVAGE_PER_KILL;
        steps(w,20);
        assertEquals(SALVAGE_PER_KILL,w.salvageCount());
        assertFalse(item.active);
    }
    @Test void sameSeedReproducesSpawnsAndMovement() {
        GameWorld a=world(),b=world();
        steps(a,700); steps(b,700);
        assertEquals(a.player.health,b.player.health);
        for(int i=0;i<a.drones.capacity();i++) {
            assertEquals(a.drones.at(i).x,b.drones.at(i).x);
            assertEquals(a.drones.at(i).y,b.drones.at(i).y);
        }
    }
    @Test void authoredFinalMissionDoesNotAutoCompleteAtTheLegacyThreeMinuteMark() {
        GameWorld w=new GameWorld(RandomProvider.seeded(LEVEL_SEED),
            com.projectblue.game.config.RunSpec.create(10,com.projectblue.game.config.Difficulty.NORMAL,
                com.projectblue.game.config.Loadout.standard()));
        int[] finishedEvents={0};
        w.events.subscribe((type,x,y,value)->{if(type==GameEvents.Type.FINISHED)finishedEvents[0]++;});
        for(int i=0;i<11000 && !w.finished();i++) {
            w.player.health=w.player.maxHealth;
            float target=WIDTH/2f+(float)Math.sin(w.elapsed()*1.5f)*190;
            w.update(STEP,true,target,PLAYER_START_Y);
        }
        assertFalse(w.finished());
        assertTrue(w.elapsed()>LEVEL_SECONDS);
        assertEquals(0,finishedEvents[0]);
    }
    @Test void deathEndsLevelAndCannotGrantCompletionStars() {
        GameWorld w=world(); w.player.health=0; steps(w,1);
        assertTrue(w.finished()); assertFalse(w.result().completed); assertEquals(0,w.result().stars);
    }
    @Test void poolRecyclesTheSameObjectAndBoundsMemory() {
        EntityPool pool=new EntityPool(1); Entity first=pool.obtain();
        first.health=70; first.friendly=true;
        assertNull(pool.obtain());
        first.active=false;
        assertSame(first,pool.obtain()); assertEquals(0,first.health); assertFalse(first.friendly);
    }
}
