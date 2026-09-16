package com.projectblue.game.save;

import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import static com.projectblue.game.config.GameConfig.*;

public final class GdxSaveStore implements SaveStore {
    private final FileHandle file, temp, backup;
    public GdxSaveStore(FileHandle directory) {
        file = directory.child(SAVE_FILE);
        temp = directory.child(SAVE_FILE + ".tmp");
        backup = directory.child(SAVE_FILE + ".bak");
    }
    public String read() throws IOException {
        // Recover an interrupted rename, but a corrupt current file falls back to defaults in SaveService.
        FileHandle source = file.exists() ? file : backup;
        if (!source.exists()) return null;
        if (source.length() > MAX_PROFILE_LENGTH) throw new IOException("Oversized profile");
        return source.readString("UTF-8");
    }
    public void write(String contents) throws IOException {
        try {
            file.parent().mkdirs();
            temp.writeString(contents, false, "UTF-8");
            if (file.exists()) file.copyTo(backup);
            temp.moveTo(file);
        } catch (RuntimeException e) { throw new IOException("Profile write failed", e); }
    }
}
