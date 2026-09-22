package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.save.Achievement;
import com.projectblue.game.ui.Palette;

public final class AchievementsScreen extends StageMenuScreen {
    public AchievementsScreen(ProjectBlueGame game) {
        super(game,game.i18n().text("achievements.title"),game.i18n().text("achievements.subtitle"));
        for (Achievement achievement : Achievement.values()) {
            var def = profile.content().achievement(achievement.name());
            Table card = panel();
            card.add(label(t("achievement." + achievement + ".name"),1.15f,Palette.AQUA)).row();
            card.add(label(t("achievement." + achievement + ".description"),.95f,Palette.TEXT)).row();
            card.add(label(profile.achievementUnlocked(achievement) ? t("achievements.completed")
                : profile.achievementProgress(achievement) + " / " + def.target(),1,Palette.GOLD)).row();
        }
    }
}
