package com.projectblue.game.screens;

import com.projectblue.game.ProjectBlueGame;

public final class SettingsScreen extends StageMenuScreen {
    private boolean confirmingReset;
    public SettingsScreen(ProjectBlueGame game) { super(game, "Settings", "Sound and comfort / stored on this device"); rebuild(); }
    private void rebuild() {
        body.clearChildren();
        action("sound", "Sound / " + (profile.soundEnabled ? "On" : "Off"), () -> { game.audio().toggleSound(); save(); rebuild(); });
        action("music", "Music / " + (profile.musicEnabled ? "On" : "Off"), () -> { game.audio().toggleMusic(); save(); rebuild(); });
        action("sound-volume", "Sound volume / " + Math.round(profile.soundVolume * 100) + "%", () -> {
            profile.soundVolume = nextVolume(profile.soundVolume); save(); rebuild();
        });
        action("music-volume", "Music volume / " + Math.round(profile.musicVolume * 100) + "%", () -> {
            profile.musicVolume = nextVolume(profile.musicVolume); game.audio().apply(); save(); rebuild();
        });
        note("Tap a volume to cycle through 0, 25, 50, 75 and 100 percent.");
        action("motion", "Menu motion / " + (profile.reducedMotion ? "Reduced" : "Full"), () -> {
            profile.reducedMotion = !profile.reducedMotion; save(); rebuild();
        });
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
