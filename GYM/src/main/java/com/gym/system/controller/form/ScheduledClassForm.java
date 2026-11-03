package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ScheduledClassForm {
    private ScheduledClassForm() {}

    public record ScheduledClassData(
            int id, String title, int activityId, Integer coachId,
            int dayOfWeek, String startTime, String endTime, int capacity
    ) {}

    public static Optional<ScheduledClassData> showAdd(
            List<GymRepository.ActivityRow> activities,
            List<GymRepository.CoachRow> coaches,
            boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("schedule.add"), null, activities, coaches, isDark, isArabic, t);
    }

    public static Optional<ScheduledClassData> showEdit(
            GymRepository.ScheduledClass existing,
            List<GymRepository.ActivityRow> activities,
            List<GymRepository.CoachRow> coaches,
            boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("schedule.edit"), existing, activities, coaches, isDark, isArabic, t);
    }

    private static Optional<ScheduledClassData> showForm(
            String title, GymRepository.ScheduledClass existing,
            List<GymRepository.ActivityRow> activities,
            List<GymRepository.CoachRow> coaches,
            boolean isDark, boolean isArabic, Function<String, String> t) {
        if (activities == null || activities.isEmpty()) return Optional.empty();

        TextField titleField = new TextField(existing != null ? existing.title() : "");

        ComboBox<String> activityBox = new ComboBox<>();
        int selectedActIdx = 0;
        for (int i = 0; i < activities.size(); i++) {
            GymRepository.ActivityRow act = activities.get(i);
            String actName = isArabic ? act.nameArabic() : act.nameEnglish();
            activityBox.getItems().add(act.id() + " | " + actName);
            if (existing != null && (act.nameEnglish().equalsIgnoreCase(existing.activity()) || act.nameArabic().equalsIgnoreCase(existing.activity()))) {
                selectedActIdx = i;
            }
        }
        activityBox.getSelectionModel().select(selectedActIdx);

        ComboBox<String> coachBox = new ComboBox<>();
        coachBox.getItems().add(t.apply("common.none"));
        int selectedCoachIdx = 0;
        if (coaches != null) {
            for (int i = 0; i < coaches.size(); i++) {
                GymRepository.CoachRow coach = coaches.get(i);
                coachBox.getItems().add(coach.id() + " | " + coach.name());
                if (existing != null && coach.name().equalsIgnoreCase(existing.coach())) {
                    selectedCoachIdx = i + 1;
                }
            }
        }
        coachBox.getSelectionModel().select(selectedCoachIdx);

        ComboBox<String> dayBox = new ComboBox<>(FXCollections.observableArrayList(
                "1 - " + t.apply("day.1"),
                "2 - " + t.apply("day.2"),
                "3 - " + t.apply("day.3"),
                "4 - " + t.apply("day.4"),
                "5 - " + t.apply("day.5"),
                "6 - " + t.apply("day.6"),
                "7 - " + t.apply("day.7")
        ));
        dayBox.getSelectionModel().select(existing != null && existing.dayOfWeek() >= 1 && existing.dayOfWeek() <= 7
                ? existing.dayOfWeek() - 1 : 0);

        TextField startField = new TextField(existing != null ? existing.startTime() : "09:00");
        TextField endField = new TextField(existing != null ? existing.endTime() : "10:00");
        TextField capacityField = new TextField(existing != null ? String.valueOf(existing.capacity()) : "16");

        List<String> values = FormDialog.show(title, isDark, isArabic,
                FormDialog.field(t.apply("schedule.class"), titleField),
                FormDialog.field(t.apply("activity.title"), activityBox),
                FormDialog.field(t.apply("coach.name"), coachBox),
                FormDialog.field(t.apply("schedule.day"), dayBox),
                FormDialog.field(t.apply("schedule.start"), startField),
                FormDialog.field(t.apply("schedule.end"), endField),
                FormDialog.field(t.apply("schedule.capacity"), capacityField)
        );

        if (values.isEmpty()) return Optional.empty();

        String classTitle = values.get(0).trim();
        if (classTitle.isEmpty()) return Optional.empty();

        try {
            int actId = Integer.parseInt(values.get(1).substring(0, values.get(1).indexOf(" | ")));
            Integer coachId = null;
            String coachVal = values.get(2);
            if (coachVal != null && !coachVal.equals(t.apply("common.none")) && coachVal.contains(" | ")) {
                coachId = Integer.parseInt(coachVal.substring(0, coachVal.indexOf(" | ")));
            }

            int dayOfWeek = Integer.parseInt(values.get(3).substring(0, 1));
            String startTime = values.get(4).trim();
            String endTime = values.get(5).trim();
            int capacity = Integer.parseInt(values.get(6).trim());

            if (startTime.compareTo(endTime) >= 0 || capacity <= 0) return Optional.empty();

            int id = existing != null ? existing.id() : 0;
            return Optional.of(new ScheduledClassData(id, classTitle, actId, coachId, dayOfWeek, startTime, endTime, capacity));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
