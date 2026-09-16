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
        ui.centered("HOLDING DEPTH", 653, 1.12f, Palette.TEXT);
        ui.centered("YOUR OCEAN CAN WAIT.", 604, .65f, Palette.MUTED);
        ui.centered("RESUME DIVE", 503, 1, Palette.INK);
        ui.text(game.saves().profile().soundEnabled ? "SOUND ON" : "SOUND OFF", 75, 400, .73f, Palette.TEXT);
        ui.text(game.saves().profile().musicEnabled ? "MUSIC ON" : "MUSIC OFF", 297, 400, .73f, Palette.TEXT);
        ui.centered("END DIVE / MENU", 315, .78f, Palette.MUTED);
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

