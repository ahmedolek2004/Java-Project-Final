package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;
import com.gym.system.model.User;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class UserForm {
    private UserForm() {}

    public record NewUserData(String username, char[] password, String fullName, String role, String phone, double salary) {}
    public record EditUserData(String fullName, String role, String phone, double salary) {}

    public static Optional<NewUserData> showAdd(boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField username = new TextField();
        TextField name = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("role.admin"), t.apply("role.receptionist")));
        role.getSelectionModel().selectFirst();
        TextField phone = new TextField();
        TextField salary = new TextField("0");

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("login.username"), username);
        FormGrid.addRow(grid, 1, t.apply("members.name"), name);
        FormGrid.addRow(grid, 2, t.apply("team.password"), password);
        FormGrid.addRow(grid, 3, t.apply("team.role"), role);
        FormGrid.addRow(grid, 4, t.apply("members.phone"), phone);
        FormGrid.addRow(grid, 5, t.apply("team.salary"), salary);

        boolean saved = WizardDialog.show(t.apply("team.add"), t.apply("team.add"), isDark, isArabic, t,
                List.of(new WizardDialog.Step(t.apply("team.add"), grid, () -> {
                    if (username.getText().isBlank() || name.getText().isBlank())
                        return isArabic ? "اسم المستخدم والاسم الكامل مطلوبان." : "Username and full name are required.";
                    if (password.getText().length() < 8)
                        return isArabic ? "كلمة المرور يجب ألا تقل عن 8 أحرف." : "Password must be at least 8 characters.";
                    return validateSalary(salary.getText(), isArabic);
                })));
        if (!saved) return Optional.empty();

        double salaryValue = Double.parseDouble(salary.getText().trim());
        String roleValue = role.getSelectionModel().getSelectedIndex() == 0 ? "ADMIN" : "RECEPTIONIST";
        return Optional.of(new NewUserData(username.getText().trim(), password.getText().toCharArray(),
                name.getText().trim(), roleValue, phone.getText().trim(), salaryValue));
    }

    public static Optional<EditUserData> showEdit(User existing, boolean isDark, boolean isArabic,
                                                  Function<String, String> t) {
        if (existing == null) return Optional.empty();

        TextField name = new TextField(existing.fullName());
        ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("role.admin"), t.apply("role.receptionist")));
        role.setValue("ADMIN".equalsIgnoreCase(existing.role()) ? t.apply("role.admin") : t.apply("role.receptionist"));
        TextField phone = new TextField(existing.phone() == null ? "" : existing.phone());
        TextField salary = new TextField(String.valueOf(existing.salary()));

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("members.name"), name);
        FormGrid.addRow(grid, 1, t.apply("team.role"), role);
        FormGrid.addRow(grid, 2, t.apply("members.phone"), phone);
        FormGrid.addRow(grid, 3, t.apply("team.salary"), salary);

        boolean saved = WizardDialog.show(t.apply("team.edit"), t.apply("team.edit"), isDark, isArabic, t,
                List.of(new WizardDialog.Step(t.apply("team.edit"), grid, () -> {
                    if (name.getText().isBlank()) return isArabic ? "يرجى إدخال الاسم الكامل." : "Full name is required.";
                    return validateSalary(salary.getText(), isArabic);
                })));
        if (!saved) return Optional.empty();

        return Optional.of(new EditUserData(name.getText().trim(),
                role.getSelectionModel().getSelectedIndex() == 0 ? "ADMIN" : "RECEPTIONIST",
                phone.getText().trim(), Double.parseDouble(salary.getText().trim())));
    }

    public static Optional<char[]> showChangePassword(boolean isDark, boolean isArabic, Function<String, String> t) {
        PasswordField password = new PasswordField();
        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("team.password"), password);

        boolean saved = WizardDialog.show(t.apply("team.changePassword"), t.apply("team.changePassword"),
                isDark, isArabic, t, List.of(new WizardDialog.Step(t.apply("team.changePassword"), grid, () ->
                password.getText().length() < 8
                        ? (isArabic ? "كلمة المرور يجب ألا تقل عن 8 أحرف." : "Password must be at least 8 characters.")
                        : null)));
        return saved ? Optional.of(password.getText().toCharArray()) : Optional.empty();
    }

    private static String validateSalary(String raw, boolean arabic) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (!Double.isFinite(value) || value < 0)
                return arabic ? "الراتب يجب ألا يكون سالبًا." : "Salary must be zero or greater.";
            return null;
        } catch (NumberFormatException ex) {
            return arabic ? "يرجى إدخال راتب صحيح." : "Enter a valid salary.";
        }
    }
}
