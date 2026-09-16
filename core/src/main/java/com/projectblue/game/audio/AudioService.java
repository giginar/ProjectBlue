package com.projectblue.game.audio;

import com.projectblue.game.assets.GameAssets;
import com.projectblue.game.save.Profile;
import com.projectblue.game.events.GameEvents;

/** Settings are persisted by SaveService; AssetManager owns the audio resources. */
public final class AudioService implements GameEvents.Listener {
    private final Profile profile;
    private GameAssets assets;
    private boolean suspended;
    public AudioService(Profile profile) { this.profile = profile; }
    public void attach(GameAssets assets) {
        this.assets = assets;
        assets.ocean().setLooping(true);
        apply();
    }
    public void apply() {
        if (assets == null) return;
        assets.ocean().setVolume(profile.musicVolume);
        if (profile.musicEnabled && !suspended) assets.ocean().play();
        else assets.ocean().pause();
    }
    public void suspend() {
        suspended = true;
        if (assets != null) { assets.pulse().stop(); assets.collect().stop(); }
        apply();
    }
    public void resume() { suspended = false; apply(); }
    public void toggleSound() { profile.soundEnabled = !profile.soundEnabled; apply(); }
    public void toggleMusic() { profile.musicEnabled = !profile.musicEnabled; apply(); }
    public void onEvent(GameEvents.Type type, float x, float y, int value) {
        if (assets == null || suspended || !profile.soundEnabled) return;
        switch (type) {
            case SHOT -> assets.pulse().play(profile.soundVolume * .3f);
            case PLASTIC_COLLECTED, TURTLE_RESCUED, SALVAGE_COLLECTED -> assets.collect().play(profile.soundVolume);
            default -> { }
        }
    }
}

