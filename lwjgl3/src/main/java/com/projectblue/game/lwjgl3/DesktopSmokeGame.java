package com.projectblue.game.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.projectblue.game.ProjectBlueGame;
import com.projectblue.game.config.*;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.platform.NoOpPlatformService;
import com.projectblue.game.save.*;
import com.projectblue.game.screens.*;
import com.projectblue.game.logic.GameWorld;
import com.projectblue.game.logic.ShorelineCompactor;
import static com.projectblue.game.config.GameConfig.*;

/** Real OpenGL integration checks. Only the isolated build/smoke/profile directory is written. */
final class DesktopSmokeGame extends ProjectBlueGame {
    private int frame, step, stepFrames;
    private float pausedElapsed, initialX;
    private final boolean reload;
    private boolean advancing;
    private final Vector2 point = new Vector2();
    DesktopSmokeGame(boolean reload) { super(new NoOpPlatformService(profilePath())); this.reload = reload; }
    private static String profilePath() { return System.getProperty("user.dir") + "/build/smoke/profile"; }
    @Override public void create() {
        super.create();
        if (!reload) require(saves().resetForDevelopment(true), "isolated smoke profile reset");
    }
    @Override public void render() {
        super.render();
        // GLFW can synchronously render again from setWindowedMode's resize callback.
        if (advancing) return;
        advancing = true;
        try { advanceSmoke(); } finally { advancing = false; }
    }
    private void advanceSmoke() {
        frame++; stepFrames++;
        if (frame > 2000) throw new IllegalStateException("Desktop smoke timed out at step " + step);
        if (stepFrames < 5 || frame < 30) return;
        if (reload) {
            if (!(getScreen() instanceof MainMenuScreen)) return;
            verifyPersisted(saves().profile()); capture("15-reloaded");
            Gdx.app.log("SMOKE", "PASS: separate application launch restored campaign, records, equipment, achievements and settings");
            Gdx.app.exit(); return;
        }
        switch (step) {
            case 0 -> {
                if (!(getScreen() instanceof MainMenuScreen)) return;
                require(!router().requestDive(2, Difficulty.NORMAL), "locked level rejected by router");
                require(!router().requestDive(1, Difficulty.HARD), "locked difficulty rejected by router");
                capture("01-menu"); clickActor("play"); next();
            }
            case 1 -> {
                require(getScreen() instanceof LevelSelectScreen, "menu -> level select");
                require(((Button) actor("level-2")).isDisabled(), "locked level visibly disabled");
                capture("02-levels"); clickActor("level-1"); next();
            }
            case 2 -> {
                require(((Button) actor("difficulty-HARD")).isDisabled(), "hard locked initially");
                require(((Button) actor("difficulty-NORMAL")).isChecked(), "normal selected");
                capture("03-briefing"); clickActor("launch"); next();
            }
            case 3 -> {
                require(getScreen() instanceof GameScreen, "briefing -> gameplay");
                initialX = router().activeRun().world().player.x;
                pointerDown(270, 170); pointerDrag(420, 370); next();
            }
            case 4 -> {
                if (router().activeRun().world().player.x <= initialX + 90) return;
                pointerUp(420, 370); capture("04-gameplay"); click(482, 903); next();
            }
            case 5 -> {
                require(getScreen() instanceof PauseScreen, "pause button");
                pausedElapsed = router().activeRun().world().elapsed(); capture("05-paused"); next();
            }
            case 6 -> {
                require(router().activeRun().world().elapsed() == pausedElapsed, "pause freezes simulation");
                click(270, 493); next();
            }
            case 7 -> { require(getScreen() instanceof GameScreen, "resume"); pause(); next(); }
            case 8 -> {
                require(getScreen() instanceof PauseScreen, "lifecycle suspension");
                resume(); require(getScreen() instanceof PauseScreen, "lifecycle waits for player");
                click(270, 493); next();
            }
            case 9 -> { require(getScreen() instanceof GameScreen, "lifecycle continue"); Gdx.graphics.setWindowedMode(700, 600); next(); }
            case 10 -> {
                require(ui().viewport.getWorldWidth() == WIDTH && ui().viewport.getWorldHeight() == HEIGHT, "original world dimensions");
                require(ui().viewport.getScreenX() > 0, "wide gameplay letterboxing");
                float before = router().activeRun().world().player.x;
                Gdx.input.getInputProcessor().touchDown(0, 300, 0, Input.Buttons.LEFT);
                Gdx.input.getInputProcessor().touchDragged(690, 300, 0);
                getScreen().render(STEP);
                Gdx.input.getInputProcessor().touchUp(690, 300, 0, Input.Buttons.LEFT);
                require(before == router().activeRun().world().player.x, "letterbox cannot steer");
                capture("06-wide-game"); Gdx.graphics.setWindowedMode(486, 864); next();
            }
            case 11 -> {
                GameWorld world = router().activeRun().world();
                for (int i = 0; i < 90 && !world.finished(); i++) {
                    float x = WIDTH / 2f + (float) Math.sin(world.elapsed() * 1.5f) * 190;
                    world.player.health = world.player.maxHealth;
                    if (world.compactor() != null) {
                        ShorelineCompactor boss=world.compactor();
                        switch (boss.state()) {
                            case DISCHARGE, PRESS_ACTIVE, CORE_EXPOSED -> boss.hitCore(Integer.MAX_VALUE);
                            case PIPES -> { boss.hitPipe(true,Integer.MAX_VALUE); boss.hitPipe(false,Integer.MAX_VALUE); }
                            default -> { }
                        }
                    }
                    world.update(STEP, true, x, PLAYER_START_Y);
                }
                if (world.finished()) {
                    require(world.result().completed, "Blue Coast boss and recovery completion");
                    require(world.elapsed() >= world.mission().boss.start(), "five-minute authored mission");
                    require(world.hostileBullets() == 0, "dangerous projectiles cleared after boss"); next();
                }
            }
            case 12 -> {
                require(getScreen() instanceof ResultScreen, "results transition"); capture("07-result");
                Profile p = saves().profile();
                require(p.completedRuns == 1, "result recorded exactly once");
                require(p.canPlay(2, Difficulty.NORMAL), "level two automatically unlocked");
                require(p.canPlay(1, Difficulty.HARD) && !p.canPlay(2, Difficulty.HARD), "per-level difficulty unlock");
                SaveService disk = new SaveService(new GdxSaveStore(Gdx.files.absolute(profilePath())));
                require(!disk.recovered() && disk.profile().canPlay(2, Difficulty.NORMAL), "disk round trip");
                clickActor("replay"); next();
            }
            case 13 -> {
                require(getScreen() instanceof GameScreen, "replay creates a fresh run");
                require(router().activeRun().world().elapsed() < 1, "replay resets clock");
                require(router().activeRun().world().player.health == PLAYER_HEALTH, "replay resets hull");
                router().request(ScreenRouter.Route.MENU); next();
            }
            case 14 -> { clickActor("hangar"); next(); }
            case 15 -> { require(getScreen() instanceof HangarScreen, "hangar"); capture("08-hangar"); clickActor("submarines"); next(); }
            case 16 -> { capture("09-submarines"); clickActor("submarine-MANTA"); next(); }
            case 17 -> { require(saves().profile().selectedSubmarine == Submarine.MANTA, "submarine selection"); clickActor("back"); next(); }
            case 18 -> { clickActor("pilots"); next(); }
            case 19 -> { capture("10-pilots"); clickActor("pilot-NERI"); next(); }
            case 20 -> { require(saves().profile().selectedPilot == Pilot.NERI, "pilot selection"); clickActor("back"); next(); }
            case 21 -> {
                // Explicit test funds in the isolated profile exercise a purchase and duplicate-event protection.
                saves().profile().totalSalvage = 200; saves().save(); clickActor("weapons"); step = 38; stepFrames = 0;
            }
            case 22 -> { capture("11-upgrades"); clickActor("upgrade-HULL"); next(); }
            case 23 -> {
                require(saves().profile().upgradeLevel(Upgrade.HULL) == 1 && saves().profile().totalSalvage == 170, "one click purchases exactly once");
                clickActor("back"); next();
            }
            case 24 -> { clickActor("back"); next(); }
            case 25 -> { clickActor("achievements"); next(); }
            case 26 -> { require(getScreen() instanceof AchievementsScreen, "achievements"); capture("12-achievements"); clickActor("back"); next(); }
            case 27 -> { clickActor("settings"); next(); }
            case 28 -> {
                require(getScreen() instanceof SettingsScreen, "settings");
                require(((StageMenuScreen) getScreen()).stage().getRoot().findActor("reset") == null, "release platform hides reset");
                clickActor("sound"); next();
            }
            case 29 -> {
                require(!saves().profile().soundEnabled, "sound toggle fires once");
                Gdx.graphics.setWindowedMode(320, 640); next();
            }
            case 30 -> { capture("13-phone-settings"); clickActor("back"); next(); }
            case 31 -> { clickActor("credits"); next(); }
            case 32 -> { require(getScreen() instanceof CreditsScreen, "credits"); Gdx.graphics.setWindowedMode(960, 540); next(); }
            case 33 -> { capture("14-wide-credits"); clickActor("back"); next(); }
            case 34 -> {
                verifyPersisted(saves().profile());
                require(router().requestDive(1, Difficulty.HARD), "new hard difficulty playable"); next();
            }
            case 35 -> {
                GameWorld world = router().activeRun().world();
                require(world.spec().difficulty() == Difficulty.HARD, "difficulty passed to simulation");
                require(world.player.maxHealth == 95, "loadout and permanent hull upgrade applied");
                require(world.spec().loadout().rescueSeconds() < RESCUE_SECONDS, "pilot applied");
                Gdx.graphics.setWindowedMode(486, 864); next();
            }
            case 36 -> {
                GameWorld world = router().activeRun().world();
                // Keep the test pilot alive to inspect the new boss without altering the saved result.
                for (int i = 0; i < 90 && world.elapsed() < 244; i++) { world.player.health = 95; world.update(STEP, false, 0, 0); }
                if (world.elapsed() >= 244) { require(world.boss.active, "hard Shoreline Compactor encounter"); next(); }
            }
            case 37 -> {
                capture("16-shoreline-compactor");
                require(saves().profile().completedRuns == 1, "abandoned replay grants no completion");
                Gdx.app.log("SMOKE", "PASS: menus, equipment locks, Blue Coast, campaign, disk save, replay, vessel/pilot/weapon selection, upgrades, achievement toast, audio, aspect ratios, boss and lifecycle");
                Gdx.app.exit(); next();
            }
            case 38 -> {
                require(getScreen() instanceof WeaponSelectScreen, "weapon selection");
                require(((Button)actor("weapon-FOCUS_LASER")).isDisabled(), "locked weapon disabled");
                capture("17-weapons"); clickActor("weapon-SPREAD_CANNON"); next();
            }
            case 39 -> {
                require(saves().profile().selectedWeapon == Weapon.SPREAD_CANNON, "weapon selection saved");
                clickActor("back"); next();
            }
            case 40 -> { clickActor("upgrades"); step = 22; stepFrames = 0; }
            default -> { }
        }
    }
    private void verifyPersisted(Profile p) {
        require(p.completedRuns == 1 && p.canPlay(2, Difficulty.NORMAL), "persisted level progression");
        require(p.canPlay(1, Difficulty.HARD) && !p.canPlay(1, Difficulty.EXPERT), "persisted difficulty locks");
        require(p.level(1).bestStars > 0 && p.level(1).bestScore > 0, "persisted records");
        require(p.selectedPilot == Pilot.NERI && p.selectedSubmarine == Submarine.MANTA, "persisted equipment");
        require(p.selectedWeapon == Weapon.SPREAD_CANNON, "persisted weapon");
        require(p.upgradeLevel(Upgrade.HULL) == 1 && p.totalSalvage == 170, "persisted economy");
        require(!p.soundEnabled && p.achievementProgress(Achievement.FIRST_DIVE) == 1, "persisted settings and achievements");
    }
    private void next() { step++; stepFrames = 0; }
    private Actor actor(String name) {
        require(getScreen() instanceof StageMenuScreen, "Scene2D screen for " + name);
        StageMenuScreen menu = (StageMenuScreen) getScreen();
        menu.stage().draw();
        Actor actor = menu.stage().getRoot().findActor(name);
        require(actor != null, "actor exists: " + name); return actor;
    }
    private void clickActor(String name) {
        Actor target = actor(name);
        // Scroll through the actual menu, then inject touch input through the Stage.
        ScrollPane scroll = target.firstAscendant(ScrollPane.class);
        if (scroll != null) {
            Actor content = scroll.getActor();
            target.localToAscendantCoordinates(content, point.set(0, 0));
            scroll.scrollTo(point.x, point.y, target.getWidth(), target.getHeight(), false, true);
            scroll.updateVisualScroll(); scroll.act(0); scroll.validate();
            ((StageMenuScreen) getScreen()).stage().draw();
        }
        target.localToStageCoordinates(point.set(target.getWidth() / 2, target.getHeight() / 2));
        ((StageMenuScreen) getScreen()).stage().stageToScreenCoordinates(point);
        Gdx.input.getInputProcessor().touchDown(Math.round(point.x), Math.round(point.y), 0, Input.Buttons.LEFT);
        Gdx.input.getInputProcessor().touchUp(Math.round(point.x), Math.round(point.y), 0, Input.Buttons.LEFT);
    }
    private void capture(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        PixmapIO.writePNG(Gdx.files.local("build/smoke/" + name + ".png"), pixels, -1, true); pixels.dispose();
    }
    private void project(float x, float y) { ui().viewport.project(point.set(x, y)); point.y = Gdx.graphics.getHeight() - point.y; }
    private void pointerDown(float x, float y) { project(x, y); Gdx.input.getInputProcessor().touchDown((int) point.x, (int) point.y, 0, Input.Buttons.LEFT); }
    private void pointerDrag(float x, float y) { project(x, y); Gdx.input.getInputProcessor().touchDragged((int) point.x, (int) point.y, 0); }
    private void pointerUp(float x, float y) { project(x, y); Gdx.input.getInputProcessor().touchUp((int) point.x, (int) point.y, 0, Input.Buttons.LEFT); }
    private void click(float x, float y) { pointerDown(x, y); pointerUp(x, y); }
    private void require(boolean condition, String check) { if (!condition) throw new IllegalStateException("Smoke failed at step " + step + ": " + check); }
}
