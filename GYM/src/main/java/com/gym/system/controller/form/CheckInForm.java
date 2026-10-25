package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import javafx.scene.control.ComboBox;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class CheckInForm {
    private CheckInForm() {}

    public static Optional<Integer> show(List<GymRepository.MemberRow> members,
                                         boolean isDark, boolean isArabic, Function<String, String> t) {
        if (members == null || members.isEmpty()) return Optional.empty();

        ComboBox<String> memberBox = new ComboBox<>();
        members.forEach(m -> memberBox.getItems().add(m.id() + " | " + m.fullName() + " (" + m.phone() + ")"));
        memberBox.getSelectionModel().selectFirst();

        List<String> values = FormDialog.show(t.apply("attendance.checkin"), isDark, isArabic,
                FormDialog.field(t.apply("members.name"), memberBox)
        );

        if (values.isEmpty()) return Optional.empty();

        String val = memberBox.getValue();
        if (val == null || !val.contains(" | ")) return Optional.empty();

        try {
            int memberId = Integer.parseInt(val.substring(0, val.indexOf(" | ")));
            return Optional.of(memberId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
