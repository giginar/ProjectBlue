package com.projectblue.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.ui.Palette;

public final class ResultScreen extends StageMenuScreen {
    public ResultScreen(ProjectBlueGame game, LevelResult result, String unlocked) {
        super(game, game.i18n().text("result.title"), game.i18n().text("level." + result.levelId + ".name") + " / " + game.i18n().text("difficulty." + result.difficulty));
        Table report = panel();
        report.add(label(result.completed ? t("result.success") : t("result.failed"), 1.2f, Palette.AQUA)).row();
        report.add(rating(result.stars)).height(40).row();
        report.add(label(t("result.rewards", result.score, result.salvage), 1.05f, Palette.TEXT)).row();
        report.add(label(t("result.metrics", Math.round(result.combat), Math.round(result.cleanup), Math.round(result.rescue), Math.round(result.integrity)), .9f, Palette.TEXT)).row();
        report.add(environmentComparison(result)).height(126).padTop(8).row();
        if (!unlocked.isEmpty()) note(unlocked);
        else note(result.completed ? t("result.updated") : t("result.try_again"));
        rewardActions(!result.completed);
        action("replay", t("result.replay") + " / " + t("difficulty." + result.difficulty), () -> game.router().requestDive(result.levelId, result.difficulty));
        route("levels", t("result.levels"), ScreenRouter.Route.LEVEL_SELECT);
        route("hangar", t("menu.hangar"), ScreenRouter.Route.HANGAR);
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("back")).setText(t("result.menu"));
    }
    private Table environmentComparison(LevelResult result) {
        Table comparison = new Table(); comparison.defaults().grow().space(10);
        MissionConfig mission=MissionConfig.forLevel(result.levelId);
        MissionConfig.MissionType type=mission==null?MissionConfig.MissionType.BLUE_COAST:mission.type;
        String before=t("result.before." + type), after=t("result.after." + type);
        comparison.add(environmentPanel(before, new Color(.08f,.16f,.19f,1))).uniformX();
        int restored = Math.round(result.afterRestoration * 100);
        comparison.add(environmentPanel(after + restored + "%\n" + t("result.wildlife"), new Color(.08f,.42f,.39f,1))).uniformX();
        return comparison;
    }
    private Stack environmentPanel(String text, Color tint) {
        Image water = new Image(game.menuTheme().panel); water.setColor(tint);
        Stack stack = new Stack(); stack.add(water); stack.add(label(text,.62f,Palette.TEXT));
        return stack;
    }
}
