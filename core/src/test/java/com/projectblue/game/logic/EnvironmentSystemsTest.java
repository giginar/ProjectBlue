package com.projectblue.game.logic;

import com.projectblue.game.config.*;
import org.junit.jupiter.api.Test;
import static com.projectblue.game.config.GameConfig.*;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentSystemsTest {
    @Test void sunkenRouteChangesAndTightensAcrossDifficulties() {
        var normal=EnvironmentSystems.route(MissionConfig.MissionType.SUNKEN_CITY,40,Difficulty.NORMAL);
        var later=EnvironmentSystems.route(MissionConfig.MissionType.SUNKEN_CITY,90,Difficulty.NORMAL);
        var abyss=EnvironmentSystems.route(MissionConfig.MissionType.SUNKEN_CITY,40,Difficulty.ABYSS);
        assertNotEquals(normal.left(),later.left());
        assertTrue(abyss.right()-abyss.left()<normal.right()-normal.left());
        assertEquals(0,EnvironmentSystems.route(MissionConfig.MissionType.BLUE_COAST,40,Difficulty.NORMAL).left());
        assertEquals(WIDTH,EnvironmentSystems.route(MissionConfig.MissionType.BLUE_COAST,40,Difficulty.NORMAL).right());
    }

    @Test void visibilityCleaningAndTelegraphsScaleByDifficulty() {
        assertTrue(EnvironmentSystems.visibilityRadius(MissionConfig.MissionType.BLACK_TIDE,Difficulty.ABYSS,1)
            < EnvironmentSystems.visibilityRadius(MissionConfig.MissionType.BLACK_TIDE,Difficulty.NORMAL,1));
        assertTrue(EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.OIL_FIELD,Difficulty.ABYSS)
            > EnvironmentSystems.interactionSeconds(MissionConfig.EnvironmentKind.OIL_FIELD,Difficulty.NORMAL));
        assertTrue(EnvironmentSystems.collapseWarning(Difficulty.ABYSS)<EnvironmentSystems.collapseWarning(Difficulty.NORMAL));
        assertTrue(EnvironmentSystems.hiddenSeconds(Difficulty.ABYSS)>EnvironmentSystems.hiddenSeconds(Difficulty.NORMAL));
    }

    @Test void barrelDamageCreatesToxicFieldAndOilCanBeCleanedByTheSharedBeam() {
        GameWorld city=new GameWorld(RandomProvider.seeded(7),RunSpec.create(4,Difficulty.NORMAL,Loadout.standard()));
        Entity barrel=city.environments.obtain(); barrel.environment=MissionConfig.EnvironmentKind.CHEMICAL_BARREL;
        barrel.x=270; barrel.y=500; barrel.radius=24; barrel.health=barrel.maxHealth=30;
        city.playerProjectile(barrel.x,barrel.y,0,0,10,0); city.update(STEP,false,0,0);
        assertEquals(1,city.hazards.activeCount());
        assertEquals(MissionConfig.EnvironmentKind.TOXIC_FIELD,city.hazards.at(0).environment);

        GameWorld tide=new GameWorld(RandomProvider.seeded(8),RunSpec.create(5,Difficulty.NORMAL,Loadout.standard()));
        Entity oil=tide.hazards.obtain(); oil.environment=MissionConfig.EnvironmentKind.OIL_FIELD;
        oil.x=tide.player.x; oil.y=tide.player.y; oil.radius=70;
        for (int i=0;i<150 && oil.active;i++) { tide.player.health=tide.player.maxHealth; tide.update(STEP,false,0,0); }
        assertFalse(oil.active); assertEquals(1,tide.oilCleaned());
    }
}
