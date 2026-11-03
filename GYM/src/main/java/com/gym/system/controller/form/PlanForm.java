package com.gym.system.controller.form;

import com.gym.system.controller.FormDialog;
import com.gym.system.dao.GymRepository;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

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
        TextField nameField = new TextField(existing != null ? existing.name() : "");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList(
                t.apply("subscription.TIME_BASED"), t.apply("subscription.SESSION_BASED")));
        typeBox.getSelectionModel().select(existing != null && existing.sessionBased() ? 1 : 0);
        TextField amountField = new TextField(existing != null ? String.valueOf(existing.amount()) : "1");
        TextField priceField = new TextField(existing != null ? String.valueOf(existing.price()) : "");

        List<String> values = FormDialog.show(title, isDark, isArabic,
                FormDialog.field(t.apply("plan.name"), nameField),
                FormDialog.field(t.apply("subscription.type"), typeBox),
                FormDialog.field(t.apply("subscription.duration"), amountField),
                FormDialog.field(t.apply("plan.price"), priceField));
        if (values.isEmpty()) {
            return Optional.empty();
        }
        try {
            String name = values.get(0).trim();
            String type = typeBox.getSelectionModel().getSelectedIndex() == 1 ? "SESSION_BASED" : "TIME_BASED";
            int amount = Integer.parseInt(values.get(2).trim());
            double price = Double.parseDouble(values.get(3).trim());
            if (name.isEmpty() || amount <= 0 || price < 0) {
                throw new IllegalArgumentException("Invalid plan values.");
            }
            return Optional.of(new PlanInput(name, type, amount, price));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid plan values.", exception);
        }
    }
}
