package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;

public final class SettingsScreen extends StageMenuScreen {
    private boolean confirmingReset;
    private boolean privacyRequired;
    @Override public void render(float delta) {
        if (privacyRequired != game.platform().consent().isPrivacyOptionsRequired()) rebuild();
        super.render(delta);
    }
    public SettingsScreen(ProjectBlueGame game) { super(game, "Settings", "Sound and comfort / stored on this device"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        privacyRequired = game.platform().consent().isPrivacyOptionsRequired();
        if (privacyRequired) action("privacy-options", "Privacy Options", () -> game.platform().consent().showPrivacyOptions(() -> {}));
        route("privacy", "Privacy information", ScreenRouter.Route.PRIVACY);
        action("mute", "Master audio / " + (profile.muted ? "Muted" : "On"), () -> { game.audio().toggleMute(); save(); rebuild(); });
        action("sound", "Sound / " + (profile.soundEnabled ? "On" : "Off"), () -> { game.audio().toggleSound(); save(); rebuild(); });
        action("music", "Music / " + (profile.musicEnabled ? "On" : "Off"), () -> { game.audio().toggleMusic(); save(); rebuild(); });
        action("sound-volume", "Sound volume / " + Math.round(profile.soundVolume * 100) + "%", () -> {
            profile.soundVolume = nextVolume(profile.soundVolume); save(); rebuild();
        });
        action("music-volume", "Music volume / " + Math.round(profile.musicVolume * 100) + "%", () -> {
            profile.musicVolume = nextVolume(profile.musicVolume); game.audio().apply(); save(); rebuild();
        });
        note("Tap a volume to cycle through 0, 25, 50, 75 and 100 percent.");
        action("motion", "Visual effects / " + (profile.reducedMotion ? "Reduced" : "Full"), () -> {
            profile.reducedMotion = !profile.reducedMotion; save(); rebuild();
        });
        note("Reduced effects lowers background geometry, motion and gameplay particles for slower devices.");
        action("haptic", "Haptic feedback / " + (profile.hapticEnabled ? "On" : "Off"), () -> {
            profile.hapticEnabled = !profile.hapticEnabled; save(); rebuild();
        });
        action("shake", "Screen shake / " + (profile.screenShakeEnabled ? "On" : "Off"), () -> {
            profile.screenShakeEnabled = !profile.screenShakeEnabled; save(); rebuild();
        });
        action("contrast", "Telegraph contrast / " + (profile.highContrastTelegraphs ? "High" : "Standard"), () -> {
            profile.highContrastTelegraphs = !profile.highContrastTelegraphs; save(); rebuild();
        });
        note("Danger uses shapes and motion as well as color. High contrast adds bright warning marks.");
        action("flashes", "Flashing effects / " + (profile.reducedFlashes ? "Limited" : "Standard"), () -> {
            profile.reducedFlashes = !profile.reducedFlashes; save(); rebuild();
        });
        action("ui-scale", "UI size / " + (profile.largeUi ? "Large" : "Standard"), () -> {
            profile.largeUi = !profile.largeUi; save(); rebuild();
        });
        note("Large UI applies fully when the next screen opens.");
        action("save", "Save profile / Retry", this::save);
        if (game.platform().developmentBuild()) {
            note("Development tools");
            action("reset", confirmingReset ? "Confirm / Erase local progress" : "Reset profile", () -> {
                if (!confirmingReset) { confirmingReset = true; rebuild(); return; }
                game.saves().resetForDevelopment(game.platform().developmentBuild());
                game.audio().apply(); game.router().selectLevel(1); confirmingReset = false; rebuild();
            });
            if (confirmingReset) action("cancel-reset", "Keep profile", () -> { confirmingReset = false; rebuild(); });
        }
    }
    private static float nextVolume(float value) { return ((Math.round(value * 4) + 1) % 5) / 4f; }
}
