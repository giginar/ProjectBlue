package com.projectblue.game.screens;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuLayoutPolicyTest {
    @Test void compactStandardTallAndCutoutWidthsStayInsideSafeArea() {
        float[][] cases={{360,640,0,0},{540,960,0,0},{540,1200,0,0},{620,960,42,28},{720,900,70,70}};
        for(float[] c:cases) {
            float width=MenuLayoutPolicy.frameWidth(c[0],c[2],c[3]);
            assertTrue(width>=280&&width<=640);
            assertTrue(width<=Math.max(320,c[0]-c[2]-c[3]));
        }
        assertTrue(MenuLayoutPolicy.compact(960)); assertFalse(MenuLayoutPolicy.compact(1200));
        assertTrue(MenuLayoutPolicy.actionHeight(true)>=72); assertEquals(84,MenuLayoutPolicy.actionHeight(false));
    }
}
