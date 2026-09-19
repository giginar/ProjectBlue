package com.projectblue.game.logic;

import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SonarThermalSystemTest {
    @Test void sonarSpendsRegeneratesAndPickupRechargesWithoutOverusePenalty() {
        SonarSystem sonar=new SonarSystem(MissionConfig.SILENT_REEF.sonar,Difficulty.NORMAL);
        assertTrue(sonar.activate());
        assertEquals(72,sonar.energy(),.001f); assertTrue(sonar.revealing());
        assertTrue(sonar.activate()); assertTrue(sonar.activate()); assertFalse(sonar.activate());
        float depleted=sonar.energy(); sonar.update(2);
        assertEquals(depleted+9,sonar.energy(),.001f);
        sonar.recharge(1000); assertEquals(sonar.capacity(),sonar.energy(),.001f);
        sonar.update(4); assertFalse(sonar.revealing());
    }
    @Test void difficultyChangesSonarResourcePressureBeyondEnemyHealth() {
        SonarSystem normal=new SonarSystem(MissionConfig.SILENT_REEF.sonar,Difficulty.NORMAL);
        SonarSystem abyss=new SonarSystem(MissionConfig.SILENT_REEF.sonar,Difficulty.ABYSS);
        assertTrue(abyss.cost()>normal.cost());
        normal.activate(); abyss.activate(); normal.update(1); abyss.update(1);
        assertTrue(normal.energy()>abyss.energy());
    }
    @Test void thermalExposureBuildsProgressiveDamageAndColdZonesRecoverHeat() {
        ThermalSystem thermal=new ThermalSystem(MissionConfig.FROZEN_DEPTHS.thermal,Difficulty.NORMAL);
        for (int i=0;i<360;i++) thermal.update(1f/60f,1,false);
        assertTrue(thermal.dangerous());
        int first=thermal.consumeDamage(); assertTrue(first>=3);
        assertEquals(0,thermal.consumeDamage());
        float hot=thermal.heat();
        for (int i=0;i<120;i++) thermal.update(1f/60f,0,true);
        assertTrue(thermal.heat()<hot); assertFalse(thermal.dangerous());
    }
    @Test void higherDifficultyRaisesThermalPressure() {
        ThermalSystem normal=new ThermalSystem(MissionConfig.FROZEN_DEPTHS.thermal,Difficulty.NORMAL);
        ThermalSystem abyss=new ThermalSystem(MissionConfig.FROZEN_DEPTHS.thermal,Difficulty.ABYSS);
        normal.update(2,1,false); abyss.update(2,1,false);
        assertTrue(abyss.heat()>normal.heat());
    }
}
