package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Submarine;
import com.projectblue.game.ui.Palette;

public final class SubmarineSelectScreen extends StageMenuScreen {
    public SubmarineSelectScreen(ProjectBlueGame game) { super(game,game.i18n().text("select.submarine"),game.i18n().text("select.subtitle")); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        var current = profile.content().submarine(profile.selectedSubmarine.name());
        note(t("select.compare", contentName(profile.selectedSubmarine)));
        for (Submarine choice : Submarine.values()) {
            var def = profile.content().submarine(choice.name());
            Table card = panel();
            card.add(label(contentName(choice),1.2f,Palette.AQUA)).row();
            card.add(label(contentDescription(choice),.95f,Palette.TEXT)).row();
            card.add(label(t("select.stats", def.baseHealth(), delta(def.baseHealth()-current.baseHealth()), def.primaryDamage(), delta(def.primaryDamage()-current.primaryDamage()), Math.round(def.movementSpeed()), delta(Math.round(def.movementSpeed()-current.movementSpeed())), def.shieldCapacity(), delta(def.shieldCapacity()-current.shieldCapacity()), t("ability." + choice)),.85f,Palette.MUTED)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(unlock(def.unlockCondition()),.9f,Palette.GOLD)).row();
            TextButton select = button("submarine-" + choice,profile.selectedSubmarine == choice ? t("select.selected") : unlocked ? t("common.select", contentName(choice)) : t("select.locked"),() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedSubmarine == choice);
            card.add(select).height(84).row();
        }
    }
    private static String delta(int value) { return value == 0 ? " =" : value > 0 ? " +" + value : " " + value; }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
