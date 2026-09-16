package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.BuildInfo;
import com.projectblue.game.ui.Palette;

public final class MainMenuScreen extends BaseMenuScreen {
    private final String best;
    public MainMenuScreen(ProjectBlueGame game) {
        super(game);
        best = "BEST " + game.saves().profile().bestScore + " / STARS " + game.saves().profile().bestStars;
    }
    public void render(float delta) {
        time += Math.min(delta, .1f);
        game.ocean().backdrop(time, .45f);
        ui.beginShapes();
        game.ocean().submarine(270, 530 + (float)Math.sin(time) * 7, 2.05f, time);
        game.ocean().turtle(405, 478, .85f, true);
        ui.rect(34, 335, 472, 78, Palette.PANEL);
        ui.rect(34, 335, 4, 78, Palette.AQUA);
        ui.button(34, 228, 472, 74, true);
        ui.button(34, 145, 226, 56, false);
        ui.button(280, 145, 226, 56, false);
        ui.endShapes();
        ui.beginText();
        ui.centered("OCEAN RECOVERY DIVISION", 891, .65f, Palette.AQUA);
        ui.centered("PROJECT", 826, 1.5f, Palette.TEXT);
        ui.centered("BLUE", 768, 4.9f, Palette.TEXT);
        ui.centered("LEAVE A LIVING OCEAN BEHIND.", 635, .7f, Palette.MUTED);
        ui.text("01  THE QUIET REEF", 55, 392, .82f, Palette.TEXT);
        ui.text("3 MIN / COMBAT + CLEANUP + RESCUE", 55, 362, .6f, Palette.MUTED);
        ui.centered("DIVE IN  >", 273, 1.05f, Palette.INK);
        ui.text(game.saves().profile().soundEnabled ? "SOUND ON" : "SOUND OFF", 64, 180, .76f, Palette.TEXT);
        ui.text(game.saves().profile().musicEnabled ? "MUSIC ON" : "MUSIC OFF", 309, 180, .76f, Palette.TEXT);
        ui.centered("DRAG TO STEER. WE HANDLE THE REST.", 110, .61f, Palette.MUTED);
        ui.centered(best, 69, .61f, Palette.GOLD);
        if (game.saves().writeFailed()) ui.centered("PROFILE NOT SAVED / STORAGE UNAVAILABLE", 34, .54f, Palette.RED);
        else if (game.saves().recovered()) ui.centered("PROFILE RECOVERED TO DEFAULTS", 34, .54f, Palette.GOLD);
        else ui.centered("BUILD " + BuildInfo.VERSION_NAME, 34, .5f, Palette.MUTED);
        ui.endText();
    }
    protected void click(float x, float y) {
        if (inside(x,y,34,228,472,74)) game.router().request(ScreenRouter.Route.PLAY);
        else if (inside(x,y,34,145,226,56)) { game.audio().toggleSound(); game.saves().save(); }
        else if (inside(x,y,280,145,226,56)) { game.audio().toggleMusic(); game.saves().save(); }
    }
    protected void back() { game.router().request(ScreenRouter.Route.EXIT); }
}
