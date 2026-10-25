package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Member;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class MemberForm {
    private MemberForm() {}

    public static Optional<Member> showAdd(boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("members.add"), null, isDark, isArabic, t);
    }

    public static Optional<Member> showEdit(GymRepository.MemberRow existing, boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("members.edit"), existing, isDark, isArabic, t);
    }

    private static Optional<Member> showForm(String title, GymRepository.MemberRow existing,
                                             boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField nameField = new TextField(existing != null ? existing.fullName() : "");
        TextField phoneField = new TextField(existing != null ? existing.phone() : "");
        TextField emailField = new TextField(existing != null ? existing.email() : "");
        
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("gender.unspecified"),
                t.apply("gender.male"),
                t.apply("gender.female")
        ));
        if (existing != null && "FEMALE".equalsIgnoreCase(existing.gender())) {
            genderBox.setValue(t.apply("gender.female"));
        } else if (existing != null && "MALE".equalsIgnoreCase(existing.gender())) {
            genderBox.setValue(t.apply("gender.male"));
        } else {
            genderBox.setValue(t.apply("gender.unspecified"));
        }

        TextField notesField = new TextField(existing != null ? existing.medicalNotes() : "");

        List<String> values = FormDialog.show(title, isDark, isArabic,
                FormDialog.field(t.apply("members.name"), nameField),
                FormDialog.field(t.apply("members.phone"), phoneField),
                FormDialog.field(t.apply("members.email"), emailField),
                FormDialog.field(t.apply("members.gender"), genderBox),
                FormDialog.field(t.apply("members.notes"), notesField)
        );

        if (values.isEmpty()) return Optional.empty();

        String name = values.get(0).trim();
        String phone = values.get(1).trim();
        if (name.isEmpty() || phone.isEmpty()) {
            return Optional.empty();
        }

        String genderVal = values.get(3);
        String genderCode = "UNSPECIFIED";
        if (genderVal.equals(t.apply("gender.female"))) {
            genderCode = "FEMALE";
        } else if (genderVal.equals(t.apply("gender.male"))) {
            genderCode = "MALE";
        }

        int id = existing != null ? existing.id() : 0;
        return Optional.of(new Member(id, name, phone, values.get(2), genderCode, values.get(4)));
    }
}