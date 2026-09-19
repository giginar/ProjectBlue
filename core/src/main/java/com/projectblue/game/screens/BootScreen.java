package com.projectblue.game.screens;
import com.badlogic.gdx.ScreenAdapter;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.ui.Palette;

public final class BootScreen extends ScreenAdapter {
    private final ProjectBlueGame game;
    private boolean ready;
    public BootScreen(ProjectBlueGame game) { this.game = game; game.ui().setFont(game.assets().font()); }
    public void render(float delta) {
        if (game.assets().update() && !ready) {
            ready = true;
            game.ui().setFont(game.assets().font());
            game.audio().attach(game.assets());
            game.router().request(ScreenRouter.Route.MENU);
        }
        game.ui().beginShapes();
        game.ui().rect(0, 0, 540, 960, Palette.INK);
        game.ui().bar(70, 460, 400, 8, game.assets().progress(), Palette.AQUA);
        game.ui().endShapes();
        game.ui().beginText();
        game.ui().centered("PROJECT BLUE", 535, 1.25f, Palette.TEXT);
        game.ui().centered("PREPARING THE DIVE", 495, .7f, Palette.AQUA);
        game.ui().centered(Math.round(game.assets().progress()*100) + "%", 440, .65f, Palette.MUTED);
        game.ui().endText();
    }
    public void resize(int width, int height) { game.ui().resize(width, height); }
}
