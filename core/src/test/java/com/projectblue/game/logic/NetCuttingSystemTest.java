package com.projectblue.game.logic;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetCuttingSystemTest {
    @Test void cutterOpensNetsFasterThanStandardFireAndProgressIsBounded() {
        float shots=0;
        for (int i=0;i<10;i++) shots=NetCuttingSystem.standardShot(shots);
        assertFalse(NetCuttingSystem.opened(shots));
        float cutter=NetCuttingSystem.cutter(0,1,1);
        assertTrue(NetCuttingSystem.opened(cutter));
        assertEquals(1,cutter);
        assertEquals(.5f,NetCuttingSystem.cutter(.5f,Float.NaN,1));
    }
}
