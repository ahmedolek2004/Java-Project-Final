package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;
import com.gym.system.dao.GymRepository;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ScheduledClassForm {
    private ScheduledClassForm() {}

    public record ScheduledClassData(
            int id, String title, int activityId, Integer coachId,
            int dayOfWeek, String startTime, String endTime, int capacity
    ) {}

    public static Optional<ScheduledClassData> showAdd(List<GymRepository.ActivityRow> activities,
            List<GymRepository.CoachRow> coaches, boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("schedule.add"), null, activities, coaches, isDark, isArabic, t);
    }

    public static Optional<ScheduledClassData> showEdit(GymRepository.ScheduledClass existing,
            List<GymRepository.ActivityRow> activities, List<GymRepository.CoachRow> coaches,
            boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("schedule.edit"), existing, activities, coaches, isDark, isArabic, t);
    }

    private static Optional<ScheduledClassData> showForm(String title, GymRepository.ScheduledClass existing,
            List<GymRepository.ActivityRow> activities, List<GymRepository.CoachRow> coaches,
            boolean isDark, boolean isArabic, Function<String, String> t) {
        if (activities == null || activities.isEmpty()) return Optional.empty();

        TextField classTitle = new TextField(existing == null ? "" : existing.title());
        ComboBox<String> activity = new ComboBox<>();
        int activityIndex = 0;
        for (int i = 0; i < activities.size(); i++) {
            GymRepository.ActivityRow row = activities.get(i);
            String label = isArabic ? row.nameArabic() : row.nameEnglish();
            activity.getItems().add(row.id() + " | " + label);
            if (existing != null && (row.nameEnglish().equalsIgnoreCase(existing.activity())
                    || row.nameArabic().equalsIgnoreCase(existing.activity()))) activityIndex = i;
        }
        activity.getSelectionModel().select(activityIndex);

        ComboBox<String> coach = new ComboBox<>();
        coach.getItems().add(t.apply("common.none"));
        int coachIndex = 0;
        if (coaches != null) {
            for (int i = 0; i < coaches.size(); i++) {
                GymRepository.CoachRow row = coaches.get(i);
                coach.getItems().add(row.id() + " | " + row.name());
                if (existing != null && row.name().equalsIgnoreCase(existing.coach())) coachIndex = i + 1;
            }
        }
        coach.getSelectionModel().select(coachIndex);

        ComboBox<String> day = new ComboBox<>(FXCollections.observableArrayList(
                "1 - " + t.apply("day.1"), "2 - " + t.apply("day.2"), "3 - " + t.apply("day.3"),
                "4 - " + t.apply("day.4"), "5 - " + t.apply("day.5"), "6 - " + t.apply("day.6"),
                "7 - " + t.apply("day.7")));
        day.getSelectionModel().select(existing != null && existing.dayOfWeek() >= 1 && existing.dayOfWeek() <= 7
                ? existing.dayOfWeek() - 1 : 0);

        TextField start = new TextField(existing == null ? "09:00" : existing.startTime());
        TextField end = new TextField(existing == null ? "10:00" : existing.endTime());
        TextField capacity = new TextField(existing == null ? "16" : String.valueOf(existing.capacity()));

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("schedule.class"), classTitle);
        FormGrid.addRow(grid, 1, t.apply("activity.title"), activity);
        FormGrid.addRow(grid, 2, t.apply("coach.name"), coach);
        FormGrid.addRow(grid, 3, t.apply("schedule.day"), day);
        FormGrid.addRow(grid, 4, t.apply("schedule.start"), start);
        FormGrid.addRow(grid, 5, t.apply("schedule.end"), end);
        FormGrid.addRow(grid, 6, t.apply("schedule.capacity"), capacity);

        boolean saved = WizardDialog.show(title, title, isDark, isArabic, t, List.of(
                new WizardDialog.Step(title, grid, () -> {
                    if (classTitle.getText().isBlank()) return isArabic ? "يرجى إدخال اسم الحصة." : "Class title is required.";
                    if (activity.getValue() == null || day.getValue() == null)
                        return isArabic ? "يرجى اختيار النشاط واليوم." : "Select an activity and a day.";
                    try {
                        LocalTime from = LocalTime.parse(start.getText().trim());
                        LocalTime to = LocalTime.parse(end.getText().trim());
                        int seats = Integer.parseInt(capacity.getText().trim());
                        if (!from.isBefore(to)) return isArabic ? "وقت النهاية يجب أن يكون بعد وقت البداية." : "End time must be after start time.";
                        if (seats <= 0) return isArabic ? "السعة يجب أن تكون أكبر من صفر." : "Capacity must be greater than zero.";
                    } catch (DateTimeParseException ex) {
                        return isArabic ? "أدخل الوقت بصيغة HH:mm." : "Enter time in HH:mm format.";
                    } catch (NumberFormatException ex) {
                        return isArabic ? "أدخل سعة صحيحة." : "Enter a valid capacity.";
                    }
                    return null;
                })
        ));
        if (!saved) return Optional.empty();

        try {
            int activityId = Integer.parseInt(activity.getValue().substring(0, activity.getValue().indexOf(" | ")));
            String coachValue = coach.getValue();
            Integer coachId = coachValue == null || coachValue.equals(t.apply("common.none")) ? null
                    : Integer.parseInt(coachValue.substring(0, coachValue.indexOf(" | ")));
            int dayNumber = Integer.parseInt(day.getValue().substring(0, day.getValue().indexOf(" - ")));
            return Optional.of(new ScheduledClassData(existing == null ? 0 : existing.id(),
                    classTitle.getText().trim(), activityId, coachId, dayNumber,
                    start.getText().trim(), end.getText().trim(),
                    Integer.parseInt(capacity.getText().trim())));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }
}
