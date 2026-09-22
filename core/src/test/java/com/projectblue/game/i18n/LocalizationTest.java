package com.projectblue.game.i18n;

import com.projectblue.game.save.*;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    @Test void firstLaunchRequiresExplicitChoiceAndLocaleOnlySuggests() {
        assertEquals("", new Profile().language);
        assertEquals(GameLanguage.TURKISH, GameLanguage.suggested(Locale.forLanguageTag("tr-TR")));
        assertEquals(GameLanguage.ENGLISH, GameLanguage.suggested(Locale.GERMAN));
    }
    @Test void englishAndTurkishSwitchWithoutRestart() {
        Localization text = new Localization("en");
        assertEquals("Play", text.text("menu.play"));
        text.setLanguage(GameLanguage.TURKISH);
        assertEquals("Oyna", text.text("menu.play"));
    }
    @Test void catalogsAreCompleteAndUnknownKeysAreVisible() {
        Localization text = new Localization("tr");
        assertTrue(text.hasCompleteTranslations());
        assertEquals("[missing.test.key]", text.text("missing.test.key"));
    }
    @Test void invalidPersistedIdentifierFallsBackAndIsNormalizedForNewChoice() {
        Localization text = new Localization("invalid");
        assertEquals(GameLanguage.ENGLISH, text.language());
        Profile profile = new Profile(); profile.language = "invalid"; profile.normalize();
        assertEquals("", profile.language);
    }
    @Test void languageRoundTripsWithoutChangingProgression() throws Exception {
        Profile profile = new Profile(); profile.language = "tr"; profile.totalSalvage = 42;
        Profile decoded = new ProfileCodec().decode(new ProfileCodec().encode(profile));
        assertEquals("tr", decoded.language); assertEquals(42, decoded.totalSalvage); assertTrue(decoded.level(1).unlocked);
    }
}
