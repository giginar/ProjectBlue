package com.projectblue.game.screens;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.save.Achievement;
import com.projectblue.game.ui.Palette;

public final class AchievementsScreen extends StageMenuScreen {
    public AchievementsScreen(ProjectBlueGame game) {
        super(game,"Achievements","Your recovery log / saved on this device");
        for (Achievement achievement : Achievement.values()) {
            var def = profile.content().achievement(achievement.name());
            Table card = panel();
            card.add(label(def.displayName(),1.15f,Palette.AQUA)).row();
            card.add(label(def.description(),.95f,Palette.TEXT)).row();
            card.add(label(profile.achievementUnlocked(achievement) ? "Completed"
                : profile.achievementProgress(achievement) + " / " + def.target(),1,Palette.GOLD)).row();
        }
    }
}
