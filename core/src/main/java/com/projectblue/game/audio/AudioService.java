package com.projectblue.game.audio;

import com.projectblue.game.assets.GameAssets;
import com.projectblue.game.save.Profile;
import com.projectblue.game.events.GameEvents;

/** Settings are persisted by SaveService; AssetManager owns the audio resources. */
public final class AudioService implements GameEvents.Listener {
    interface MusicControl {
        void setLooping(boolean looping);
        void setVolume(float volume);
        boolean isPlaying();
        void play();
        void pause();
    }
    private final Profile profile;
    private GameAssets assets;
    private MusicControl music;
    private boolean suspended;
    public AudioService(Profile profile) { this.profile = profile; }
    public void attach(GameAssets assets) {
        this.assets = assets;
        var track=assets.ocean();
        music=track==null?null:new MusicControl() {
            public void setLooping(boolean looping) { track.setLooping(looping); }
            public void setVolume(float volume) { track.setVolume(volume); }
            public boolean isPlaying() { return track.isPlaying(); }
            public void play() { track.play(); }
            public void pause() { track.pause(); }
        };
        if (music!=null) music.setLooping(true);
        apply();
    }
    void attach(MusicControl music) { this.music=music; if(music!=null) music.setLooping(true); apply(); }
    public void apply() {
        if (music==null) return;
        music.setVolume(profile.muted ? 0 : profile.musicVolume);
        boolean shouldPlay=!profile.muted&&profile.musicEnabled&&!suspended;
        if (shouldPlay&&!music.isPlaying()) music.play();
        else if (!shouldPlay&&music.isPlaying()) music.pause();
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
