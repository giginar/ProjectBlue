package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;
import com.projectblue.game.ui.Palette;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class MainMenuScreen extends StageMenuScreen {
    private final Table welcome, information;
    private final com.badlogic.gdx.scenes.scene2d.ui.Label build;
    public MainMenuScreen(ProjectBlueGame game) {
        super(game, "Project Blue", "Leave a living ocean behind.");
        welcome = panel();
        welcome.add(label("Surface operations", 1.1f, Palette.AQUA)).row();
        welcome.add(label("Chart a route. Prepare your crew.\nBring the ocean back to life.", .95f, Palette.TEXT)).row();
        route("play", "Play", ScreenRouter.Route.LEVEL_SELECT);
        route("hangar", "Hangar", ScreenRouter.Route.HANGAR);
        route("achievements", "Achievements", ScreenRouter.Route.ACHIEVEMENTS);
        route("settings", "Settings", ScreenRouter.Route.SETTINGS);
        if (profile.campaignCompleted()) route("finale", "Final Results", ScreenRouter.Route.FINALE);
        information = new Table(); information.defaults().growX().height(84).space(10);
        information.add(button("credits", "Credits", () -> game.router().request(ScreenRouter.Route.CREDITS))).uniformX();
        information.add(button("privacy", "Privacy", () -> game.router().request(ScreenRouter.Route.PRIVACY))).uniformX();
        body.add(information).growX().row();
        build = label("Build " + BuildInfo.VERSION_NAME, .92f, Palette.MUTED);
        build.setName("build-version");
        body.add(build).growX().padBottom(16).row();
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("back")).setText("EXIT");
    }
    @Override protected void compactLayout(boolean compact) {
        float height = compact ? 72 : 84;
        for (String id : new String[]{"play", "hangar", "achievements", "settings", "finale"}) {
            var actor = stage.getRoot().findActor(id);
            if (actor != null && body.getCell(actor) != null) body.getCell(actor).height(height);
        }
        for (var cell : information.getCells()) cell.height(height).space(compact ? 6 : 10);
        welcome.pad(compact ? 12 : 20);
        String version = BuildInfo.VERSION_NAME;
        int revision = version.indexOf("-g");
        build.setText("BUILD " + (compact && revision > 0 ? version.substring(0, revision) : version));
        body.getCell(build).padBottom(compact ? 4 : 16);
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.EXIT); }
}
