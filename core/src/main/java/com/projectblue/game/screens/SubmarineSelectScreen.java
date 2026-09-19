package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Submarine;
import com.projectblue.game.ui.Palette;

public final class SubmarineSelectScreen extends StageMenuScreen {
    public SubmarineSelectScreen(ProjectBlueGame game) { super(game,"Submarine Select","Choose unlocked equipment for your next dive"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        var current = profile.content().submarine(profile.selectedSubmarine.name());
        note("Each card compares its base frame with " + current.displayName() + ". Pilot and upgrade bonuses apply after selection.");
        for (Submarine choice : Submarine.values()) {
            var def = profile.content().submarine(choice.name());
            Table card = panel();
            card.add(label(def.displayName(),1.2f,Palette.AQUA)).row();
            card.add(label(def.description(),.95f,Palette.TEXT)).row();
            card.add(label("Hull " + def.baseHealth() + delta(def.baseHealth()-current.baseHealth())
                + " / Damage " + def.primaryDamage() + delta(def.primaryDamage()-current.primaryDamage())
                + "\nSpeed " + Math.round(def.movementSpeed()) + delta(Math.round(def.movementSpeed()-current.movementSpeed()))
                + " / Shield " + def.shieldCapacity() + delta(def.shieldCapacity()-current.shieldCapacity())
                + "\n" + def.specialAbility(),.85f,Palette.MUTED)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(def.unlockCondition().description(),.9f,Palette.GOLD)).row();
            TextButton select = button("submarine-" + choice,profile.selectedSubmarine == choice ? "Selected" : unlocked ? "Select " + def.displayName() : "Locked",() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedSubmarine == choice);
            card.add(select).height(84).row();
        }
    }
    private static String delta(int value) { return value == 0 ? " =" : value > 0 ? " +" + value : " " + value; }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
