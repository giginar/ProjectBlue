package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.ui.Palette;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class HangarScreen extends StageMenuScreen {
    public HangarScreen(ProjectBlueGame game) {
        super(game, "Hangar", "Crew and equipment / ready for the next dive");
        Loadout loadout = Loadout.from(profile);
        Table summary = panel();
        summary.add(label(profile.selectedSubmarine.definition().displayName() + " / " + profile.selectedPilot.definition().displayName(), 1.1f, Palette.AQUA)).row();
        summary.add(label("Hull " + loadout.health() + " / Damage " + loadout.damage()
            + "\nShield " + loadout.shieldCapacity() + " / " + loadout.weapon().displayName(), 1, Palette.TEXT)).row();
        route("submarines", "Submarine Select", ScreenRouter.Route.SUBMARINE_SELECT);
        route("pilots", "Pilot Select", ScreenRouter.Route.PILOT_SELECT);
        route("weapons", "Weapon Select", ScreenRouter.Route.WEAPON_SELECT);
        route("upgrades", "Upgrades", ScreenRouter.Route.UPGRADES);
        note("Salvage funds permanent equipment upgrades. Your selected crew and vessel apply to the next dive.");
    }
}
