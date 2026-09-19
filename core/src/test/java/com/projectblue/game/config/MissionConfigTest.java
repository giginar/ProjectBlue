package com.projectblue.game.config;

import com.projectblue.game.logic.SpawnTimeline;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MissionConfigTest {
    private String source() throws IOException {
        try (InputStream input=getClass().getResourceAsStream("/config/blue-coast.json")) {
            assertNotNull(input); return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    @Test void blueCoastDefinesTheRequiredMissionContent() {
        MissionConfig m=MissionConfig.BLUE_COAST;
        assertEquals("BLUE_COAST",m.id); assertEquals(300,m.durationSeconds);
        assertEquals(3,m.turtleCount); assertEquals(4,m.coralCount);
        assertEquals(Set.of("SCOUT","SWEEPER","NET_LAUNCHER","CARRIER","REPAIR","TURRET"),
            new HashSet<>(m.enemies().stream().map(MissionConfig.Enemy::id).toList()));
        assertTrue(m.wasteCount>=40); assertTrue(m.plasticCount>=20);
        assertEquals("Shoreline Compactor",m.boss.name());
    }
    @Test void duplicateIdsNegativeCountsAndInvalidFormationFailClearly() throws IOException {
        String json=source();
        IllegalArgumentException duplicate=assertThrows(IllegalArgumentException.class,
            () -> MissionConfig.parse(json.replaceFirst("\\\"id\\\": \\\"SWEEPER\\\"","\\\"id\\\": \\\"SCOUT\\\"")));
        assertTrue(duplicate.getMessage().contains("Duplicate enemy id"));
        assertThrows(IllegalArgumentException.class,
            () -> MissionConfig.parse(json.replaceFirst("\\\"count\\\": 3","\\\"count\\\": -1")));
        assertThrows(IllegalArgumentException.class,
            () -> MissionConfig.parse(json.replaceFirst("\\\"spacing\\\": 135","\\\"spacing\\\": 300")));
    }
    @Test void invalidJsonLogsAndUsesSafePlayableFallback() throws Exception {
        ByteArrayOutputStream log=new ByteArrayOutputStream();
        MissionConfig fallback=MissionConfig.readOrFallback(
            new ByteArrayInputStream("{ broken".getBytes(StandardCharsets.UTF_8)),new PrintStream(log));
        assertEquals("BLUE_COAST_SAFE",fallback.id);
        assertNotNull(fallback.enemy("SCOUT")); assertEquals(1,fallback.turtleCount);
        assertTrue(log.toString(StandardCharsets.UTF_8).contains("using safe fallback"));
    }
    @Test void expandedTimelineIsOrderedDeterministicAndCanBeStopped() {
        MissionConfig m=MissionConfig.BLUE_COAST;
        SpawnTimeline timeline=new SpawnTimeline(m,1);
        List<Float> times=new ArrayList<>();
        timeline.advance(m.boss.start(),event -> times.add(event.time()));
        assertEquals(timeline.size(),times.size());
        for(int i=1;i<times.size();i++) assertTrue(times.get(i)>=times.get(i-1));
        int dispatched=timeline.dispatched(); timeline.stop(); timeline.advance(Float.MAX_VALUE,event -> fail());
        assertTrue(timeline.stopped()); assertEquals(dispatched,timeline.dispatched());
    }
}
