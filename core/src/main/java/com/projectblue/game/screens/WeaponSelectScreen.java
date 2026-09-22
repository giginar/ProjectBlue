package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Weapon;
import com.projectblue.game.ui.Palette;

public final class WeaponSelectScreen extends StageMenuScreen {
    public WeaponSelectScreen(ProjectBlueGame game) { super(game,game.i18n().text("select.weapon"),game.i18n().text("select.subtitle")); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        for (Weapon choice : Weapon.values()) {
            var def = profile.content().weapon(choice.name());
            Table card = panel();
            card.add(label(weaponName(choice),1.2f,Palette.AQUA)).row();
            card.add(label(weaponDescription(choice),.95f,Palette.TEXT)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(unlock(def.unlockCondition()),.9f,Palette.GOLD)).row();
            TextButton select = button("weapon-" + choice,profile.selectedWeapon == choice ? t("select.selected") : unlocked ? t("common.select", weaponName(choice)) : t("select.locked"),() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedWeapon == choice);
            card.add(select).height(84).row();
        }
    }
    private String weaponName(Weapon weapon) { return weapon == Weapon.SUPPORT_DRONE ? t("weapon.SUPPORT_DRONE.name") : contentName(weapon); }
    private String weaponDescription(Weapon weapon) { return weapon == Weapon.SUPPORT_DRONE ? t("weapon.SUPPORT_DRONE.description") : contentDescription(weapon); }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
