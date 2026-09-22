package com.projectblue.game.i18n;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

/** UTF-8 translation catalog with English fallback and stable dot-separated keys. */
public final class Localization {
    private static final String ROOT = "/i18n/strings_";
    private final Properties english;
    private Properties active;
    private GameLanguage language;
    public Localization(String persistedLanguage) { english = load(GameLanguage.ENGLISH); setLanguage(GameLanguage.fromId(persistedLanguage)); }
    public GameLanguage language() { return language; }
    public void setLanguage(GameLanguage requested) {
        language = requested == null ? GameLanguage.ENGLISH : requested;
        active = language == GameLanguage.ENGLISH ? english : load(language);
    }
    public String text(String key, Object... arguments) {
        String pattern = active.getProperty(key, english.getProperty(key));
        if (pattern == null) return "[" + key + "]";
        return arguments.length == 0 ? pattern : new MessageFormat(pattern, Locale.ROOT).format(arguments);
    }
    public boolean hasCompleteTranslations() { return active.stringPropertyNames().containsAll(english.stringPropertyNames()); }
    public Set<String> keys() { return Collections.unmodifiableSet(english.stringPropertyNames()); }
    private static Properties load(GameLanguage language) {
        String path = ROOT + language.id() + ".properties";
        try (InputStream source = Localization.class.getResourceAsStream(path)) {
            if (source == null) throw new IllegalStateException("Missing localization resource: " + path);
            Properties result = new Properties(); result.load(new InputStreamReader(source, StandardCharsets.UTF_8)); return result;
        } catch (IOException e) { throw new IllegalStateException("Cannot read localization resource: " + path, e); }
    }
}
