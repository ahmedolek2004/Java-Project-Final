package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;
import com.gym.system.dao.GymRepository;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PlanForm {
    private PlanForm() {}

    public record PlanInput(String name, String type, int amount, double price) {}

    public static Optional<PlanInput> showAdd(boolean isDark, boolean isArabic, Function<String, String> t) {
        return showForm(t.apply("plan.add"), null, isDark, isArabic, t);
    }

    public static Optional<PlanInput> showEdit(GymRepository.PlanRow existing, boolean isDark, boolean isArabic,
                                               Function<String, String> t) {
        return showForm(t.apply("plan.edit"), existing, isDark, isArabic, t);
    }

    private static Optional<PlanInput> showForm(String title, GymRepository.PlanRow existing, boolean isDark,
                                                boolean isArabic, Function<String, String> t) {
        TextField name = new TextField(existing == null ? "" : existing.name());
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("subscription.TIME_BASED"), t.apply("subscription.SESSION_BASED")));
        type.getSelectionModel().select(existing != null && existing.sessionBased() ? 1 : 0);
        TextField amount = new TextField(existing == null ? "1" : String.valueOf(existing.amount()));
        TextField price = new TextField(existing == null ? "" : String.valueOf(existing.price()));

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("plan.name"), name);
        FormGrid.addRow(grid, 1, t.apply("subscription.type"), type);
        FormGrid.addRow(grid, 2, t.apply("subscription.duration"), amount);
        FormGrid.addRow(grid, 3, t.apply("plan.price"), price);

        boolean saved = WizardDialog.show(title, title, isDark, isArabic, t, List.of(
                new WizardDialog.Step(title, grid, () -> {
                    if (name.getText().isBlank()) return isArabic ? "يرجى إدخال اسم الباقة." : "Plan name is required.";
                    if (type.getValue() == null) return isArabic ? "يرجى اختيار نوع الباقة." : "Please select a plan type.";
                    try {
                        int count = Integer.parseInt(amount.getText().trim());
                        double cost = Double.parseDouble(price.getText().trim());
                        if (count <= 0 || !Double.isFinite(cost) || cost < 0)
                            return isArabic ? "تحقق من المدة والسعر." : "Check the duration and price.";
                    } catch (NumberFormatException ex) {
                        return isArabic ? "أدخل مدة وسعرًا صحيحين." : "Enter a valid duration and price.";
                    }
                    return null;
                })
        ));
        if (!saved) return Optional.empty();

        int count = Integer.parseInt(amount.getText().trim());
        double cost = Double.parseDouble(price.getText().trim());
        String planType = type.getSelectionModel().getSelectedIndex() == 1 ? "SESSION_BASED" : "TIME_BASED";
        return Optional.of(new PlanInput(name.getText().trim(), planType, count, cost));
    }
}
