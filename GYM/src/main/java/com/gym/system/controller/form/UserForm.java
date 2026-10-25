package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.model.User;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class UserForm {
    private UserForm() {}

    public record NewUserData(String username, char[] password, String fullName, String role, String phone, double salary) {}
    public record EditUserData(String fullName, String role, String phone, double salary) {}

    public static Optional<NewUserData> showAdd(boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField usernameField = new TextField();
        TextField nameField = new TextField();
        PasswordField passwordField = new PasswordField();
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("role.admin"),
                t.apply("role.receptionist")
        ));
        roleBox.getSelectionModel().selectFirst();
        TextField phoneField = new TextField();
        TextField salaryField = new TextField("0");

        List<String> values = FormDialog.show(t.apply("team.add"), isDark, isArabic,
                FormDialog.field(t.apply("login.username"), usernameField),
                FormDialog.field(t.apply("members.name"), nameField),
                FormDialog.field(t.apply("team.password"), passwordField),
                FormDialog.field(t.apply("team.role"), roleBox),
                FormDialog.field(t.apply("members.phone"), phoneField),
                FormDialog.field(t.apply("team.salary"), salaryField)
        );

        if (values.isEmpty()) return Optional.empty();

        String username = values.get(0).trim();
        String name = values.get(1).trim();
        String password = values.get(2);
        if (username.isEmpty() || name.isEmpty() || password == null || password.length() < 8) {
            return Optional.empty();
        }

        String role = values.get(3).equals(t.apply("role.admin")) ? "ADMIN" : "RECEPTIONIST";
        try {
            double salary = Double.parseDouble(values.get(5).trim());
            return Optional.of(new NewUserData(username, password.toCharArray(), name, role, values.get(4).trim(), salary));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<EditUserData> showEdit(User existing, boolean isDark, boolean isArabic, Function<String, String> t) {
        if (existing == null) return Optional.empty();

        TextField nameField = new TextField(existing.fullName());
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("role.admin"),
                t.apply("role.receptionist")
        ));
        roleBox.setValue("ADMIN".equalsIgnoreCase(existing.role()) ? t.apply("role.admin") : t.apply("role.receptionist"));

        TextField phoneField = new TextField(existing.phone() != null ? existing.phone() : "");
        TextField salaryField = new TextField(String.valueOf(existing.salary()));

        List<String> values = FormDialog.show(t.apply("team.edit"), isDark, isArabic,
                FormDialog.field(t.apply("members.name"), nameField),
                FormDialog.field(t.apply("team.role"), roleBox),
                FormDialog.field(t.apply("members.phone"), phoneField),
                FormDialog.field(t.apply("team.salary"), salaryField)
        );

        if (values.isEmpty()) return Optional.empty();

        String name = values.get(0).trim();
        if (name.isEmpty()) return Optional.empty();

        String role = values.get(1).equals(t.apply("role.admin")) ? "ADMIN" : "RECEPTIONIST";
        try {
            double salary = Double.parseDouble(values.get(3).trim());
            return Optional.of(new EditUserData(name, role, values.get(2).trim(), salary));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<char[]> showChangePassword(boolean isDark, boolean isArabic, Function<String, String> t) {
        PasswordField passField = new PasswordField();
        List<String> values = FormDialog.show(t.apply("team.changePassword"), isDark, isArabic,
                FormDialog.field(t.apply("team.password"), passField)
        );

        if (values.isEmpty() || values.get(0).length() < 8) {
            return Optional.empty();
        }
        return Optional.of(values.get(0).toCharArray());
    }
}
