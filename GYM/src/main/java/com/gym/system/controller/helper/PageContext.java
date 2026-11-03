package com.gym.system.controller.helper;

import com.gym.system.controller.PrintController;
import com.gym.system.dao.GymDataAccess;
import com.gym.system.model.User;

import java.util.function.Consumer;
import java.util.function.Function;

public record PageContext(
        GymDataAccess repository,
        User currentUser,
        PrintController printController,
        Function<String, String> tFunction,
        Function<Double, String> moneyFormatter,
        boolean isArabic,
        boolean isDarkMode,
        Consumer<String> showInfo,
        Consumer<String> showError,
        Function<String, Boolean> confirm,
        Consumer<String> navigateTo
) {
    public String t(String key) {
        return tFunction.apply(key);
    }

    public String t(String key, String... replacements) {
        String template = tFunction.apply(key);
        for (int i = 0; i < replacements.length; i++) {
            template = template.replace("{" + i + "}", replacements[i]);
        }
        return template;
    }

    public String money(double amount) {
        return moneyFormatter.apply(amount);
    }

    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.role());
    }
}
