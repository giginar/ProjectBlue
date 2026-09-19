package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OilKrakenTest {
    private static void advance(OilKraken boss,OilKraken.State target,float oil) {
        for (int i=0;i<600 && boss.state()!=target;i++) boss.update(1f/60f,oil);
        assertEquals(target,boss.state());
    }
    @Test void valvesAndOilClearanceBothGateTheAcceleratingCorePhase() {
        OilKraken boss=new OilKraken(MissionConfig.BLACK_TIDE.boss,1,1);
        boss.start(); advance(boss,OilKraken.State.PIPE_ARMS,0);
        boss.hitCore(Integer.MAX_VALUE); advance(boss,OilKraken.State.VALVES,0);
        assertEquals(0,boss.hitCore(100));
        boss.closeValve(true); boss.closeValve(false); boss.update(.1f,.59f);
        assertEquals(OilKraken.State.VALVES,boss.state());
        boss.update(.1f,.6f); assertEquals(OilKraken.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated());
    }
}
