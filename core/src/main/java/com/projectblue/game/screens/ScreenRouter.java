package com.projectblue.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.logic.LevelResult;
import com.projectblue.game.save.Profile;

/** Transitions commit after render, so a screen can never dispose itself mid-frame. */
public final class ScreenRouter {
    public enum Route { MENU, LEVEL_SELECT, HANGAR, SUBMARINE_SELECT, PILOT_SELECT, WEAPON_SELECT, UPGRADES, ACHIEVEMENTS, SETTINGS, CREDITS, FINALE, PLAY, PAUSE, RESUME, RESULT, EXIT }
    private final ProjectBlueGame game;
    private GameScreen run;
    private Route pending;
    private boolean lifecyclePaused;
    private int selectedLevel = 1;
    private Difficulty selectedDifficulty = Difficulty.NORMAL;
    public ScreenRouter(ProjectBlueGame game) { this.game = game; }
    public void request(Route route) { if (pending == null) pending = route; }
    public int selectedLevel() { return selectedLevel; }
    public Difficulty selectedDifficulty() { return selectedDifficulty; }
    public boolean selectLevel(int id) {
        if (!CampaignConfig.isAvailable(id) || !game.saves().profile().canPlay(id, Difficulty.NORMAL)) return false;
        selectedLevel = id; selectedDifficulty = Difficulty.NORMAL; return true;
    }
    public boolean selectDifficulty(Difficulty difficulty) {
        if (!game.saves().profile().canPlay(selectedLevel, difficulty)) return false;
        selectedDifficulty = difficulty; return true;
    }
    public boolean requestDive(int id, Difficulty difficulty) {
        if (pending != null || lifecyclePaused || !CampaignConfig.isAvailable(id) || !game.saves().profile().canPlay(id, difficulty)) return false;
        selectedLevel = id; selectedDifficulty = difficulty; request(Route.PLAY); return true;
    }
    public void flush() {
        if (pending == null) return;
        Route route = pending; pending = null;
        if (lifecyclePaused && (route == Route.PLAY || route == Route.RESUME)) return;
        switch (route) {
            case MENU, LEVEL_SELECT, HANGAR, SUBMARINE_SELECT, PILOT_SELECT, WEAPON_SELECT, UPGRADES, ACHIEVEMENTS, SETTINGS, CREDITS, FINALE -> {
                game.audio().resume(); switchTo(menu(route)); disposeRun();
            }
            case PLAY -> {
                if (!CampaignConfig.isAvailable(selectedLevel) || !game.saves().profile().canPlay(selectedLevel, selectedDifficulty)) {
                    switchTo(new LevelSelectScreen(game)); disposeRun(); break;
                }
                disposeRun();
                run = new GameScreen(game, RunSpec.create(selectedLevel, selectedDifficulty, Loadout.from(game.saves().profile())));
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
                    LevelResult result = run.world().result();
                    Profile profile = game.saves().profile();
                    int nextLevel = result.levelId + 1;
                    boolean levelWasOpen = profile.canPlay(nextLevel, Difficulty.NORMAL);
                    Difficulty nextDifficulty = result.difficulty == Difficulty.ABYSS ? null : Difficulty.values()[result.difficulty.ordinal() + 1];
                    boolean difficultyWasOpen = profile.canPlay(result.levelId, nextDifficulty);
                    profile.record(result);
                    game.saves().save();
                    String unlocked = "";
                    if (!levelWasOpen && profile.canPlay(nextLevel, Difficulty.NORMAL)) unlocked += "Sector " + nextLevel + " unlocked. ";
                    if (!difficultyWasOpen && nextDifficulty != null && profile.canPlay(result.levelId, nextDifficulty)) unlocked += nextDifficulty + " unlocked for this sector.";
                    if (result.completed && result.levelId==CampaignConfig.LEVEL_COUNT) switchTo(new FinaleScreen(game));
                    else switchTo(new ResultScreen(game, result, unlocked));
                    disposeRun();
                }
            }
            case EXIT -> Gdx.app.exit();
        }
    }
    private Screen menu(Route route) {
        return switch (route) {
            case MENU -> new MainMenuScreen(game);
            case LEVEL_SELECT -> new LevelSelectScreen(game);
            case HANGAR -> new HangarScreen(game);
            case SUBMARINE_SELECT -> new SubmarineSelectScreen(game);
            case PILOT_SELECT -> new PilotSelectScreen(game);
            case WEAPON_SELECT -> new WeaponSelectScreen(game);
            case UPGRADES -> new UpgradesScreen(game);
            case ACHIEVEMENTS -> new AchievementsScreen(game);
            case SETTINGS -> new SettingsScreen(game);
            case CREDITS -> new CreditsScreen(game);
            case FINALE -> game.saves().profile().campaignCompleted() ? new FinaleScreen(game) : new LevelSelectScreen(game);
            default -> throw new IllegalArgumentException("Not a menu route");
        };
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
