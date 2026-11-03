package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Coach;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

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
        TextField name = new TextField(existing == null ? "" : existing.name());
        TextField phone = new TextField(existing == null || existing.phone() == null ? "" : existing.phone());
        TextField salary = new TextField(existing == null ? "0" : String.valueOf(existing.salary()));
        ComboBox<String> activity = new ComboBox<>();
        activity.getItems().add(t.apply("common.none"));
        int selected = 0;
        if (activities != null) {
            for (int i = 0; i < activities.size(); i++) {
                GymRepository.ActivityRow row = activities.get(i);
                String label = isArabic ? row.nameArabic() : row.nameEnglish();
                activity.getItems().add(row.id() + " | " + label);
                if (existing != null && (label.equalsIgnoreCase(existing.activity())
                        || row.nameArabic().equalsIgnoreCase(existing.activity())
                        || row.nameEnglish().equalsIgnoreCase(existing.activity()))) selected = i + 1;
            }
        }
        activity.getSelectionModel().select(selected);
        CheckBox personal = new CheckBox(t.apply("coach.personal"));
        personal.setSelected(existing != null && existing.personalTrainer());

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("coach.name"), name);
        FormGrid.addRow(grid, 1, t.apply("members.phone"), phone);
        FormGrid.addRow(grid, 2, t.apply("team.salary"), salary);
        FormGrid.addRow(grid, 3, t.apply("activity.title"), activity);
        FormGrid.addRow(grid, 4, t.apply("coach.personal"), personal);

        boolean saved = WizardDialog.show(title, title, isDark, isArabic, t, List.of(
                new WizardDialog.Step(title, grid, () -> {
                    if (name.getText().isBlank()) return isArabic ? "يرجى إدخال اسم المدرب." : "Coach name is required.";
                    try {
                        double value = Double.parseDouble(salary.getText().trim());
                        if (!Double.isFinite(value) || value < 0) return isArabic ? "الراتب يجب ألا يكون سالبًا." : "Salary must be zero or greater.";
                    } catch (NumberFormatException ex) {
                        return isArabic ? "يرجى إدخال راتب صحيح." : "Enter a valid salary.";
                    }
                    return null;
                })
        ));
        if (!saved) return Optional.empty();

        try {
            double salaryValue = Double.parseDouble(salary.getText().trim());
            String selectedActivity = activity.getValue();
            Integer activityId = selectedActivity == null || selectedActivity.equals(t.apply("common.none"))
                    ? null : Integer.parseInt(selectedActivity.substring(0, selectedActivity.indexOf(" | ")));
            return Optional.of(new Coach(existing == null ? 0 : existing.id(), name.getText().trim(),
                    phone.getText().trim(), activityId, salaryValue, personal.isSelected()));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
