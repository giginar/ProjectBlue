package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import java.util.*;

/** Expanded once. Cursor dispatch is deterministic, allocation-free and cannot replay old events. */
public final class SpawnTimeline {
    public record Event(float time, MissionConfig.SpawnKind kind, MissionConfig.Enemy enemy,
                        MissionConfig.Waste waste, MissionConfig.Creature creature,
                        MissionConfig.EnvironmentKind environment, float x, float y, float horizontalSpeed,
                        MissionConfig.Formation formation, int memberIndex, int memberCount) {}
    @FunctionalInterface public interface Sink { void spawn(Event event); }
    private final Event[] events;
    private int cursor;
    private boolean stopped;
    public SpawnTimeline(MissionConfig config, float density) {
        if (!Float.isFinite(density) || density < 1 || density > 2.5f) throw new IllegalArgumentException("Invalid spawn density");
        List<Event> expanded = new ArrayList<>();
        for (MissionConfig.Wave wave : config.waves()) {
            int count = Math.max(1,Math.round(wave.count()*density));
            for (int i=0;i<count;i++) expanded.add(enemyEvent(config,wave,i,count));
        }
        for (MissionConfig.Prop prop : config.props()) expanded.add(new Event(prop.time(),prop.kind(),null,
            prop.waste()==null ? null : config.waste(prop.waste()),
            prop.creature()==null ? null : config.creature(prop.creature()),prop.environment(),prop.x(),0,0,
            MissionConfig.Formation.CHAIN,0,1));
        expanded.sort(Comparator.comparingDouble(Event::time));
        events = expanded.toArray(new Event[0]);
    }
    private static Event enemyEvent(MissionConfig config,MissionConfig.Wave wave,int index,int count) {
        float time=wave.time()+index*wave.interval(),x=wave.x()+(index%wave.count())*wave.spacing();
        float y=com.projectblue.game.config.GameConfig.SPAWN_Y,horizontalSpeed=0;
        switch (wave.formation()) {
            case FLEET_LEFT -> { time=wave.time()+index*Math.min(.35f,wave.interval()); x=575; y-=index*34; horizontalSpeed=-85; }
            case FLEET_RIGHT -> { time=wave.time()+index*Math.min(.35f,wave.interval()); x=-35; y-=index*34; horizontalSpeed=85; }
            case V -> {
                int wing=(index+1)/2*(index%2==1?-1:1);
                time=wave.time()+index*Math.min(.4f,wave.interval()); x=wave.x()+wing*Math.max(52,Math.abs(wave.spacing())); y+=Math.abs(wing)*28;
            }
            case CROSS -> { time=wave.time()+index*Math.min(.3f,wave.interval()); boolean left=index%2==0; x=left?-35:575; y+=(index/2)*28; horizontalSpeed=left?78:-78; }
            case WIDE -> { time=wave.time()+index*Math.min(.25f,wave.interval()); x=count==1?270:70+index*400f/(count-1); }
            case ESCORT -> { time=wave.time()+index*Math.min(.35f,wave.interval()); int slot=index==0?0:(index%2==1?-(index+1)/2:(index/2)); x=wave.x()+slot*62; y+=Math.abs(slot)*22; }
            case PRIORITY -> { time=wave.time()+index*Math.min(.45f,wave.interval()); x=index==0?wave.x():wave.x()+(index%2==1?-1:1)*Math.min(180,55*((index+1)/2)); }
            case BARRAGE_GAP -> {
                time=wave.time()+index*Math.min(.18f,wave.interval());
                int leftCount=count/2;
                float leftEdge=55,rightEdge=485,gapLeft=wave.safeCorridorX()-wave.safeCorridorWidth()/2;
                float gapRight=wave.safeCorridorX()+wave.safeCorridorWidth()/2;
                if (index<leftCount) x=leftCount==1?(leftEdge+gapLeft)/2:leftEdge+index*(gapLeft-leftEdge)/(leftCount-1);
                else { int right=index-leftCount,rightCount=count-leftCount; x=rightCount==1?(gapRight+rightEdge)/2:gapRight+right*(rightEdge-gapRight)/(rightCount-1); }
            }
            case CHAIN -> { }
        }
        return new Event(time,MissionConfig.SpawnKind.ENEMY,config.enemy(wave.enemy()),null,null,null,x,y,horizontalSpeed,
            wave.formation(),index,count);
    }
    public void advance(float elapsed, Sink sink) {
        if (stopped || !Float.isFinite(elapsed)) return;
        while (cursor < events.length && events[cursor].time() <= elapsed + .0001f) sink.spawn(events[cursor++]);
    }
    public void stop() { stopped=true; }
    public int dispatched() { return cursor; }
    public int size() { return events.length; }
    public boolean stopped() { return stopped; }
}
