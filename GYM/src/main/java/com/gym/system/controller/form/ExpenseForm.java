package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ExpenseForm {
    private ExpenseForm() {}

    public record ExpenseData(String description, double amount, LocalDate date) {}

    public static Optional<ExpenseData> show(boolean isDark, boolean isArabic, Function<String, String> t) {
        TextField descField = new TextField();
        TextField amountField = new TextField();
        DatePicker datePicker = new DatePicker(LocalDate.now());

        List<String> values = FormDialog.show(t.apply("finance.addExpense"), isDark, isArabic,
                FormDialog.field(t.apply("finance.description"), descField),
                FormDialog.field(t.apply("finance.amount"), amountField),
                FormDialog.field(t.apply("finance.date"), datePicker)
        );

        if (values.isEmpty()) return Optional.empty();

        String desc = values.get(0).trim();
        if (desc.isEmpty()) return Optional.empty();

        try {
            double amount = Double.parseDouble(values.get(1).trim());
            if (amount < 0) return Optional.empty();
            LocalDate date = values.get(2).isEmpty() ? LocalDate.now() : LocalDate.parse(values.get(2));
            return Optional.of(new ExpenseData(desc, amount, date));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}