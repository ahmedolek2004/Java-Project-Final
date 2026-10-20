package com.gym.system.model;

public record Activity(int id, String nameArabic, String nameEnglish, String description) {
    public Activity {
        if (nameArabic == null || nameArabic.isBlank() || nameEnglish == null || nameEnglish.isBlank()) {
            throw new IllegalArgumentException("Activity names are required in both languages.");
        }
    }
}
