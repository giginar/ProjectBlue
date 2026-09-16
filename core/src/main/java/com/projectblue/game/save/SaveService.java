package com.projectblue.game.save;
import java.io.IOException;

public final class SaveService {
    private final SaveStore store;
    private final ProfileCodec codec = new ProfileCodec();
    private final Profile profile;
    private boolean recovered, writeFailed;
    public SaveService(SaveStore store) {
        this.store = store;
        Profile loaded;
        try {
            String input = store.read();
            loaded = input == null ? new Profile() : codec.decode(input);
        } catch (IOException | RuntimeException e) {
            loaded = new Profile();
            recovered = true;
        }
        profile = loaded;
    }
    public Profile profile() { return profile; }
    public boolean recovered() { return recovered; }
    public boolean writeFailed() { return writeFailed; }
    public boolean save() {
        try { store.write(codec.encode(profile)); writeFailed = false; return true; }
        catch (IOException | RuntimeException e) { writeFailed = true; return false; }
    }
}

