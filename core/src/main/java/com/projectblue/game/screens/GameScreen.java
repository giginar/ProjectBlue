package com.projectblue.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.input.PointerInput;
import com.projectblue.game.logic.*;
import com.projectblue.game.ui.Hud;
import com.projectblue.game.config.RunSpec;
import static com.projectblue.game.config.GameConfig.*;

public final class GameScreen extends ScreenAdapter {
    private final ProjectBlueGame game;
    private final GameWorld world;
    private final PointerInput input;
    private final Hud hud;
    private float accumulator;
    private boolean disposed;
    public GameScreen(ProjectBlueGame game, RunSpec spec) {
        this.game = game;
        if (!game.saves().profile().canPlay(spec.level().id(), spec.difficulty())) throw new IllegalArgumentException("Locked dive");
        world = new GameWorld(RandomProvider.seeded(spec.level().seed()), spec);
        world.setReducedEffects(game.saves().profile().reducedMotion);
        input = new PointerInput(game.ui().viewport, world, () -> game.router().request(ScreenRouter.Route.PAUSE));
        hud = new Hud(game.ui());
        world.events.subscribe(hud);
        world.events.subscribe(game.audio());
        hud.update(0, world);
    }
    public void show() { resetInput(); Gdx.input.setInputProcessor(input); }
    public void render(float delta) {
        accumulator += Math.min(Math.max(delta, 0), MAX_FRAME_TIME);
        while (accumulator >= STEP && !world.finished()) {
            world.update(STEP, input.moving(), input.targetX(), input.targetY());
            accumulator -= STEP;
            hud.update(STEP, world);
        }
        drawFrozen();
        if (world.finished()) game.router().request(ScreenRouter.Route.RESULT);
    }
    public void drawFrozen() { game.ocean().world(world); hud.draw(world); }
    public GameWorld world() { return world; }
    public void resetInput() { input.reset(); accumulator = 0; }
    public void hide() { resetInput(); Gdx.input.setInputProcessor(null); }
    public void pause() { resetInput(); }
    public void resize(int width, int height) { game.ui().resize(width, height); resetInput(); }
    public void dispose() {
        if (disposed) return;
        disposed = true;
        world.events.unsubscribe(hud);
        world.events.unsubscribe(game.audio());
    }
}
