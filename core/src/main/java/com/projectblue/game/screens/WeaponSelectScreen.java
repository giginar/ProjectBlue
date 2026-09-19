package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Weapon;
import com.projectblue.game.ui.Palette;

public final class WeaponSelectScreen extends StageMenuScreen {
    public WeaponSelectScreen(ProjectBlueGame game) { super(game,"Weapon Select","Choose unlocked equipment for your next dive"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        for (Weapon choice : Weapon.values()) {
            var def = profile.content().weapon(choice.name());
            Table card = panel();
            card.add(label(def.displayName(),1.2f,Palette.AQUA)).row();
            card.add(label(def.description(),.95f,Palette.TEXT)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(def.unlockCondition().description(),.9f,Palette.GOLD)).row();
            TextButton select = button("weapon-" + choice,profile.selectedWeapon == choice ? "Selected" : unlocked ? "Select " + def.displayName() : "Locked",() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedWeapon == choice);
            card.add(select).height(84).row();
        }
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
