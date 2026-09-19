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
        super(game,"Guardian of the Blue","NEREID is silent / the ocean is alive");
        FinaleSummary summary=new FinaleSummary(profile);
        Table ending=panel();
        ending.add(label("Ocean recovery complete",1.25f,Palette.AQUA)).row();
        ending.add(label("Cold black corridors open into living blue water. The animals rescued across the campaign return as the ten regions recover.",.92f,Palette.TEXT)).row();

        Table map=panel();
        map.add(label("World recovery map",1.12f,Palette.AQUA)).row();
        for (CampaignConfig.Level level:CampaignConfig.DEFAULT.levels()) {
            boolean restored=profile.level(level.id()).bestStars>0;
            String text=String.format(Locale.ROOT,"%02d  %s\n%s  /  %s",level.id(),level.name(),level.region(),restored?"RESTORED":"RECOVERY NEEDED");
            map.add(label(text,.78f,restored?RESTORED:Palette.MUTED)).padBottom(7).row();
        }

        Table wildlife=panel();
        wildlife.add(label("Life returns",1.12f,Palette.AQUA)).row();
        StringBuilder species=new StringBuilder();
        for (MissionConfig.CreatureKind kind:summary.rescuedSpecies()) {
            if (species.length()>0) species.append("  /  ");
            species.append(kind.name().replace('_',' '));
        }
        wildlife.add(label(species.length()==0?"Complete rescues to bring wildlife home":species.toString(),.82f,RESTORED)).row();

        Table totals=panel();
        totals.add(label("Final results",1.12f,Palette.AQUA)).row();
        totals.add(label("Regions restored  "+summary.completedSectors()+" / "+CampaignConfig.LEVEL_COUNT
            +"\nTotal stars  "+summary.totalStars()+" / "+(CampaignConfig.LEVEL_COUNT*3)
            +"\nCleanup average  "+Math.round(summary.cleanupAverage())+"%"
            +"\nRescue average  "+Math.round(summary.rescueAverage())+"%"
            +"\nAchievements  "+summary.achievements()+" / "+Achievement.values().length,1,Palette.TEXT)).row();
        totals.add(label(profile.achievementUnlocked(Achievement.GUARDIAN_OF_THE_BLUE)
            ? "Achievement unlocked / Guardian of the Blue" : "Guardian of the Blue awaits",.94f,Palette.GOLD)).row();

        action("replay","Replay NEREID Core / Normal",() -> game.router().requestDive(10,Difficulty.NORMAL));
        route("levels","Replay any sector",ScreenRouter.Route.LEVEL_SELECT);
        route("credits","Continue to credits",ScreenRouter.Route.CREDITS);
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton)stage.getRoot().findActor("back")).setText("MAIN MENU");
    }

    @Override protected float backdropRestoration() { return 1; }
    @Override protected MissionConfig.MissionType backdropType() { return MissionConfig.MissionType.NEREID_CORE; }
}
