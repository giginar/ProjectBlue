package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout;
import com.projectblue.game.ui.Palette;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class HangarScreen extends StageMenuScreen {
    public HangarScreen(ProjectBlueGame game) {
        super(game, game.i18n().text("hangar.title"), game.i18n().text("hangar.subtitle"));
        Loadout loadout = Loadout.from(profile);
        Table summary = panel();
        summary.add(label(contentName(profile.selectedSubmarine) + " / " + contentName(profile.selectedPilot), 1.1f, Palette.AQUA)).row();
        String weapon = profile.selectedWeapon == Loadout.Weapon.SUPPORT_DRONE ? t("weapon.SUPPORT_DRONE.name") : contentName(profile.selectedWeapon);
        summary.add(label(t("hangar.stats", loadout.health(), loadout.damage(), loadout.shieldCapacity(), weapon), 1, Palette.TEXT)).row();
        route("submarines", t("hangar.submarines"), ScreenRouter.Route.SUBMARINE_SELECT);
        route("pilots", t("hangar.pilots"), ScreenRouter.Route.PILOT_SELECT);
        route("weapons", t("hangar.weapons"), ScreenRouter.Route.WEAPON_SELECT);
        route("upgrades", t("hangar.upgrades"), ScreenRouter.Route.UPGRADES);
        note(t("hangar.help"));
    }
}
