package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import java.util.*;

/** Expanded once. Cursor dispatch is deterministic, allocation-free and cannot replay old events. */
public final class SpawnTimeline {
    public record Event(float time, MissionConfig.SpawnKind kind, MissionConfig.Enemy enemy, MissionConfig.Waste waste, float x) {}
    @FunctionalInterface public interface Sink { void spawn(Event event); }
    private final Event[] events;
    private int cursor;
    private boolean stopped;
    public SpawnTimeline(MissionConfig config, float density) {
        if (!Float.isFinite(density) || density < 1 || density > 2.5f) throw new IllegalArgumentException("Invalid spawn density");
        List<Event> expanded = new ArrayList<>();
        for (MissionConfig.Wave wave : config.waves()) {
            int count = Math.max(1,Math.round(wave.count()*density));
            for (int i=0;i<count;i++) expanded.add(new Event(wave.time()+i*wave.interval(),MissionConfig.SpawnKind.ENEMY,
                config.enemy(wave.enemy()),null,wave.x()+(i % wave.count())*wave.spacing()));
        }
        for (MissionConfig.Prop prop : config.props()) expanded.add(new Event(prop.time(),prop.kind(),null,
            prop.waste()==null ? null : config.waste(prop.waste()),prop.x()));
        expanded.sort(Comparator.comparingDouble(Event::time));
        events = expanded.toArray(new Event[0]);
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
