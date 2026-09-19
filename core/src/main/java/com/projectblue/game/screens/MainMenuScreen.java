package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;
import com.projectblue.game.ui.Palette;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class MainMenuScreen extends StageMenuScreen {
    public MainMenuScreen(ProjectBlueGame game) {
        super(game, "Project Blue", "Leave a living ocean behind.");
        Table welcome = panel();
        welcome.add(label("Surface operations", 1.1f, Palette.AQUA)).row();
        welcome.add(label("Chart a route. Prepare your crew.\nBring the ocean back to life.", .95f, Palette.TEXT)).row();
        route("play", "Play", ScreenRouter.Route.LEVEL_SELECT);
        route("hangar", "Hangar", ScreenRouter.Route.HANGAR);
        route("achievements", "Achievements", ScreenRouter.Route.ACHIEVEMENTS);
        route("settings", "Settings", ScreenRouter.Route.SETTINGS);
        if (profile.campaignCompleted()) route("finale", "Final Results", ScreenRouter.Route.FINALE);
        route("credits", "Credits", ScreenRouter.Route.CREDITS);
        note("Build " + BuildInfo.VERSION_NAME);
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("back")).setText("EXIT");
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.EXIT); }
}
