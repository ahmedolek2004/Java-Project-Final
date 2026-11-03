package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Activity;

import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ActivityForm {
    private ActivityForm() {}

    public static Optional<Activity> showAdd(boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("activity.add"), null, isDark, isArabic, t);
    }

    public static Optional<Activity> showEdit(GymRepository.ActivityRow existing,
                                               boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("activity.edit"), existing, isDark, isArabic, t);
    }

    private static Optional<Activity> showForm(String title, GymRepository.ActivityRow existing,
                                                boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField arField = new TextField(existing == null ? "" : existing.nameArabic());
        TextField enField = new TextField(existing == null ? "" : existing.nameEnglish());
        TextField descField = new TextField();

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("activity.arabic"), arField);
        FormGrid.addRow(grid, 1, t.apply("activity.english"), enField);
        FormGrid.addRow(grid, 2, t.apply("finance.description"), descField);

        boolean saved = WizardDialog.show(title, title, isDark, isArabic, t, List.of(
                new WizardDialog.Step(title, grid, () -> {
                    if (arField.getText().isBlank() || enField.getText().isBlank()) {
                        return isArabic ? "يرجى إدخال الاسم بالعربية والإنجليزية." :
                                "Please enter both Arabic and English names.";
                    }
                    return null;
                })
        ));
        if (!saved) return Optional.empty();

        return Optional.of(new Activity(existing == null ? 0 : existing.id(),
                arField.getText().trim(), enField.getText().trim(), descField.getText().trim()));
    }
}
