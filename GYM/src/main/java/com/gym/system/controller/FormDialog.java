package com.gym.system.controller;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;

public final class FormDialog {
    private FormDialog() {}

    public record Field(String label, Control input) {}

    public static Field field(String label, Control input) {
        return new Field(label, input);
    }

    public static List<String> show(String title, boolean darkMode, boolean arabic, Field... fields) {
        Dialog<List<String>> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(12, 0, 0, 0));
        ColumnConstraints labels = new ColumnConstraints();
        ColumnConstraints inputs = new ColumnConstraints();
        inputs.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, inputs);

        for (int index = 0; index < fields.length; index++) {
            Field field = fields[index];
            grid.add(new javafx.scene.control.Label(field.label()), 0, index);
            field.input().setMaxWidth(Double.MAX_VALUE);
            grid.add(field.input(), 1, index);
        }

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getStylesheets().add(
                FormDialog.class.getResource("/com/gym/system/views/css/style.css").toExternalForm());
        if (darkMode) {
            dialog.getDialogPane().getStyleClass().add("dark-dialog");
        }
        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            List<String> values = new ArrayList<>();
            for (Field field : fields) {
                values.add(controlValue(field.input()));
            }
            return values;
        });
        dialog.getDialogPane().setNodeOrientation(
                arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        return dialog.showAndWait().orElse(List.of());
    }

    private static String controlValue(Control control) {
        if (control instanceof TextField field) return field.getText().trim();
        if (control instanceof PasswordField field) return field.getText();
        if (control instanceof ComboBox<?> combo) {
            Object val = combo.getValue();
            return val == null ? "" : String.valueOf(val);
        }
        if (control instanceof DatePicker date) return date.getValue() == null ? "" : date.getValue().toString();
        if (control instanceof CheckBox checkBox) return Boolean.toString(checkBox.isSelected());
        return "";
    }
}
