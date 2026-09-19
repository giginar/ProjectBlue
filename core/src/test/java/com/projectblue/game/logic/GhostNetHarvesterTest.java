package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GhostNetHarvesterTest {
    private static void advance(GhostNetHarvester boss,GhostNetHarvester.State state) {
        for (int i=0;i<600 && boss.state()!=state;i++) boss.update(1f/60f);
        assertEquals(state,boss.state());
    }
    @Test void netWallsMoveBeforeGeneratorsOpenTheFinalCore() {
        GhostNetHarvester boss=new GhostNetHarvester(MissionConfig.GHOST_NETS.boss,1,1);
        boss.start(); advance(boss,GhostNetHarvester.State.NET_BARRAGE);
        boss.hitCore(Integer.MAX_VALUE);
        assertEquals(GhostNetHarvester.State.WALL_WARNING,boss.state());
        advance(boss,GhostNetHarvester.State.NET_WALLS);
        float lane=boss.safeLaneX(); boss.update(.5f);
        assertNotEquals(lane,boss.safeLaneX());
        boss.hitCore(Integer.MAX_VALUE);
        advance(boss,GhostNetHarvester.State.GENERATORS);
        assertFalse(boss.coreVulnerable());
        boss.hitGenerator(true,Integer.MAX_VALUE); boss.hitGenerator(false,Integer.MAX_VALUE);
        assertEquals(GhostNetHarvester.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE);
        assertTrue(boss.defeated());
    }
}
