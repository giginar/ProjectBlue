package com.projectblue.game.save;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.projectblue.game.config.GameConfig.*;

class SaveServiceTest {
    private static final class MemoryStore implements SaveStore {
        String data; boolean fail;
        public String read() { return data; }
        public void write(String contents) throws IOException { if(fail)throw new IOException("Full storage"); data=contents; }
    }
    @Test void defaultsAreReadyForFirstLaunch() {
        Profile p=new SaveService(new MemoryStore()).profile();
        assertEquals(PROFILE_VERSION,p.version); assertTrue(p.soundEnabled); assertTrue(p.musicEnabled);
        assertEquals(DEFAULT_SOUND_VOLUME,p.soundVolume); assertEquals(DEFAULT_MUSIC_VOLUME,p.musicVolume);
        assertEquals(0,p.bestScore); assertEquals(0,p.bestStars); assertEquals(0,p.totalSalvage); assertEquals(0,p.completedRuns);
    }
    @Test void roundTripRetainsSettingsAndProgress() {
        MemoryStore store=new MemoryStore(); SaveService first=new SaveService(store);
        first.profile().soundEnabled=false; first.profile().bestScore=12345; first.profile().bestStars=3;
        first.profile().totalSalvage=45; first.profile().completedRuns=2;
        assertTrue(first.save());
        SaveService second=new SaveService(store);
        assertFalse(second.recovered()); assertFalse(second.profile().soundEnabled);
        assertEquals(12345,second.profile().bestScore); assertEquals(45,second.profile().totalSalvage);
    }
    @Test void corruptTruncatedAndUnknownFutureProfilesRecoverToDefaults() {
        for(String content:new String[]{"not a profile","version=1\nbestScore=9","version=99\n",""}) {
            MemoryStore store=new MemoryStore(); store.data=content;
            SaveService service=new SaveService(store);
            assertTrue(service.recovered()); assertEquals(0,service.profile().bestScore); assertTrue(service.profile().musicEnabled);
        }
    }
    @Test void checksumDetectsWellFormedButCorruptedValues() {
        MemoryStore store=new MemoryStore(); SaveService service=new SaveService(store); service.save();
        store.data=store.data.replace("bestScore=0","bestScore=1000");
        assertTrue(new SaveService(store).recovered());
    }
    @Test void migratesVersionZeroWithoutInventingProgress() {
        MemoryStore store=new MemoryStore();
        store.data="version=0\nsoundEnabled=false\nmusicEnabled=true\nbestScore=250";
        SaveService service=new SaveService(store);
        assertFalse(service.recovered()); assertEquals(PROFILE_VERSION,service.profile().version);
        assertEquals(250,service.profile().bestScore); assertEquals(0,service.profile().bestStars);
        service.save(); assertTrue(store.data.startsWith("version=1"));
    }
    @Test void unavailableStorageKeepsGameUsableAndReportsFailure() {
        MemoryStore store=new MemoryStore(); store.fail=true;
        SaveService service=new SaveService(store);
        assertFalse(service.save()); assertTrue(service.writeFailed());
        assertNotNull(service.profile());
        store.fail=false; assertTrue(service.save()); assertFalse(service.writeFailed());
    }
}

