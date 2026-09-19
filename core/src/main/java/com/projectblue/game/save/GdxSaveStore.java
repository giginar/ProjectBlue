package com.projectblue.game.save;

import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import static com.projectblue.game.config.GameConfig.*;

public final class GdxSaveStore implements SaveStore {
    private final FileHandle file, temp, backup;
    public GdxSaveStore(FileHandle directory) {
        file = directory.child(SAVE_FILE);
        temp = directory.child(SAVE_FILE + ".tmp");
        backup = directory.child(SAVE_FILE + ".bak");
    }
    public String read() throws IOException {
        // Recover an interrupted rename; SaveService also checks the backup after checksum failure.
        FileHandle source = file.exists() ? file : backup;
        if (!source.exists()) return null;
        if (source.length() > MAX_PROFILE_LENGTH) throw new IOException("Oversized profile");
        return source.readString("UTF-8");
    }
    public String readBackup() throws IOException {
        if (!backup.exists()) return null;
        if (backup.length() > MAX_PROFILE_LENGTH) throw new IOException("Oversized backup");
        return backup.readString("UTF-8");
    }
    public void write(String contents) throws IOException {
        try {
            file.parent().mkdirs();
            temp.writeString(contents, false, "UTF-8");
            new ProfileCodec().decode(temp.readString("UTF-8"));
            try (FileChannel channel = FileChannel.open(temp.file().toPath(), StandardOpenOption.WRITE)) { channel.force(true); }
            if (file.exists()) {
                try {
                    // Never replace a good backup with a corrupt primary profile.
                    new ProfileCodec().decode(read());
                    file.copyTo(backup);
                } catch (IOException ignored) { /* Keep the last verified backup. */ }
            }
            try {
                Files.move(temp.file().toPath(), file.file().toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                throw new IOException("Storage does not support atomic profile replacement", e);
            }
        } catch (RuntimeException e) { throw new IOException("Profile write failed", e); }
    }
}
