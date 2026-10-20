package com.gym.system.util;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton language manager for bidirectional (Arabic/English) translations.
 */
public final class LanguageManager {
    private static final String BUNDLE_NAME = "com.gym.system.messages";
    
    private volatile Locale locale = Locale.ENGLISH;
    private volatile ResourceBundle messages;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private LanguageManager() {
        refreshBundle();
    }

    public static LanguageManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Supported language codes: "en", "ar"
     */
    public Locale locale() {
        return locale;
    }

    /**
     * Set application language. Thread-safe.
     * @param tag Language code ("en" or "ar")
     */
    public void setLanguage(String tag) {
        if (tag == null || !"ar".equals(tag) && !"en".equals(tag)) {
            throw new IllegalArgumentException("Language must be 'en' or 'ar'.");
        }
        locale = Locale.forLanguageTag(tag);
        refreshBundle();
    }

    private void refreshBundle() {
        messages = ResourceBundle.getBundle(BUNDLE_NAME, locale);
        cache.clear();
    }

    /**
     * Get translated text. Returns key if translation missing (fail-fast).
     * @param key Translation key
     * @return Translated text or original key
     */
    public String text(String key) {
        return cache.computeIfAbsent(key, messages::getString);
    }

    /**
     * Format with arguments, supports Arabic/English.
     */
    public String format(String key, Object... args) {
        return String.format(locale == Locale.ENGLISH ? Locale.US : Locale.getDefault(), text(key), args);
    }

    private static final class SingletonHolder {
        private static final LanguageManager INSTANCE = new LanguageManager();
    }
}
