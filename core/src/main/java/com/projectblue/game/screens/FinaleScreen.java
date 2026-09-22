package com.projectblue.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.CampaignConfig;
import com.projectblue.game.config.Difficulty;
import com.projectblue.game.config.MissionConfig;
import com.projectblue.game.save.Achievement;
import com.projectblue.game.save.FinaleSummary;
import com.projectblue.game.ui.Palette;
import java.util.Locale;

/** Persistent campaign finale and final results; all values come from the saved profile. */
public final class FinaleScreen extends StageMenuScreen {
    private static final Color RESTORED = new Color(.24f,.9f,.58f,1);

    public FinaleScreen(ProjectBlueGame game) {
        super(game,game.i18n().text("finale.title"),game.i18n().text("finale.subtitle"));
        FinaleSummary summary=new FinaleSummary(profile);
        Table ending=panel();
        ending.add(label(t("finale.complete"),1.25f,Palette.AQUA)).row();
        ending.add(label(t("finale.story"),.92f,Palette.TEXT)).row();

        Table map=panel();
        map.add(label(t("finale.map"),1.12f,Palette.AQUA)).row();
        for (CampaignConfig.Level level:CampaignConfig.DEFAULT.levels()) {
            boolean restored=profile.level(level.id()).bestStars>0;
            String text=String.format(Locale.ROOT,"%02d  %s\n%s  /  %s",level.id(),t("level."+level.id()+".name"),t("level."+level.id()+".region"),restored?t("levels.restored"):t("finale.needed"));
            map.add(label(text,.78f,restored?RESTORED:Palette.MUTED)).padBottom(7).row();
        }

        Table wildlife=panel();
        wildlife.add(label(t("finale.life"),1.12f,Palette.AQUA)).row();
        StringBuilder species=new StringBuilder();
        for (MissionConfig.CreatureKind kind:summary.rescuedSpecies()) {
            if (species.length()>0) species.append("  /  ");
            species.append(t("creature." + kind.name()));
        }
        wildlife.add(label(species.length()==0?t("finale.rescue_help"):species.toString(),.82f,RESTORED)).row();

        Table totals=panel();
        totals.add(label(t("finale.results"),1.12f,Palette.AQUA)).row();
        totals.add(label(t("finale.totals",summary.completedSectors(),CampaignConfig.LEVEL_COUNT,summary.totalStars(),CampaignConfig.LEVEL_COUNT*3,Math.round(summary.cleanupAverage()),Math.round(summary.rescueAverage()),summary.achievements(),Achievement.values().length),1,Palette.TEXT)).row();
        totals.add(label(profile.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE)
            ? t("finale.achievement") : t("finale.awaits"),.94f,Palette.GOLD)).row();

        if (game.router().rewards() != null) rewardActions(false);
        action("replay",t("finale.replay"),() -> game.router().requestDive(10,Difficulty.NORMAL));
        route("levels",t("finale.levels"),ScreenRouter.Route.LEVEL_SELECT);
        route("credits",t("finale.credits"),ScreenRouter.Route.CREDITS);
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton)stage.getRoot().findActor("back")).setText(t("result.menu"));
    }

    @Override protected float backdropRestoration() { return 1; }
    @Override protected MissionConfig.MissionType backdropType() { return MissionConfig.MissionType.NEREID_CORE; }
}
