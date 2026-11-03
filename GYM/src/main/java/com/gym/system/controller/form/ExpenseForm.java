package com.gym.system.controller.form;

import com.gym.system.controller.WizardDialog;

import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ExpenseForm {
    private ExpenseForm() {}

    public record ExpenseData(String description, double amount, LocalDate date) {}

    public static Optional<ExpenseData> show(boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField description = new TextField();
        TextField amount = new TextField();
        DatePicker date = new DatePicker(LocalDate.now());

        GridPane grid = FormGrid.create();
        FormGrid.addRow(grid, 0, t.apply("finance.description"), description);
        FormGrid.addRow(grid, 1, t.apply("finance.amount"), amount);
        FormGrid.addRow(grid, 2, t.apply("finance.date"), date);

        boolean saved = WizardDialog.show(t.apply("finance.addExpense"), t.apply("finance.addExpense"),
                isDark, isArabic, t, List.of(new WizardDialog.Step(t.apply("finance.addExpense"), grid, () -> {
                    if (description.getText().isBlank()) return isArabic ? "يرجى إدخال وصف المصروف." : "Description is required.";
                    try {
                        double value = Double.parseDouble(amount.getText().trim());
                        if (!Double.isFinite(value) || value <= 0) return isArabic ? "المبلغ يجب أن يكون أكبر من صفر." : "Amount must be greater than zero.";
                    } catch (NumberFormatException ex) {
                        return isArabic ? "يرجى إدخال مبلغ صحيح." : "Enter a valid amount.";
                    }
                    if (date.getValue() == null) return isArabic ? "يرجى اختيار التاريخ." : "Please select a date.";
                    return null;
                })));
        if (!saved) return Optional.empty();

        return Optional.of(new ExpenseData(description.getText().trim(),
                Double.parseDouble(amount.getText().trim()), date.getValue()));
    }
}
