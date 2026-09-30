package com.projectblue.game.assets;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.io.StringReader;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TurkishGlyphTest {
    @Test void bitmapFontContainsEveryTurkishGlyph() throws Exception {
        String font = Files.readString(Path.of("../assets/fonts/blue.fnt"));
        for (int codePoint : "çÇğĞıİöÖşŞüÜ".codePoints().toArray())
            assertTrue(font.contains("char id=" + codePoint + " "), "missing glyph " + new String(Character.toChars(codePoint)));
    }
    @Test void fontCoversEveryCharacterUsedByBothCatalogs() throws Exception {
        String font=Files.readString(Path.of("../assets/fonts/blue.fnt")); Set<Integer> ids=new HashSet<>();
        for(String line:font.lines().toList()) if(line.startsWith("char id="))
            ids.add(Integer.parseInt(line.substring(8,line.indexOf(' ',8))));
        for(String catalog:List.of("strings_en.properties","strings_tr.properties")) {
            Properties values=new Properties();
            values.load(new StringReader(Files.readString(Path.of("src/main/resources/i18n/"+catalog))));
            for(String value:values.stringPropertyNames().stream().map(values::getProperty).toList())
                value.codePoints().filter(c -> !Character.isWhitespace(c)).forEach(c -> assertTrue(ids.contains(c),catalog+" missing "+new String(Character.toChars(c))));
        }
        for(int codePoint:"0123456789.,%/-:()".codePoints().toArray()) assertTrue(ids.contains(codePoint));
    }
    @Test void descriptorDeclaresTheDeterministicBoldAtlasGeometry() throws Exception {
        String font=Files.readString(Path.of("../assets/fonts/blue.fnt"));
        assertTrue(font.startsWith("info face=\"Blue Grid\" size=21 bold=1"));
        assertTrue(font.contains("scaleW=324 scaleH=189")); assertTrue(font.contains("chars count=109"));
    }
}
