package com.projectblue.game.assets;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class TurkishGlyphTest {
    @Test void bitmapFontContainsEveryTurkishGlyph() throws Exception {
        String font = Files.readString(Path.of("../assets/fonts/blue.fnt"));
        for (int codePoint : "çÇğĞıİöÖşŞüÜ".codePoints().toArray())
            assertTrue(font.contains("char id=" + codePoint + " "), "missing glyph " + new String(Character.toChars(codePoint)));
    }
}
