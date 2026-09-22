package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;
import com.projectblue.game.ui.Palette;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class MainMenuScreen extends StageMenuScreen {
    private final Table welcome, information;
    private final com.badlogic.gdx.scenes.scene2d.ui.Label build;
    public MainMenuScreen(ProjectBlueGame game) {
        super(game, "Project Blue: Ocean Guard", game.i18n().text("menu.subtitle"));
        welcome = panel();
        welcome.add(label(t("menu.surface"), 1.1f, Palette.AQUA)).row();
        welcome.add(label(t("menu.welcome"), .95f, Palette.TEXT)).row();
        route("play", t("menu.play"), ScreenRouter.Route.LEVEL_SELECT);
        route("hangar", t("menu.hangar"), ScreenRouter.Route.HANGAR);
        route("achievements", t("menu.achievements"), ScreenRouter.Route.ACHIEVEMENTS);
        route("settings", t("menu.settings"), ScreenRouter.Route.SETTINGS);
        if (profile.campaignCompleted()) route("finale", t("menu.final_results"), ScreenRouter.Route.FINALE);
        information = new Table(); information.defaults().growX().height(84).space(10);
        information.add(button("credits", t("menu.credits"), () -> game.router().request(ScreenRouter.Route.CREDITS))).uniformX();
        information.add(button("privacy", t("menu.privacy"), () -> game.router().request(ScreenRouter.Route.PRIVACY))).uniformX();
        body.add(information).growX().row();
        build = label(t("menu.build", BuildInfo.VERSION_NAME), .92f, Palette.MUTED);
        build.setName("build-version");
        body.add(build).growX().padBottom(16).row();
        ((com.badlogic.gdx.scenes.scene2d.ui.TextButton) stage.getRoot().findActor("back")).setText(t("common.exit"));
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
        build.setText(t("menu.build", compact && revision > 0 ? version.substring(0, revision) : version).toUpperCase(java.util.Locale.ROOT));
        body.getCell(build).padBottom(compact ? 4 : 16);
    }
    @Override protected void back() { game.router().request(ScreenRouter.Route.EXIT); }
}
