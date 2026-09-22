package com.projectblue.game.i18n;

import java.util.Locale;

/** Stable persisted language identifiers. Enum names must never be persisted. */
public enum GameLanguage {
    ENGLISH("en", "English"), TURKISH("tr", "Türkçe");
    private final String id, displayName;
    GameLanguage(String id, String displayName) { this.id = id; this.displayName = displayName; }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public static GameLanguage fromId(String id) {
        if (id != null) for (GameLanguage value : values()) if (value.id.equals(id)) return value;
        return null;
    }
    public static GameLanguage suggested(Locale locale) {
        return locale != null && "tr".equalsIgnoreCase(locale.getLanguage()) ? TURKISH : ENGLISH;
    }
}
