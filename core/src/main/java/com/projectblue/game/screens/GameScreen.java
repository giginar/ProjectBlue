package com.projectblue.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.input.PointerInput;
import com.projectblue.game.logic.*;
import com.projectblue.game.events.GameEvents;
import com.projectblue.game.platform.PlatformService;
import com.projectblue.game.ui.Hud;
import com.projectblue.game.config.RunSpec;
import static com.projectblue.game.config.GameConfig.*;

public final class GameScreen extends ScreenAdapter implements GameEvents.Listener {
    private final ProjectBlueGame game;
    private final GameWorld world;
    private final PointerInput input;
    private final Hud hud;
    private float accumulator, shakeTime, shakeClock;
    private boolean disposed;
    public GameScreen(ProjectBlueGame game, RunSpec spec) {
        this.game = game;
        if (!game.saves().profile().canPlay(spec.level().id(), spec.difficulty())) throw new IllegalArgumentException("Locked dive");
        world = new GameWorld(RandomProvider.seeded(spec.level().seed()), spec);
        world.setReducedEffects(game.saves().profile().reducedMotion);
        input = new PointerInput(game.ui().viewport, world, () -> game.router().request(ScreenRouter.Route.PAUSE));
        hud = new Hud(game.ui(), game.i18n());
        world.events.subscribe(hud);
        world.events.subscribe(game.audio());
        world.events.subscribe(this);
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
        shakeClock += Math.min(Math.max(delta, 0), MAX_FRAME_TIME);
        drawFrozen();
        shakeTime = Math.max(0, shakeTime - Math.min(Math.max(delta, 0), MAX_FRAME_TIME));
        if (world.finished()) game.router().request(ScreenRouter.Route.RESULT);
    }
    public void drawFrozen() {
        var profile = game.saves().profile();
        var camera = game.ui().viewport.getCamera();
        float originalX = camera.position.x, originalY = camera.position.y;
        if (shakeTime > 0 && profile.screenShakeEnabled && !profile.reducedMotion) {
            float strength = 5f * Math.min(1, shakeTime / .12f);
            camera.position.x += (float)Math.sin(shakeClock * 91) * strength;
            camera.position.y += (float)Math.cos(shakeClock * 73) * strength;
            camera.update();
        }
        game.ocean().world(world, profile.highContrastTelegraphs, profile.reducedFlashes);
        camera.position.set(originalX, originalY, camera.position.z); camera.update();
        hud.draw(world);
    }
    void refreshHud() { hud.refresh(world); }
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
        world.events.unsubscribe(this);
    }
    @Override public void onEvent(GameEvents.Type type, float x, float y, int value) {
        var profile = game.saves().profile();
        if (type == GameEvents.Type.PLAYER_HIT) {
            shakeTime = .22f;
            if (profile.hapticEnabled) game.platform().haptic(PlatformService.Haptic.DAMAGE);
        } else if (profile.hapticEnabled && type == GameEvents.Type.TURTLE_RESCUED) {
            game.platform().haptic(PlatformService.Haptic.SUCCESS);
        } else if (profile.hapticEnabled && type == GameEvents.Type.SONAR_PULSE) {
            game.platform().haptic(PlatformService.Haptic.LIGHT);
        }
    }
}
