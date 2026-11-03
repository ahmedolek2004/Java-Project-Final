package com.gym.system.controller.form;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Builds the label/input grid that every wizard-style form dialog shares. */
final class FormGrid {
    private static final double LABEL_MIN_WIDTH = 150;
    private static final double LABEL_PREF_WIDTH = 170;
    private static final double LABEL_MAX_WIDTH = 190;

    private FormGrid() {}

    /** Creates an empty two-column grid: fixed-width labels on one side, growing inputs on the other. */
    static GridPane create() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 0, 0));
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(LABEL_MIN_WIDTH);
        labels.setPrefWidth(LABEL_PREF_WIDTH);
        labels.setMaxWidth(LABEL_MAX_WIDTH);
        ColumnConstraints inputs = new ColumnConstraints();
        inputs.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, inputs);
        return grid;
    }

    /** Adds a captioned input as one row of the grid. */
    static void addRow(GridPane grid, int row, String label, Node input) {
        Label caption = new Label(label);
        caption.getStyleClass().add("wizard-field-label");
        caption.setWrapText(true);
        grid.add(caption, 0, row);
        if (input instanceof Control control) {
            control.setMaxWidth(Double.MAX_VALUE);
        }
        grid.add(input, 1, row);
    }
}
