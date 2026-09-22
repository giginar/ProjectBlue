package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;

public final class SettingsScreen extends StageMenuScreen {
    private boolean confirmingReset;
    private boolean privacyRequired;
    @Override public void render(float delta) {
        if (privacyRequired != game.platform().consent().isPrivacyOptionsRequired()) rebuild();
        super.render(delta);
    }
    public SettingsScreen(ProjectBlueGame game) { super(game, game.i18n().text("settings.title"), game.i18n().text("settings.subtitle")); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        privacyRequired = game.platform().consent().isPrivacyOptionsRequired();
        action("language", t("settings.language", game.i18n().language().displayName()), () -> {
            var next = game.i18n().language() == com.projectblue.game.i18n.GameLanguage.ENGLISH ? com.projectblue.game.i18n.GameLanguage.TURKISH : com.projectblue.game.i18n.GameLanguage.ENGLISH;
            game.chooseLanguage(next); game.router().request(ScreenRouter.Route.SETTINGS);
        });
        if (privacyRequired) action("privacy-options", t("settings.privacy_options"), () -> game.platform().consent().showPrivacyOptions(() -> {}));
        route("privacy", t("settings.privacy_info"), ScreenRouter.Route.PRIVACY);
        action("mute", t("settings.master", profile.muted ? t("common.off") : t("common.on")), () -> { game.audio().toggleMute(); save(); rebuild(); });
        action("sound", t("settings.sound", profile.soundEnabled ? t("common.on") : t("common.off")), () -> { game.audio().toggleSound(); save(); rebuild(); });
        action("music", t("settings.music", profile.musicEnabled ? t("common.on") : t("common.off")), () -> { game.audio().toggleMusic(); save(); rebuild(); });
        action("sound-volume", t("settings.sound_volume", Math.round(profile.soundVolume * 100)), () -> {
            profile.soundVolume = nextVolume(profile.soundVolume); save(); rebuild();
        });
        action("music-volume", t("settings.music_volume", Math.round(profile.musicVolume * 100)), () -> {
            profile.musicVolume = nextVolume(profile.musicVolume); game.audio().apply(); save(); rebuild();
        });
        note(t("settings.volume_help"));
        action("motion", t("settings.visual", profile.reducedMotion ? t("settings.reduced") : t("settings.full")), () -> {
            profile.reducedMotion = !profile.reducedMotion; save(); rebuild();
        });
        note(t("settings.motion_help"));
        action("haptic", t("settings.haptic", profile.hapticEnabled ? t("common.on") : t("common.off")), () -> {
            profile.hapticEnabled = !profile.hapticEnabled; save(); rebuild();
        });
        action("shake", t("settings.shake", profile.screenShakeEnabled ? t("common.on") : t("common.off")), () -> {
            profile.screenShakeEnabled = !profile.screenShakeEnabled; save(); rebuild();
        });
        action("contrast", t("settings.contrast", profile.highContrastTelegraphs ? t("settings.high") : t("settings.standard")), () -> {
            profile.highContrastTelegraphs = !profile.highContrastTelegraphs; save(); rebuild();
        });
        note(t("settings.contrast_help"));
        action("flashes", t("settings.flashes", profile.reducedFlashes ? t("settings.limited") : t("settings.standard")), () -> {
            profile.reducedFlashes = !profile.reducedFlashes; save(); rebuild();
        });
        action("ui-scale", t("settings.ui", profile.largeUi ? t("settings.large") : t("settings.standard")), () -> {
            profile.largeUi = !profile.largeUi; save(); rebuild();
        });
        note(t("settings.ui_help"));
        action("save", t("settings.save"), this::save);
        if (game.platform().developmentBuild()) {
            note(t("settings.dev"));
            action("reset", confirmingReset ? t("settings.confirm_reset") : t("settings.reset"), () -> {
                if (!confirmingReset) { confirmingReset = true; rebuild(); return; }
                game.saves().resetForDevelopment(game.platform().developmentBuild());
                game.audio().apply(); game.router().selectLevel(1); confirmingReset = false; rebuild();
            });
            if (confirmingReset) action("cancel-reset", t("settings.keep"), () -> { confirmingReset = false; rebuild(); });
        }
    }
    private static float nextVolume(float value) { return ((Math.round(value * 4) + 1) % 5) / 4f; }
}
