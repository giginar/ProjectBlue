package com.projectblue.game.screens;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.ui.Palette;

public final class ResultScreen extends BaseMenuScreen {
    private static final String[] NAMES = {"COMBAT", "CLEANUP", "RESCUE", "INTEGRITY"};
    private final LevelResult result;
    private final float[] values;
    private final String[] labels;
    private final String score, salvage;
    public ResultScreen(ProjectBlueGame game, LevelResult result) {
        super(game); this.result = result;
        values = new float[]{result.combat, result.cleanup, result.rescue, result.integrity};
        labels = new String[4];
        for (int i = 0; i < 4; i++) labels[i] = Math.round(values[i]) + "%";
        score = "SCORE  " + result.score; salvage = "SALVAGE RECOVERED  " + result.salvage;
    }
    public void render(float delta) {
        time += Math.min(delta, .1f);
        game.ocean().backdrop(time, result.cleanup / 100);
        ui.beginShapes();
        ui.rect(34, 305, 472, 344, Palette.INK);
        for (int i = 0; i < 3; i++) star(190 + i * 80, 727, i < result.stars);
        for (int i = 0; i < 4; i++) ui.bar(58, 564 - i * 69, 424, 7, values[i] / 100, Palette.AQUA);
        ui.button(34, 189, 472, 68, true);
        ui.button(34, 99, 472, 56, false);
        ui.endShapes();
        ui.beginText();
        ui.centered("DIVE REPORT / 01", 903, .72f, Palette.AQUA);
        ui.centered(result.completed ? "REEF REACHED" : "DIVE ENDED", 842, 1.55f, Palette.TEXT);
        ui.centered(result.completed ? "EVERY SMALL ACTION LEAVES LIFE." : "REPAIR, REGROUP, RETURN.", 791, .6f, Palette.MUTED);
        ui.centered(score, 679, 1.05f, Palette.GOLD);
        for (int i = 0; i < 4; i++) {
            ui.text(NAMES[i], 58, 608 - i * 69, .8f, Palette.TEXT);
            ui.text(labels[i], 407, 608 - i * 69, .8f, Palette.AQUA);
        }
        ui.centered(salvage, 292, .63f, Palette.GOLD);
        ui.centered("DIVE AGAIN", 233, 1, Palette.INK);
        ui.centered("BACK TO SURFACE", 136, .8f, Palette.TEXT);
        ui.centered(game.saves().writeFailed() ? "PROFILE NOT SAVED / STORAGE UNAVAILABLE" : "RESTORE THE REEF. PROTECT WHAT REMAINS.", 56, .54f, Palette.MUTED);
        ui.endText();
    }
    private void star(float x, float y, boolean earned) {
        ui.shapes.setColor(earned ? Palette.GOLD : Palette.EDGE);
        for (int i = 0; i < 10; i++) {
            double a = Math.PI / 2 + i * Math.PI / 5, b = a + Math.PI / 5;
            float r = i % 2 == 0 ? 25 : 11, next = i % 2 == 0 ? 11 : 25;
            ui.shapes.triangle(x,y,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,
                x+(float)Math.cos(b)*next,y+(float)Math.sin(b)*next);
        }
    }
    protected void click(float x, float y) {
        if (inside(x,y,34,189,472,68)) game.router().request(ScreenRouter.Route.PLAY);
        else if (inside(x,y,34,99,472,56)) game.router().request(ScreenRouter.Route.MENU);
    }
}

