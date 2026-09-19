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
        report.add(label("Mission score / " + result.score + "\nSalvage / +" + result.salvage, 1.05f, Palette.TEXT)).row();
        report.add(label("Combat " + Math.round(result.combat) + "%   Cleanup " + Math.round(result.cleanup)
            + "%\nRescue " + Math.round(result.rescue) + "%   Hull integrity " + Math.round(result.integrity) + "%", .9f, Palette.TEXT)).row();
        report.add(environmentComparison(result)).height(126).padTop(8).row();
        if (!unlocked.isEmpty()) note(unlocked);
        else note(result.completed ? "Records updated. Return whenever the ocean calls." : "Repair, regroup, return. Earn a star to advance.");
        rewardActions(!result.completed);
        action("replay", "Dive again / " + result.difficulty, () -> game.router().requestDive(result.levelId, result.difficulty));
        route("levels", "Level Select", ScreenRouter.Route.LEVEL_SELECT);
        route("hangar", "Hangar", ScreenRouter.Route.HANGAR);
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("back")).setText("MAIN MENU");
    }
    private Table environmentComparison(LevelResult result) {
        Table comparison = new Table(); comparison.defaults().grow().space(10);
        MissionConfig mission=MissionConfig.forLevel(result.levelId);
        MissionConfig.MissionType type=mission==null?MissionConfig.MissionType.BLUE_COAST:mission.type;
        String before=before(type), after=after(type);
        comparison.add(environmentPanel(before, new Color(.08f,.16f,.19f,1))).uniformX();
        int restored = Math.round(result.afterRestoration * 100);
        comparison.add(environmentPanel(after + restored + "%\nWILDLIFE RETURNED", new Color(.08f,.42f,.39f,1))).uniformX();
        return comparison;
    }
    private static String before(MissionConfig.MissionType type) {
        return switch (type) {
            case BLUE_COAST -> "BEFORE\nPOLLUTED COAST\nTRAPPED TURTLES";
            case CORAL_GARDENS -> "BEFORE\nBLEACHED CORAL\nMINERAL SCARS";
            case GHOST_NETS -> "BEFORE\nDARK CURRENT\nGHOST NETS";
            case SUNKEN_CITY -> "BEFORE\nTOXIC RUINS\nTRAPPED DIVERS";
            case BLACK_TIDE -> "BEFORE\nBLACK OIL\nOPEN LEAKS";
            case SILENT_REEF -> "BEFORE\nSONAR HAZE\nSILENT REEF";
            case FROZEN_DEPTHS -> "BEFORE\nTHERMAL WASTE\nACTIVE DRILLS";
            case ABYSS_MINE -> "BEFORE\nCRUSHING DEPTH\nACTIVE MINES";
            case PLASTIC_VORTEX -> "BEFORE\nCHOKED GYRE\nTRASH STORM";
            case NEREID_CORE -> "BEFORE\nBLACK METAL\nRED ALARMS";
        };
    }
    private static String after(MissionConfig.MissionType type) {
        return switch (type) {
            case BLUE_COAST -> "AFTER\nCLEAR COAST\n+";
            case CORAL_GARDENS -> "AFTER\nCORAL COLOR\n+";
            case GHOST_NETS -> "AFTER\nOPEN WATER\n+";
            case SUNKEN_CITY -> "AFTER\nSAFE CITY\n+";
            case BLACK_TIDE -> "AFTER\nCLEAN CURRENT\n+";
            case SILENT_REEF -> "AFTER\nREEF SONG\n+";
            case FROZEN_DEPTHS -> "AFTER\nCOOLED WATER\n+";
            case ABYSS_MINE -> "AFTER\nSAFE DEPTH\n+";
            case PLASTIC_VORTEX -> "AFTER\nOPEN CURRENT\n+";
            case NEREID_CORE -> "AFTER\nLIVING OCEAN\n+";
        };
    }
    private Stack environmentPanel(String text, Color tint) {
        Image water = new Image(game.menuTheme().panel); water.setColor(tint);
        Stack stack = new Stack(); stack.add(water); stack.add(label(text,.62f,Palette.TEXT));
        return stack;
    }
}
