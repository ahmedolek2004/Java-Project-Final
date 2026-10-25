package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Activity;
import javafx.scene.control.TextField;

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
        TextField arField = new TextField(existing != null ? existing.nameArabic() : "");
        TextField enField = new TextField(existing != null ? existing.nameEnglish() : "");
        TextField descField = new TextField("");

        List<String> values = FormDialog.show(title, isDark, isArabic,
                FormDialog.field(t.apply("activity.arabic"), arField),
                FormDialog.field(t.apply("activity.english"), enField),
                FormDialog.field(t.apply("finance.description"), descField)
        );

        if (values.isEmpty()) return Optional.empty();

        String arName = values.get(0).trim();
        String enName = values.get(1).trim();
        if (arName.isEmpty() || enName.isEmpty()) return Optional.empty();

        int id = existing != null ? existing.id() : 0;
        return Optional.of(new Activity(id, arName, enName, values.get(2).trim()));
    }
}
