package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class BlueCoastPacingTest {
    private record Timing(float firstEntity, float firstVisible, float firstEngageable,
                          float substantialFormation, float secondGroup, float thirdGroup,
                          float firstEnemyShot, float maximumEmptyGap) {}

    private static GameWorld world() {
        return new GameWorld(RandomProvider.seeded(20260916),
            RunSpec.create(1,Difficulty.NORMAL,Loadout.standard()));
    }

    private static boolean intersectsCombatBand(Entity enemy) {
        return enemy.active && enemy.x+enemy.radius>=0 && enemy.x-enemy.radius<=WIDTH
            && enemy.y-enemy.radius<=PLAY_MAX_Y && enemy.y+enemy.radius>=PLAY_MIN_Y;
    }

    private static int engageableEnemies(GameWorld world) {
        int count=0;
        for(int i=0;i<world.drones.capacity();i++)
            if(intersectsCombatBand(world.drones.at(i)) && world.enemyVisible(world.drones.at(i))) count++;
        return count;
    }

    private static boolean hasHostileShot(GameWorld world) {
        for(int i=0;i<world.bullets.capacity();i++) {
            Entity bullet=world.bullets.at(i);
            if(bullet.active&&!bullet.friendly) return true;
        }
        return false;
    }

    private static Timing measure(float dt) {
        GameWorld world=world();
        float firstEntity=Float.NaN,firstVisible=Float.NaN,firstEngageable=Float.NaN;
        float substantial=Float.NaN,secondGroup=Float.NaN,thirdGroup=Float.NaN;
        float firstShot=Float.NaN,emptyStart=0,maximumGap=0;
        boolean encounterStarted=false;
        while(world.elapsed()<30) {
            world.player.health=world.player.maxHealth;
            world.update(dt,false,0,0);
            if(Float.isNaN(firstEntity)&&world.spawnedDrones()>0) firstEntity=world.elapsed();
            int engageable=engageableEnemies(world);
            if(Float.isNaN(firstVisible)&&engageable>0) firstVisible=world.elapsed();
            if(Float.isNaN(firstEngageable)&&engageable>0) firstEngageable=world.elapsed();
            if(Float.isNaN(substantial)&&engageable>=4) substantial=world.elapsed();
            if(Float.isNaN(secondGroup)&&world.elapsed()>=7&&hasNewEngageableEnemy(world,7)) secondGroup=world.elapsed();
            if(Float.isNaN(thirdGroup)&&world.elapsed()>=12.5f&&hasNewEngageableEnemy(world,12.5f)) thirdGroup=world.elapsed();
            if(Float.isNaN(firstShot)&&hasHostileShot(world)) firstShot=world.elapsed();
            if(engageable>0) {
                if(encounterStarted) maximumGap=Math.max(maximumGap,world.elapsed()-emptyStart);
                encounterStarted=true; emptyStart=world.elapsed();
            }
        }
        return new Timing(firstEntity,firstVisible,firstEngageable,substantial,secondGroup,thirdGroup,firstShot,maximumGap);
    }

    private static boolean hasNewEngageableEnemy(GameWorld world,float waveStart) {
        for(int i=0;i<world.drones.capacity();i++) {
            Entity enemy=world.drones.at(i);
            if(intersectsCombatBand(enemy)&&world.enemyVisible(enemy)
                &&enemy.age<=world.elapsed()-waveStart+STEP*1.5f) return true;
        }
        return false;
    }

    @Test void blueCoastStartsWithVisibleFightableFormation() {
        List<SpawnTimeline.Event> dispatched=new ArrayList<>();
        new SpawnTimeline(MissionConfig.BLUE_COAST,CampaignConfig.DEFAULT.tuning(Difficulty.NORMAL).spawnDensity())
            .advance(2.5f,dispatched::add);
        List<SpawnTimeline.Event> enemies=dispatched.stream()
            .filter(e->e.kind()==MissionConfig.SpawnKind.ENEMY).toList();
        assertFalse(enemies.isEmpty());
        assertEquals(1.5f,enemies.get(0).time(),.001f);
        assertTrue(enemies.get(0).memberCount()>=5,"The opening is a formation, not a lone enemy");

        Timing timing=measure(STEP);
        assertEquals(1.5f,timing.firstEntity(),STEP+.001f);
        assertTrue(timing.firstVisible()<=2.5f,"First visible enemy: "+timing.firstVisible());
        assertTrue(timing.firstEngageable()<=2.5f,"First engageable enemy: "+timing.firstEngageable());
        assertTrue(timing.substantialFormation()<=3.1f,"Four visible members: "+timing.substantialFormation());
        assertTrue(timing.secondGroup()>=7&&timing.secondGroup()<=8);
        assertTrue(timing.thirdGroup()>=12.5f&&timing.thirdGroup()<=14);
        assertTrue(timing.firstEnemyShot()>timing.firstVisible()+.6f,"Entry must telegraph before enemy fire");
    }

    @Test void openingUsesVariedGroupsWithoutLongEmptyIntervals() {
        MissionConfig mission=MissionConfig.BLUE_COAST;
        List<MissionConfig.Wave> opening=mission.waves().stream().filter(w->w.time()<=30).toList();
        assertEquals(List.of(1.5f,7f,12.5f,18f,23.5f,29f),opening.stream().map(MissionConfig.Wave::time).toList());
        assertTrue(opening.stream().limit(3).map(MissionConfig.Wave::formation).distinct().count()>=2);
        for(int i=1;i<opening.size();i++) assertTrue(opening.get(i).time()-opening.get(i-1).time()<=5.5f);
        assertTrue(measure(STEP).maximumEmptyGap()<=5.5f);
    }

    @Test void formationDelaysAndEntryPositionsRemainBounded() {
        for(int level=1;level<CampaignConfig.LEVEL_COUNT;level++) {
            MissionConfig mission=MissionConfig.forLevel(level);
            List<SpawnTimeline.Event> events=new ArrayList<>();
            new SpawnTimeline(mission,1).advance(30,events::add);
            List<SpawnTimeline.Event> enemies=events.stream()
                .filter(e->e.kind()==MissionConfig.SpawnKind.ENEMY).toList();
            assertFalse(enemies.isEmpty(),"Sector "+level);
            SpawnTimeline.Event first=enemies.get(0);
            assertTrue(first.time()<=2.5f,"Sector "+level+" dispatch");
            assertTrue(first.y()-first.enemy().stats().radius()<=PLAY_MAX_Y+12.001f,"Sector "+level+" entry distance");
            assertTrue(first.x()+first.enemy().stats().radius()>=-35
                && first.x()-first.enemy().stats().radius()<=WIDTH+35,"Sector "+level+" horizontal entry");
            float firstFormationEnd=enemies.stream().filter(e->e.formation()==first.formation()
                    && e.memberCount()==first.memberCount()).limit(first.memberCount())
                .map(SpawnTimeline.Event::time).max(Float::compare).orElseThrow();
            MissionConfig.Wave firstWave=mission.waves().get(0);
            float memberStep=firstWave.formation()==MissionConfig.Formation.CHAIN
                ?firstWave.interval():Math.min(.45f,firstWave.interval());
            assertTrue(firstFormationEnd-first.time()<=Math.max(memberStep*(first.memberCount()-1),.001f)+.001f,
                "Sector "+level+" formation delay must stay per-member, not cumulative");

            GameWorld world=new GameWorld(RandomProvider.seeded(level),
                RunSpec.create(level,Difficulty.NORMAL,Loadout.standard()));
            float visibleAt=Float.NaN;
            while(world.elapsed()<3.5f&&Float.isNaN(visibleAt)) {
                world.player.health=world.player.maxHealth; world.update(STEP,false,0,0);
                if(engageableEnemies(world)>0) visibleAt=world.elapsed();
            }
            assertTrue(Float.isFinite(visibleAt),"Sector "+level+" visible threat by 3.5 seconds");
        }
        assertEquals(12f,MissionConfig.NEREID_CORE.waves().get(0).time(),.001f);
    }

    @Test void fixedStepSubdivisionKeepsVisibleTimingStableAndRestartStartsAtZero() {
        Timing sixtyHz=measure(STEP),oneTwentyHz=measure(STEP/2);
        assertEquals(sixtyHz.firstVisible(),oneTwentyHz.firstVisible(),STEP+.001f);
        assertEquals(sixtyHz.substantialFormation(),oneTwentyHz.substantialFormation(),STEP*2+.001f);
        GameWorld restarted=world();
        while(restarted.elapsed()<8) restarted.update(STEP,false,0,0);
        restarted=world();
        assertEquals(0,restarted.elapsed(),.001f); assertEquals(0,restarted.spawnedDrones());
        while(restarted.elapsed()<1.4f) restarted.update(STEP,false,0,0);
        assertEquals(0,restarted.spawnedDrones());
    }

    @Test void healthRewardsAndDifficultyScalingAreUnchanged() {
        MissionConfig.Enemy scout=MissionConfig.BLUE_COAST.enemy("SCOUT");
        assertEquals(24,scout.stats().health()); assertEquals(100,scout.reward().score());
        assertEquals(5,scout.reward().salvage());
        int previous=0;
        for(Difficulty difficulty:Difficulty.values()) {
            int health=Math.round(scout.stats().health()*CampaignConfig.DEFAULT.tuning(difficulty).health());
            assertTrue(health>previous); previous=health;
        }
    }

    @Test void reportsPacingSmokeEvidence() {
        Timing timing=measure(STEP);
        System.out.printf(Locale.ROOT,
            "Blue Coast pacing: entity=%.3f visible=%.3f engageable=%.3f substantial=%.3f second=%.3f third=%.3f shot=%.3f maxGap=%.3f%n",
            timing.firstEntity(),timing.firstVisible(),timing.firstEngageable(),timing.substantialFormation(),
            timing.secondGroup(),timing.thirdGroup(),timing.firstEnemyShot(),timing.maximumEmptyGap());
    }
}
