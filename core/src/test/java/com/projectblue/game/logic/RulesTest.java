package com.projectblue.game.logic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class RulesTest {
    @ParameterizedTest @CsvSource({"100,8,92","5,8,0","100,-3,100","-5,10,0","0,8,0"})
    void damageNeverHealsOrGoesNegative(int hp,int hit,int expected) { assertEquals(expected, Rules.damage(hp,hit)); }
    @ParameterizedTest @CsvSource({"false,100,100,100,100,0","true,0,0,0,1,1","true,45,45,45,45,2",
        "true,75,75,75,75,3","true,100,100,0,100,2","true,44,44,44,44,1","true,100,100,50,50,3"})
    void starsRequireCompletionAndBalancedSuccess(boolean done,float a,float b,float c,float d,int stars) {
        assertEquals(stars,Rules.stars(done,a,b,c,d));
    }
    @Test void cleanupUsesEntireAuthoredLevel() {
        assertEquals(0,Rules.cleanup(0));
        assertEquals(50,Rules.cleanup(PLASTIC_COUNT/2));
        assertEquals(100,Rules.cleanup(PLASTIC_COUNT+10));
        assertEquals(0,Rules.cleanup(-1));
    }
    @Test void rescueAndEmptyTotalsAreSafe() {
        assertEquals(20,Rules.rescue(1));
        assertEquals(100,Rules.rescue(TURTLE_COUNT));
        assertEquals(0,Rules.percentage(0,0));
    }
    @Test void scoreCombinesIndependentActionsAndOnlyAwardsCompletionBonusOnSuccess() {
        assertEquals(1040,Rules.score(2,1,2,10,100,false));
        assertEquals(2040,Rules.score(2,1,2,10,100,true));
        assertEquals(0,Rules.score(-1,-1,-1,-1,0,false));
    }
    @Test void boundsIncludeBodyRadius() {
        assertEquals(22,Rules.boundCenter(-30,22,540));
        assertEquals(518,Rules.boundCenter(999,22,540));
        assertEquals(270,Rules.boundCenter(270,22,540));
        assertEquals(22,Rules.boundCenter(Float.NaN,22,540));
        assertThrows(IllegalArgumentException.class,()->Rules.boundCenter(5,6,10));
    }
}

