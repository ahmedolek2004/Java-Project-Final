package com.gym.system.util;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.util.regex.Pattern;

/**
 * Thread-safe appearance settings with persistent dark mode and accent color.
 */
public final class AppearanceSettings {
    private static final String DEFAULT_ACCENT = "#F47721";
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Preferences PREFERENCES = Preferences.userRoot().node("gym/appearance");

    private final boolean darkMode;
    private final String accentColor;

    private AppearanceSettings(boolean darkMode, String accentColor) {
        this.darkMode = darkMode;
        this.accentColor = accentColor.toUpperCase();
    }

    public static AppearanceSettings load() {
        return new AppearanceSettings(
                PREFERENCES.getBoolean("darkMode", false),
                PREFERENCES.get("accentColor", DEFAULT_ACCENT));
    }

    public void save() throws BackingStoreException {
        PREFERENCES.putBoolean("darkMode", darkMode);
        PREFERENCES.put("accentColor", accentColor);
        PREFERENCES.flush();
    }

    /**
     * Create settings from validated values.
     */
    public static AppearanceSettings create(boolean darkMode, String accentColor) {
        if (accentColor == null || !HEX_COLOR_PATTERN.matcher(accentColor).matches()) {
            throw new IllegalArgumentException("Accent color must be #RRGGBB hex format.");
        }
        return new AppearanceSettings(darkMode, accentColor.toUpperCase());
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public String accentColor() {
        return accentColor;
    }
}
