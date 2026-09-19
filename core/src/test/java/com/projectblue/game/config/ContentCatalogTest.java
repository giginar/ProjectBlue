package com.projectblue.game.config;
import com.projectblue.game.config.Loadout.*;
import com.projectblue.game.save.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ContentCatalogTest {
    static String json() throws IOException {
        try (InputStream input = ContentCatalogTest.class.getResourceAsStream("/config/content.json")) {
            return new String(Objects.requireNonNull(input).readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    static Stream<String> invalidContent() throws IOException {
        String j = json();
        return Stream.of("{", "{}", "",
            j.replace("\"id\": \"MANTA\"","\"id\": \"TIDE\""),
            j.replace("\"cost\": 30","\"cost\": -1"),
            j.replace("\"cost\": 60","\"cost\": 20"),
            j.replace("\"level\": 2","\"level\": 6"),
            j.replace("\"level\": 2","\"level\": 1"),
            j.replace("\"level\": 2","\"level\": 2.5"),
            j.replace("\"effect\": 20","\"effect\": 5"),
            j.replace("\"baseHealth\": 100,",""),
            j.replace("\"baseHealth\": 100","\"baseHealth\": 100, \"baseHealth\": 50"),
            j.replace("\"baseHealth\": 100","\"baseHealth\": 0"),
            j.replace("\"movementSpeed\": 1100","\"movementSpeed\": \"NaN\""),
            j.replace("\"metric\": \"ALWAYS\"","\"metric\": \"UNKNOWN\""),
            j.replace("\"behavior\": \"PULSE\"","\"behavior\": \"UNKNOWN\""),
            j.replace("\"amount\": 0.15","\"amount\": 0.9"),
            j.replace("\"schemaVersion\": 1","\"schemaVersion\": 99"));
    }
    @ParameterizedTest @MethodSource("invalidContent")
    void rejectsInvalidContentAndLogsSafeFallback(String broken) {
        assertThrows(RuntimeException.class,() -> ContentCatalog.parse(broken));
        List<String> messages = new ArrayList<>();
        ContentCatalog fallback = ContentCatalog.load(new ByteArrayInputStream(broken.getBytes(StandardCharsets.UTF_8)),messages::add);
        assertTrue(fallback.fallback); assertEquals(1,messages.size());
        assertTrue(messages.get(0).contains("config/content.json rejected"));
        assertEquals(100,fallback.submarine("TIDE").baseHealth());
        assertEquals(5,fallback.upgrade("HULL").maxLevel());
        assertEquals(1000,fallback.achievement("RECYCLER_II").target());
    }
    @Test void missingAndOversizedConfigFallBack() {
        assertTrue(ContentCatalog.load(null,s -> {}).fallback);
        assertTrue(ContentCatalog.load(new ByteArrayInputStream(new byte[131073]),s -> {}).fallback);
    }
    @Test void shippedDefinitionsAreValidImmutableAndComplete() {
        ContentCatalog c = ContentCatalog.DEFAULT;
        assertFalse(c.fallback);
        for (Submarine s : Submarine.values()) assertNotNull(c.submarine(s.name()));
        for (Pilot p : Pilot.values()) {
            var def = c.pilot(p.name()); assertTrue(def.passives().size() <= 2);
            assertThrows(UnsupportedOperationException.class,() -> def.passives().clear());
        }
        for (Upgrade u : Upgrade.values()) assertEquals(5,c.upgrade(u.name()).maxLevel());
        for (Weapon w : Weapon.values()) assertNotNull(c.weapon(w.name()));
        for (Achievement a : Achievement.values()) assertNotNull(c.achievement(a.name()));
    }
    @Test void tunedCostsAndEffectsReachProfileAndGameplayWithoutCodeChanges() throws IOException {
        ContentCatalog c = ContentCatalog.parse(json().replace("\"cost\": 30","\"cost\": 7").replace("\"effect\": 10","\"effect\": 11"));
        SaveService saves = new SaveService(new SaveStore() {
            public String read() { return null; }
            public void write(String value) {}
        },c);
        saves.profile().totalSalvage = 7;
        assertEquals(SaveService.PurchaseResult.PURCHASED,saves.purchase(Upgrade.HULL));
        assertEquals(0,saves.profile().totalSalvage); assertEquals(111,Loadout.from(saves.profile()).health());
    }
}
