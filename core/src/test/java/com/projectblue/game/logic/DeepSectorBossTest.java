package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeepSectorBossTest {
    private static void advance(ResonanceEngine boss,ResonanceEngine.State target) {
        for (int i=0;i<1800 && boss.state()!=target;i++) boss.update(1f/60f);
        assertEquals(target,boss.state());
    }
    private static void advance(BorealisDrill boss,BorealisDrill.State target) {
        for (int i=0;i<1800 && boss.state()!=target;i++) boss.update(1f/60f);
        assertEquals(target,boss.state());
    }
    private static void advance(TheHarvester boss,TheHarvester.State target) {
        for (int i=0;i<1800 && boss.state()!=target;i++) boss.update(1f/60f);
        assertEquals(target,boss.state());
    }
    private static void advance(RecyclerLeviathan boss,RecyclerLeviathan.State target) {
        for (int i=0;i<1800 && boss.state()!=target;i++) boss.update(1f/60f);
        assertEquals(target,boss.state());
    }
    @Test void resonanceWeakPointsRequireAnActivePulseBeforeTheCoreReopens() {
        ResonanceEngine boss=new ResonanceEngine(MissionConfig.SILENT_REEF.boss,1,1);
        boss.start(); advance(boss,ResonanceEngine.State.SONAR_WAVES);
        boss.hitCore(Integer.MAX_VALUE); assertEquals(ResonanceEngine.State.DECOY_FIELD,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertEquals(ResonanceEngine.State.WEAK_POINTS,boss.state());
        assertEquals(0,boss.hitWeakPoint(true,100,false));
        boss.hitWeakPoint(true,Integer.MAX_VALUE,true); boss.hitWeakPoint(false,Integer.MAX_VALUE,true);
        assertEquals(ResonanceEngine.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated());
    }
    @Test void borealisThermalPhasePrecedesCoolingUnitsAndMainEngine() {
        BorealisDrill boss=new BorealisDrill(MissionConfig.FROZEN_DEPTHS.boss,1,1);
        boss.start(); advance(boss,BorealisDrill.State.DRILL_ARMS);
        boss.hitCore(Integer.MAX_VALUE); advance(boss,BorealisDrill.State.THERMAL_VENTS);
        assertFalse(boss.coreVulnerable());
        advance(boss,BorealisDrill.State.COOLING_UNITS);
        boss.hitUnit(true,Integer.MAX_VALUE); assertEquals(BorealisDrill.State.COOLING_UNITS,boss.state());
        boss.hitUnit(false,Integer.MAX_VALUE); assertEquals(BorealisDrill.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated());
    }
    @Test void harvesterArmorRemainsPoweredUntilBothEnergyStationsAreDisabled() {
        TheHarvester boss=new TheHarvester(MissionConfig.ABYSS_MINE.boss,1,1);
        boss.start(); advance(boss,TheHarvester.State.DRILL_ARMS);
        boss.hitCore(Integer.MAX_VALUE); advance(boss,TheHarvester.State.POWERED_ARMOR);
        assertFalse(boss.coreVulnerable()); assertEquals(0,boss.hitCore(100));
        assertTrue(boss.disableStation(true)); assertEquals(TheHarvester.State.POWERED_ARMOR,boss.state());
        assertTrue(boss.disableStation(false)); assertEquals(TheHarvester.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated());
    }
    @Test void recyclerUsesArmorCurrentReversalAndReturnedWasteBeforeOpeningItsCore() {
        RecyclerLeviathan boss=new RecyclerLeviathan(MissionConfig.PLASTIC_VORTEX.boss,5,1,1);
        boss.start(); advance(boss,RecyclerLeviathan.State.PLASTIC_ARMOR);
        boss.hitArmor(true,Integer.MAX_VALUE); assertEquals(RecyclerLeviathan.State.PLASTIC_ARMOR,boss.state());
        boss.hitArmor(false,Integer.MAX_VALUE); assertEquals(RecyclerLeviathan.State.CURRENT_REVERSAL,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertEquals(RecyclerLeviathan.State.WASTE_WEAPON,boss.state());
        for (int i=0;i<4;i++) assertTrue(boss.deliverWaste());
        assertFalse(boss.coreVulnerable());
        assertTrue(boss.deliverWaste()); assertEquals(RecyclerLeviathan.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated());
    }
}
