package com.gym.system.controller.helper;

import com.gym.system.controller.DashboardController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class NavigationHelper {
    private NavigationHelper() {}

    public static String sectionIcon(String section) {
        return switch (section) {
            case "dashboard" -> "⌂";
            case "members" -> "♙";
            case "subscriptions" -> "★";
            case "attendance" -> "✓";
            case "schedule" -> "📅";
            case "coaches" -> "♟";
            case "activities" -> "▦";
            case "finance" -> "$";
            case "payroll" -> "💳";
            case "team" -> "👥";
            default -> "⚙";
        };
    }

    public static List<DashboardController.NavigationItem> createNavigationItems(
            String role, Function<String, String> t, String[] sections) {
        List<DashboardController.NavigationItem> items = new ArrayList<>();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        for (String section : sections) {
            if (("team".equals(section) || "finance".equals(section) || "payroll".equals(section)) && !isAdmin) {
                continue;
            }
            items.add(new DashboardController.NavigationItem(
                    section, t.apply("nav." + section), sectionIcon(section)));
        }
        return items;
    }
}
