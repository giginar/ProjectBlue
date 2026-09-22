package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.Loadout.Pilot;
import com.projectblue.game.ui.Palette;

public final class PilotSelectScreen extends StageMenuScreen {
    public PilotSelectScreen(ProjectBlueGame game) { super(game,game.i18n().text("select.pilot"),game.i18n().text("select.subtitle")); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        for (Pilot choice : Pilot.values()) {
            var def = profile.content().pilot(choice.name());
            Table card = panel();
            card.add(label(contentName(choice),1.2f,Palette.AQUA)).row();
            card.add(label(contentDescription(choice),.95f,Palette.TEXT)).row();
            StringBuilder effects = new StringBuilder();
            for (int i=0;i<def.passives().size();i++) {
                var passive=def.passives().get(i);
                if (i>0) effects.append("  +  ");
                effects.append(statName(passive.stat())).append(" +").append(Math.round(passive.amount()*100)).append('%');
            }
            card.add(label(t("select.passive", effects),.86f,Palette.GOLD)).row();
            boolean unlocked = profile.unlocked(choice);
            if (!unlocked) card.add(label(unlock(def.unlockCondition()),.9f,Palette.GOLD)).row();
            TextButton select = button("pilot-" + choice,profile.selectedPilot == choice ? t("select.selected") : unlocked ? t("common.select", contentName(choice)) : t("select.locked"),() -> {
                game.saves().select(choice); rebuild();
            });
            select.setDisabled(!unlocked || profile.selectedPilot == choice);
            card.add(select).height(84).row();
        }
    }
    private String statName(com.projectblue.game.config.ContentCatalog.Stat stat) { return t("stat." + stat.name()); }
    @Override protected void back() { game.router().request(ScreenRouter.Route.HANGAR); }
}
