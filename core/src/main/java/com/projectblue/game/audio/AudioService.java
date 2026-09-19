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
        if (assets.ocean() != null) assets.ocean().setLooping(true);
        apply();
    }
    public void apply() {
        if (assets == null) return;
        if (assets.ocean() == null) return;
        assets.ocean().setVolume(profile.muted ? 0 : profile.musicVolume);
        if (!profile.muted && profile.musicEnabled && !suspended) assets.ocean().play();
        else assets.ocean().pause();
    }
    public void suspend() {
        suspended = true;
        if (assets != null) {
            if (assets.pulse() != null) assets.pulse().stop();
            if (assets.collect() != null) assets.collect().stop();
        }
        apply();
    }
    public void resume() { suspended = false; apply(); }
    public void toggleSound() { profile.soundEnabled = !profile.soundEnabled; apply(); }
    public void toggleMusic() { profile.musicEnabled = !profile.musicEnabled; apply(); }
    public void toggleMute() { profile.muted = !profile.muted; apply(); }
    public void onEvent(GameEvents.Type type, float x, float y, int value) {
        if (assets == null || suspended || profile.muted || !profile.soundEnabled) return;
        switch (type) {
            case SHOT -> play(assets.pulse(),profile.soundVolume * .3f);
            case SONAR_PULSE -> play(assets.pulse(),profile.soundVolume * .75f);
            case PLASTIC_COLLECTED, TURTLE_RESCUED, SALVAGE_COLLECTED -> play(assets.collect(),profile.soundVolume);
            default -> { }
        }
    }
    private static void play(com.badlogic.gdx.audio.Sound sound,float volume) { if (sound != null) sound.play(volume); }
}
