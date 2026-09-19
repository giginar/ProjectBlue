package com.projectblue.game.save;
import java.io.IOException;
public interface SaveStore {
    /** Null means no previous profile. */
    String read() throws IOException;
    default String readBackup() throws IOException { return null; }
    /** Commit a whole profile; on failure the previously committed primary must remain readable. */
    void write(String contents) throws IOException;
}
