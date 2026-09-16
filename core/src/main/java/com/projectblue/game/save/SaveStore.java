package com.projectblue.game.save;
import java.io.IOException;
public interface SaveStore {
    /** Null means no previous profile. */
    String read() throws IOException;
    void write(String contents) throws IOException;
}

