package com.projectblue.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.projectblue.game.ProjectBlueGame;

/** Transitions commit after render, so a screen can never dispose itself mid-frame. */
public final class ScreenRouter {
    public enum Route { MENU, PLAY, PAUSE, RESUME, RESULT, EXIT }
    private final ProjectBlueGame game;
    private GameScreen run;
    private Route pending;
    private boolean lifecyclePaused;
    public ScreenRouter(ProjectBlueGame game) { this.game = game; }
    public void request(Route route) { if (pending == null) pending = route; }
    public void flush() {
        if (pending == null) return;
        Route route = pending; pending = null;
        if (lifecyclePaused && (route == Route.PLAY || route == Route.RESUME)) return;
        switch (route) {
            case MENU -> { game.audio().resume(); switchTo(new MainMenuScreen(game)); disposeRun(); }
            case PLAY -> {
                disposeRun();
                run = new GameScreen(game);
                game.audio().resume();
                switchTo(run);
            }
            case PAUSE -> {
                if (game.getScreen() == run && run != null) {
                    run.resetInput(); game.audio().suspend(); switchTo(new PauseScreen(game));
                }
            }
            case RESUME -> {
                if (run != null && game.getScreen() instanceof PauseScreen) {
                    run.resetInput(); game.audio().resume(); switchTo(run);
                }
            }
            case RESULT -> {
                if (run != null && run.world().finished()) {
                    game.saves().profile().record(run.world().result());
                    game.saves().save();
                    switchTo(new ResultScreen(game, run.world().result()));
                    disposeRun();
                }
            }
            case EXIT -> Gdx.app.exit();
        }
    }
    private void switchTo(Screen next) {
        Screen old = game.getScreen();
        game.setScreen(next);
        if (old != null && old != run) old.dispose();
    }
    private void disposeRun() {
        if (run != null) { run.dispose(); run = null; }
    }
    public void pauseForLifecycle() {
        lifecyclePaused = true;
        // A completed result still commits; a queued resume must never override suspension.
        if (run != null && game.getScreen() == run && !run.world().finished()) {
            pending = Route.PAUSE;
        } else if (pending == Route.RESUME || pending == Route.PLAY) pending = null;
        flush();
        game.audio().suspend();
    }
    public void resumeFromLifecycle() {
        lifecyclePaused = false;
        if (!(game.getScreen() instanceof PauseScreen)) game.audio().resume();
    }
    public GameScreen activeRun() { return run; }
    public void dispose() {
        Screen current = game.getScreen();
        if (current != null && current != run) current.dispose();
        disposeRun();
    }
}

