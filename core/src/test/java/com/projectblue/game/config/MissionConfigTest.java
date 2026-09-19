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
    @Test void sectorsTwoAndThreeDefineTheirDistinctReusableMissionSystems() {
        MissionConfig coral=MissionConfig.CORAL_GARDENS, nets=MissionConfig.GHOST_NETS;
        assertTrue(CampaignConfig.isAvailable(2)); assertTrue(CampaignConfig.isAvailable(3));
        assertSame(coral,MissionConfig.forLevel(2)); assertSame(nets,MissionConfig.forLevel(3));
        assertEquals(MissionConfig.BossKind.REEF_BREAKER,coral.boss.kind());
        assertNotNull(coral.enemy("CORAL_CUTTER")); assertNotNull(coral.enemy("SHIELD_CARRIER"));
        assertNotNull(coral.enemy("BURROW_DRONE")); assertEquals(6,coral.creatureCount);
        assertEquals(MissionConfig.BossKind.GHOST_NET_HARVESTER,nets.boss.kind());
        assertNotNull(nets.enemy("NET_LAUNCHER")); assertNotNull(nets.enemy("NET_RECYCLER"));
        assertNotNull(nets.enemy("FAST_HUNTER_DRONE")); assertTrue(nets.currentStrength>coral.currentStrength);
        assertTrue(coral.durationSeconds>=300 && coral.durationSeconds<=420);
        assertTrue(nets.durationSeconds>=300 && nets.durationSeconds<=420);
    }
    @Test void sectorsFourAndFiveAreAuthoredFiveToSevenMinuteMissions() {
        MissionConfig city=MissionConfig.SUNKEN_CITY,tide=MissionConfig.BLACK_TIDE;
        assertSame(city,MissionConfig.forLevel(4)); assertSame(tide,MissionConfig.forLevel(5));
        assertEquals(MissionConfig.BossKind.URBAN_SALVAGER,city.boss.kind());
        assertEquals(MissionConfig.BossKind.OIL_KRAKEN,tide.boss.kind());
        assertEquals(Set.of("CHEMICAL_BOMBER","RUIN_TURRET","SALVAGE_MECH","AMBUSH_DRONE"),
            new HashSet<>(city.enemies().stream().map(MissionConfig.Enemy::id).toList()));
        assertEquals(Set.of("OIL_SPREADER","IGNITION_DRONE","PRESSURE_TANKER","PIPELINE_GUARD"),
            new HashSet<>(tide.enemies().stream().map(MissionConfig.Enemy::id).toList()));
        for (MissionConfig mission:List.of(city,tide)) {
            assertTrue(mission.durationSeconds>=300 && mission.durationSeconds<=420);
            assertTrue(mission.creatureCount>0); assertTrue(mission.mechanicCount>0);
            assertTrue(CampaignConfig.isAvailable(mission==city?4:5));
        }
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
    @Test void authoredTimelinesScaleEnemyDensityWithoutDuplicatingEnvironmentProps() {
        for (MissionConfig mission : List.of(MissionConfig.CORAL_GARDENS,MissionConfig.GHOST_NETS,
            MissionConfig.SUNKEN_CITY,MissionConfig.BLACK_TIDE)) {
            SpawnTimeline normal=new SpawnTimeline(mission,1), abyss=new SpawnTimeline(mission,2);
            assertTrue(abyss.size()>normal.size());
            int props=mission.props().size();
            assertEquals(mission.waves().stream().mapToInt(MissionConfig.Wave::count).sum()+props,normal.size());
            List<SpawnTimeline.Event> events=new ArrayList<>();
            abyss.advance(mission.boss.start(),events::add);
            assertEquals(abyss.size(),events.size());
            for (int i=1;i<events.size();i++) assertTrue(events.get(i).time()>=events.get(i-1).time());
            assertTrue(events.stream().anyMatch(e -> e.kind()==MissionConfig.SpawnKind.CREATURE));
        }
    }
}
