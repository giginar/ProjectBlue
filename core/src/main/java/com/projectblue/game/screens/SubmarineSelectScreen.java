package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Submarine;
import com.projectblue.game.ui.Palette;

public final class SubmarineSelectScreen extends StageMenuScreen {
    public SubmarineSelectScreen(ProjectBlueGame game) { super(game,"Submarine Select","Choose unlocked equipment for your next dive"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        for (Submarine choice : Submarine.values()) {
            var def = profile.content().submarine(choice.name());
            Table card = panel();
            card.add(label(def.displayName(),1.2f,Palette.AQUA)).row();
            card.add(label(def.description(),.95f,Palette.TEXT)).row();
            card.add(label("Hull " + def.baseHealth() + " / Damage " + def.primaryDamage() + "\n" + def.specialAbility(),.85f,Palette.MUTED)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(def.unlockCondition().description(),.9f,Palette.GOLD)).row();
            TextButton select = button("submarine-" + choice,profile.selectedSubmarine == choice ? "Selected" : unlocked ? "Select " + def.displayName() : "Locked",() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedSubmarine == choice);
            card.add(select).height(84).row();
        }
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
