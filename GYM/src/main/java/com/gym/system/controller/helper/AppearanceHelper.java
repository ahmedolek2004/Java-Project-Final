package com.gym.system.controller.helper;

import com.gym.system.util.AppearanceSettings;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.function.Function;

public final class AppearanceHelper {
    private static final Color LIGHT_SURFACE = Color.WHITE;
    private static final Color DARK_SURFACE = Color.web("#131B2E");
    private static final Color SIDEBAR_SURFACE = Color.web("#1E293B");
    private static final Color INK = Color.web("#0F172A");
    private static final double MIN_TEXT_CONTRAST = 4.5;
    private static final double MIN_ON_ACCENT_WHITE_CONTRAST = 2.5;

    private AppearanceHelper() {}

    public static void applyAppearance(Node root, AppearanceSettings appearance) {
        if (root == null || appearance == null) return;
        root.getStyleClass().remove("dark-theme");
        if (appearance.isDarkMode()) {
            root.getStyleClass().add("dark-theme");
        }
        root.setStyle(accentStyle(appearance.accentColor(), appearance.isDarkMode()));
    }

    public static void applyAccent(Node root, boolean darkMode) {
        if (root == null) return;
        root.setStyle(accentStyle(AppearanceSettings.load().accentColor(), darkMode));
    }

    public static String accentStyle(String accentHex, boolean darkMode) {
        Color accent = Color.web(accentHex);
        Color hover = accent.deriveColor(0, 0.9, 0.82, 1);
        Color soft = darkMode ? blend(accent, DARK_SURFACE, 0.8) : mixWithWhite(accent, 0.9);
        Color onAccent = contrast(Color.WHITE, accent) >= MIN_ON_ACCENT_WHITE_CONTRAST ? Color.WHITE : INK;
        Color textOnSurface = readableOn(accent, darkMode ? DARK_SURFACE : LIGHT_SURFACE);
        Color textOnDark = readableOn(accent, SIDEBAR_SURFACE);
        return "-gym-accent: " + colorHex(accent)
                + "; -gym-accent-hover: " + colorHex(hover)
                + "; -gym-accent-soft: " + colorHex(soft)
                + "; -gym-accent-glow: " + rgba(accent, 0.28)
                + "; -gym-on-accent: " + colorHex(onAccent)
                + "; -gym-accent-text: " + colorHex(textOnSurface)
                + "; -gym-accent-text-on-dark: " + colorHex(textOnDark) + ";";
    }

    public static Color mixWithWhite(Color color, double whiteAmount) {
        return new Color(
                color.getRed() * (1 - whiteAmount) + whiteAmount,
                color.getGreen() * (1 - whiteAmount) + whiteAmount,
                color.getBlue() * (1 - whiteAmount) + whiteAmount,
                1
        );
    }

    public static String colorHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    public static String themeButtonText(AppearanceSettings appearance, Function<String, String> t) {
        return appearance.isDarkMode() ? t.apply("appearance.lightMode") : t.apply("appearance.darkMode");
    }

    public static String languageButtonText(boolean isArabic) {
        return isArabic ? "English" : "العربية";
    }

    private static Color blend(Color base, Color target, double amount) {
        return new Color(
                base.getRed() * (1 - amount) + target.getRed() * amount,
                base.getGreen() * (1 - amount) + target.getGreen() * amount,
                base.getBlue() * (1 - amount) + target.getBlue() * amount,
                1
        );
    }

    private static Color readableOn(Color color, Color surface) {
        Color target = luminance(surface) > 0.5 ? Color.BLACK : Color.WHITE;
        Color candidate = color;
        for (int step = 0; step < 24 && contrast(candidate, surface) < MIN_TEXT_CONTRAST; step++) {
            candidate = blend(candidate, target, 0.08);
        }
        return candidate;
    }

    private static double contrast(Color first, Color second) {
        double a = luminance(first);
        double b = luminance(second);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * channel(color.getRed())
                + 0.7152 * channel(color.getGreen())
                + 0.0722 * channel(color.getBlue());
    }

    private static double channel(double value) {
        return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static String rgba(Color color, double alpha) {
        return String.format(Locale.ROOT, "rgba(%d, %d, %d, %.2f)",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                alpha);
    }
}
