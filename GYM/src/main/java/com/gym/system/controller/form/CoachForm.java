package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Coach;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CoachForm {
    private CoachForm() {}

    public static Optional<Coach> showAdd(List<GymRepository.ActivityRow> activities,
                                          boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("coach.add"), null, activities, isDark, isArabic, t);
    }

    public static Optional<Coach> showEdit(GymRepository.CoachRow existing,
                                           List<GymRepository.ActivityRow> activities,
                                           boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("coach.edit"), existing, activities, isDark, isArabic, t);
    }

    private static Optional<Coach> showForm(String title, GymRepository.CoachRow existing,
                                             List<GymRepository.ActivityRow> activities,
                                             boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField nameField = new TextField(existing != null ? existing.name() : "");
        TextField phoneField = new TextField(existing != null ? existing.phone() : "");
        TextField salaryField = new TextField(existing != null ? String.valueOf(existing.salary()) : "0");

        ComboBox<String> activityBox = new ComboBox<>();
        activityBox.getItems().add(t.apply("common.none"));
        int selectedIndex = 0;
        if (activities != null) {
            for (int i = 0; i < activities.size(); i++) {
                GymRepository.ActivityRow act = activities.get(i);
                String actName = isArabic ? act.nameArabic() : act.nameEnglish();
                activityBox.getItems().add(act.id() + " | " + actName);
                if (existing != null && actName.equalsIgnoreCase(existing.activity())) {
                    selectedIndex = i + 1;
                }
            }
        }
        activityBox.getSelectionModel().select(selectedIndex);

        CheckBox ptCheckBox = new CheckBox(t.apply("coach.personal"));
        ptCheckBox.setSelected(existing != null && existing.personalTrainer());

        List<String> values = FormDialog.show(title, isDark, isArabic,
                FormDialog.field(t.apply("coach.name"), nameField),
                FormDialog.field(t.apply("members.phone"), phoneField),
                FormDialog.field(t.apply("team.salary"), salaryField),
                FormDialog.field(t.apply("activity.title"), activityBox),
                FormDialog.field(t.apply("coach.personal"), ptCheckBox)
        );

        if (values.isEmpty()) return Optional.empty();

        String name = values.get(0).trim();
        if (name.isEmpty()) return Optional.empty();

        try {
            double salary = Double.parseDouble(values.get(2).trim());
            if (salary < 0) return Optional.empty();

            Integer activityId = null;
            String actVal = values.get(3);
            if (actVal != null && !actVal.equals(t.apply("common.none")) && actVal.contains(" | ")) {
                activityId = Integer.parseInt(actVal.substring(0, actVal.indexOf(" | ")));
            }

            boolean isPt = Boolean.parseBoolean(values.get(4));
            int id = existing != null ? existing.id() : 0;
            return Optional.of(new Coach(id, name, values.get(1).trim(), activityId, salary, isPt));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
