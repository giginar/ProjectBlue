package com.projectblue.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.projectblue.game.assets.GameAssets;
import com.projectblue.game.audio.AudioService;
import com.projectblue.game.platform.PlatformService;
import com.projectblue.game.render.OceanRenderer;
import com.projectblue.game.save.*;
import com.projectblue.game.screens.*;
import com.projectblue.game.ui.UiPainter;
import com.projectblue.game.ui.MenuTheme;
import com.projectblue.game.ui.AchievementToast;

/** Composition root. Owns global services and GPU resources for the whole application lifetime. */
public class ProjectBlueGame extends Game {
    private final PlatformService platform;
    private GameAssets assets;
    private UiPainter ui;
    private OceanRenderer ocean;
    private SaveService saves;
    private AudioService audio;
    private ScreenRouter router;
    private MenuTheme menuTheme;
    private AchievementToast achievementToast;
    public ProjectBlueGame(PlatformService platform) { this.platform = platform; }
    public void create() {
        saves = new SaveService(new GdxSaveStore(Gdx.files.absolute(platform.saveDirectory())));
        assets = new GameAssets();
        audio = new AudioService(saves.profile());
        ui = new UiPainter();
        achievementToast = new AchievementToast(new LocalAchievementService(saves), ui);
        ocean = new OceanRenderer(ui);
        router = new ScreenRouter(this);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        assets.queue();
        setScreen(new BootScreen(this));
        // Consent is asynchronous; asset loading and offline play never wait for it.
        platform.consent().requestConsent(() -> {});
    }
    public void render() {
        Gdx.gl.glClearColor(.018f, .04f, .06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
        if (!(getScreen() instanceof BootScreen)) achievementToast.draw(Math.min(Gdx.graphics.getDeltaTime(), .1f));
        router.flush();
    }
    public void pause() {
        super.pause();
        // Android can dispatch lifecycle callbacks before create() finishes.
        if (router != null) router.pauseForLifecycle();
        if (saves != null) saves.save();
    }
    public void resume() {
        super.resume();
        if (router != null) router.resumeFromLifecycle();
    }
    public void dispose() {
        if (router != null) router.dispose();
        if (saves != null) saves.save();
        if (audio != null) audio.suspend();
        if (ui != null) ui.dispose();
        if (menuTheme != null) menuTheme.dispose();
        if (assets != null) assets.dispose();
    }
    public GameAssets assets() { return assets; }
    public UiPainter ui() { return ui; }
    public MenuTheme menuTheme() {
        if (menuTheme == null) menuTheme = new MenuTheme(assets.font());
        return menuTheme;
    }
    public OceanRenderer ocean() { return ocean; }
    public SaveService saves() { return saves; }
    public AudioService audio() { return audio; }
    public ScreenRouter router() { return router; }
    public PlatformService platform() { return platform; }
}
