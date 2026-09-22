package com.projectblue.game.screens;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.ui.Palette;

public final class PauseScreen extends BaseMenuScreen {
    public PauseScreen(ProjectBlueGame game) { super(game); }
    public void render(float delta) {
        if (game.router().activeRun() != null) game.router().activeRun().drawFrozen();
        ui.beginShapes();
        ui.rect(24, 240, 492, 460, Palette.EDGE);
        ui.rect(26, 242, 488, 456, Palette.INK);
        ui.button(58, 459, 424, 69, true);
        ui.button(58, 365, 202, 55, false);
        ui.button(280, 365, 202, 55, false);
        ui.button(58, 279, 424, 56, false);
        ui.endShapes();
        ui.beginText();
        ui.centered(game.i18n().text("pause.title"), 653, 1.12f, Palette.TEXT);
        ui.centered(game.i18n().text("pause.subtitle"), 604, .65f, Palette.MUTED);
        ui.centered(game.i18n().text("pause.resume"), 503, 1, Palette.INK);
        ui.text(game.i18n().text("pause.sound", game.i18n().text(game.saves().profile().soundEnabled ? "common.on" : "common.off")), 75, 400, .73f, Palette.TEXT);
        ui.text(game.i18n().text("pause.music", game.i18n().text(game.saves().profile().musicEnabled ? "common.on" : "common.off")), 297, 400, .73f, Palette.TEXT);
        ui.centered(game.i18n().text("pause.end"), 315, .78f, Palette.MUTED);
        ui.endText();
    }
    protected void click(float x, float y) {
        if (inside(x,y,58,459,424,69)) game.router().request(ScreenRouter.Route.RESUME);
        else if (inside(x,y,58,365,202,55)) { game.audio().toggleSound(); game.saves().save(); }
        else if (inside(x,y,280,365,202,55)) { game.audio().toggleMusic(); game.saves().save(); }
        else if (inside(x,y,58,279,424,56)) game.router().request(ScreenRouter.Route.MENU);
    }
    protected void back() { game.router().request(ScreenRouter.Route.RESUME); }
}
