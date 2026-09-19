package com.projectblue.game.logic;

import com.projectblue.game.config.MissionConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrbanSalvagerTest {
    private static void advance(UrbanSalvager boss,UrbanSalvager.State target) {
        for (int i=0;i<600 && boss.state()!=target;i++) boss.update(1f/60f);
        assertEquals(target,boss.state());
    }
    @Test void armorPlatesGateTheEnergyCore() {
        UrbanSalvager boss=new UrbanSalvager(MissionConfig.SUNKEN_CITY.boss,1,1);
        boss.start(); advance(boss,UrbanSalvager.State.SCRAP_VOLLEY);
        boss.hitCore(Integer.MAX_VALUE);
        assertEquals(UrbanSalvager.State.ARMOR_WARNING,boss.state()); assertFalse(boss.coreVulnerable());
        advance(boss,UrbanSalvager.State.ARMOR_PLATES);
        assertEquals(0,boss.hitCore(100));
        boss.hitPlate(true,Integer.MAX_VALUE); assertFalse(boss.coreVulnerable());
        boss.hitPlate(false,Integer.MAX_VALUE);
        assertEquals(UrbanSalvager.State.CORE_EXPOSED,boss.state());
        boss.hitCore(Integer.MAX_VALUE); assertTrue(boss.defeated());
    }
}
