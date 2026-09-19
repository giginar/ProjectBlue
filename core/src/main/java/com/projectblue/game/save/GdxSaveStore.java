package com.projectblue.game.save;

import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import static com.projectblue.game.config.GameConfig.*;

public final class GdxSaveStore implements SaveStore {
    private final FileHandle file, temp, backup, backupTemp;
    public GdxSaveStore(FileHandle directory) {
        file = directory.child(SAVE_FILE);
        temp = directory.child(SAVE_FILE + ".tmp");
        backup = directory.child(SAVE_FILE + ".bak");
        backupTemp = directory.child(SAVE_FILE + ".bak.tmp");
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
            writeVerified(temp, contents);
            if (file.exists()) {
                String previous = null;
                try {
                    // Never replace a good backup with a corrupt primary profile.
                    String candidate = file.readString("UTF-8");
                    new ProfileCodec().decode(candidate);
                    previous = candidate;
                } catch (IOException ignored) { /* Keep the last verified backup. */ }
                if (previous != null) {
                    // If creating the next backup fails, abort before touching the good primary.
                    writeVerified(backupTemp, previous);
                    replace(backupTemp.file().toPath(), backup.file().toPath());
                }
            }
            replace(temp.file().toPath(), file.file().toPath());
        } catch (RuntimeException e) { throw new IOException("Profile write failed", e); }
    }
    private static void writeVerified(FileHandle target, String contents) throws IOException {
        target.writeString(contents, false, "UTF-8");
        new ProfileCodec().decode(target.readString("UTF-8"));
        try (FileChannel channel = FileChannel.open(target.file().toPath(), StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }
    private static void replace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // The verified backup remains readable if a non-atomic primary replacement is interrupted.
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
