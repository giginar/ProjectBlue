package com.projectblue.game.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.ui.Palette;

public final class ResultScreen extends StageMenuScreen {
    public ResultScreen(ProjectBlueGame game, LevelResult result, String unlocked) {
        super(game, "Result", CampaignConfig.DEFAULT.level(result.levelId).name() + " / " + result.difficulty);
        Table report = panel();
        report.add(label(result.completed ? "Sector restored" : "Dive ended", 1.2f, Palette.AQUA)).row();
        report.add(rating(result.stars)).height(40).row();
        report.add(label("Score " + result.score + "\nSalvage recovered " + result.salvage, 1.05f, Palette.TEXT)).row();
        report.add(label("Combat " + Math.round(result.combat) + "%\nCleanup " + Math.round(result.cleanup)
            + "%\nRescue " + Math.round(result.rescue) + "%\nIntegrity " + Math.round(result.integrity) + "%", 1, Palette.TEXT)).row();
        report.add(environmentComparison(result)).height(126).padTop(8).row();
        if (!unlocked.isEmpty()) note(unlocked);
        else note(result.completed ? "Records updated. Return whenever the ocean calls." : "Repair, regroup, return. Earn a star to advance.");
        action("replay", "Dive again / " + result.difficulty, () -> game.router().requestDive(result.levelId, result.difficulty));
        route("levels", "Level Select", ScreenRouter.Route.LEVEL_SELECT);
        route("hangar", "Hangar", ScreenRouter.Route.HANGAR);
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("back")).setText("MAIN MENU");
    }
    private Table environmentComparison(LevelResult result) {
        Table comparison = new Table(); comparison.defaults().grow().space(10);
        MissionConfig mission=MissionConfig.forLevel(result.levelId);
        String before=mission!=null && mission.type==MissionConfig.MissionType.CORAL_GARDENS
            ? "BEFORE\nBLEACHED CORAL\nMINERAL SCARS"
            : mission!=null && mission.type==MissionConfig.MissionType.GHOST_NETS
            ? "BEFORE\nDARK CURRENT\nGHOST NETS"
            : mission!=null && mission.type==MissionConfig.MissionType.SUNKEN_CITY
            ? "BEFORE\nTOXIC RUINS\nTRAPPED DIVERS"
            : mission!=null && mission.type==MissionConfig.MissionType.BLACK_TIDE
            ? "BEFORE\nBLACK OIL\nOPEN LEAKS" : "BEFORE\nMURKY WATER\nMUTED REEF";
        String after=mission!=null && mission.type==MissionConfig.MissionType.CORAL_GARDENS
            ? "AFTER\nCORAL COLOR +" : mission!=null && mission.type==MissionConfig.MissionType.GHOST_NETS
            ? "AFTER\nOPEN WATER +" : mission!=null && mission.type==MissionConfig.MissionType.SUNKEN_CITY
            ? "AFTER\nSAFE CITY +" : mission!=null && mission.type==MissionConfig.MissionType.BLACK_TIDE
            ? "AFTER\nCLEAN CURRENT +" : "AFTER\nCLEAR WATER +";
        comparison.add(environmentPanel(before, new Color(.08f,.16f,.19f,1))).uniformX();
        int restored = Math.round(result.afterRestoration * 100);
        comparison.add(environmentPanel(after + restored + "%\nWILDLIFE RETURNED", new Color(.08f,.42f,.39f,1))).uniformX();
        return comparison;
    }
    private Stack environmentPanel(String text, Color tint) {
        Image water = new Image(game.menuTheme().panel); water.setColor(tint);
        Stack stack = new Stack(); stack.add(water); stack.add(label(text,.72f,Palette.TEXT));
        return stack;
    }
}
