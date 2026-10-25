package com.gym.system.controller.helper;

import com.gym.system.util.AppearanceSettings;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.util.Locale;
import java.util.function.Function;

public final class AppearanceHelper {
    private AppearanceHelper() {}

    public static void applyAppearance(Node root, AppearanceSettings appearance) {
        if (root == null || appearance == null) return;
        root.getStyleClass().remove("dark-theme");
        if (appearance.isDarkMode()) {
            root.getStyleClass().add("dark-theme");
        }
        Color accent = Color.web(appearance.accentColor());
        root.setStyle("-gym-accent: " + appearance.accentColor()
                + "; -gym-accent-hover: " + colorHex(accent.deriveColor(0, 0.9, 0.82, 1))
                + "; -gym-accent-soft: " + colorHex(mixWithWhite(accent, 0.9)) + ";");
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
}
