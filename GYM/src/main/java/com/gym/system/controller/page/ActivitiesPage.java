package com.gym.system.controller.page;

import com.gym.system.controller.form.ActivityForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Activity;
import com.gym.system.service.ActivityService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class ActivitiesPage {
    private final ActivityService service;
    private final PageContext ctx;

    public ActivitiesPage(ActivityService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        Button addBtn = new Button(ctx.t("activity.add"));
        addBtn.getStyleClass().add("primary-button");

        Button editBtn = new Button(ctx.t("button.editActivity"));
        Button deleteBtn = new Button(ctx.t("button.delete"));

        HBox toolbar = new HBox(10, search, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, editBtn, deleteBtn);

        TableView<GymRepository.ActivityRow> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.ActivityRow> masterList = FXCollections.observableArrayList(service.getAllActivities());
        FilteredList<GymRepository.ActivityRow> filtered = new FilteredList<>(masterList, a -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getAllActivities());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(a -> q.isEmpty()
                    || a.nameArabic().toLowerCase().contains(q)
                    || a.nameEnglish().toLowerCase().contains(q));
        });

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("activity.arabic"), GymRepository.ActivityRow::nameArabic, 1.2);
        TableFactory.addColumn(table, ctx.t("activity.english"), GymRepository.ActivityRow::nameEnglish, 1.2);

        // Actions Wiring
        addBtn.setOnAction(e -> {
            Optional<Activity> act = ActivityForm.showAdd(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            act.ifPresent(a -> {
                try {
                    service.addActivity(a);
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        editBtn.setOnAction(e -> {
            GymRepository.ActivityRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("activity.select"));
                return;
            }
            Optional<Activity> act = ActivityForm.showEdit(selected, ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            act.ifPresent(a -> {
                try {
                    service.updateActivity(selected.id(), a);
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        deleteBtn.setOnAction(e -> {
            GymRepository.ActivityRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("activity.select"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("activity.confirmDelete")))) {
                try {
                    service.deleteActivity(selected.id());
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            }
        });

        content.getChildren().addAll(toolbar, actionsBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }
}