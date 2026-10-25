package com.gym.system.controller.helper;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.function.Function;

public class TableFactory {
    public static <T> TableView<T> createTable(String emptyText) {
        TableView<T> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(emptyText));
        return table;
    }

    public static <T> void addColumn(TableView<T> table, String title, Function<T, String> valueExtractor, double widthMultiplier) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(valueExtractor.apply(cell.getValue())));
        column.setMinWidth(85 * widthMultiplier);
        table.getColumns().add(column);
    }
}