package com.projectblue.game.audio;

import com.projectblue.game.events.GameEvents;
import com.projectblue.game.save.Profile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioServiceTest {
    private static final class Track implements AudioService.MusicControl {
        boolean playing,looping; float volume; int plays,pauses;
        public void setLooping(boolean looping) { this.looping=looping; }
        public void setVolume(float volume) { this.volume=volume; }
        public boolean isPlaying() { return playing; }
        public void play() { playing=true; plays++; }
        public void pause() { playing=false; pauses++; }
    }
    @Test void attachStartsAudibleLoopAndApplyDoesNotDuplicatePlayback() {
        Profile profile=new Profile(); AudioService audio=new AudioService(profile); Track track=new Track();
        audio.attach(track); audio.apply(); audio.apply();
        assertTrue(track.looping); assertTrue(track.playing); assertEquals(profile.musicVolume,track.volume,.001f);
        assertEquals(1,track.plays);
    }
    @Test void togglePauseResumeAndPersistenceStateControlRealPlayback() {
        Profile profile=new Profile(); AudioService audio=new AudioService(profile); Track track=new Track(); audio.attach(track);
        audio.toggleMusic(); assertFalse(profile.musicEnabled); assertFalse(track.playing); assertEquals(1,track.pauses);
        audio.resume(); assertFalse(track.playing);
        audio.toggleMusic(); assertTrue(track.playing);
        audio.suspend(); assertFalse(track.playing);
        audio.resume(); assertTrue(track.playing); assertEquals(3,track.plays);
    }
    @Test void missingMusicFailsSafeAndSoundSettingStaysIndependent() {
        Profile profile=new Profile(); AudioService audio=new AudioService(profile);
        assertDoesNotThrow(() -> audio.attach((AudioService.MusicControl)null));
        boolean music=profile.musicEnabled; audio.toggleSound();
        assertFalse(profile.soundEnabled); assertEquals(music,profile.musicEnabled);
        assertDoesNotThrow(() -> audio.onEvent(GameEvents.Type.SHOT,0,0,0));
    }
}
