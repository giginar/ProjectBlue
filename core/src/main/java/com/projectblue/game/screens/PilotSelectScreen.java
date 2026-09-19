package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Pilot;
import com.projectblue.game.ui.Palette;

public final class PilotSelectScreen extends StageMenuScreen {
    public PilotSelectScreen(ProjectBlueGame game) { super(game,"Pilot Select","Choose unlocked equipment for your next dive"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        for (Pilot choice : Pilot.values()) {
            var def = profile.content().pilot(choice.name());
            Table card = panel();
            card.add(label(def.displayName(),1.2f,Palette.AQUA)).row();
            card.add(label(def.description(),.95f,Palette.TEXT)).row();
            StringBuilder effects = new StringBuilder("Passive / ");
            for (int i=0;i<def.passives().size();i++) {
                var passive=def.passives().get(i);
                if (i>0) effects.append("  +  ");
                effects.append(statName(passive.stat())).append(" +").append(Math.round(passive.amount()*100)).append('%');
            }
            card.add(label(effects.toString(),.86f,Palette.GOLD)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(def.unlockCondition().description(),.9f,Palette.GOLD)).row();
            TextButton select = button("pilot-" + choice,profile.selectedPilot == choice ? "Selected" : unlocked ? "Select " + def.displayName() : "Locked",() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedPilot == choice);
            card.add(select).height(84).row();
        }
    }
    private static String statName(com.projectblue.game.config.ContentCatalog.Stat stat) {
        return switch (stat) {
            case HEALTH -> "Hull";
            case DAMAGE -> "Damage";
            case CLEANUP_POWER -> "Cleanup";
            case RESCUE_SPEED -> "Rescue speed";
            default -> stat.name().replace('_',' ');
        };
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
