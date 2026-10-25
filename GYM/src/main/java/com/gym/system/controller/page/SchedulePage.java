package com.gym.system.controller.page;

import com.gym.system.controller.form.ScheduledClassForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.ScheduleService;
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

public class SchedulePage {
    private final ScheduleService service;
    private final PageContext ctx;

    public SchedulePage(ScheduleService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        Button addBtn = new Button(ctx.t("schedule.add"));
        addBtn.getStyleClass().add("primary-button");

        Button editBtn = new Button(ctx.t("schedule.edit"));
        Button deleteBtn = new Button(ctx.t("button.delete"));

        HBox toolbar = new HBox(10, search, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, editBtn, deleteBtn);

        TableView<GymRepository.ScheduledClass> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.ScheduledClass> masterList = FXCollections.observableArrayList(service.getClasses());
        FilteredList<GymRepository.ScheduledClass> filtered = new FilteredList<>(masterList, c -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getClasses());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(c -> q.isEmpty()
                    || c.title().toLowerCase().contains(q)
                    || c.activity().toLowerCase().contains(q)
                    || c.coach().toLowerCase().contains(q));
        });

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("schedule.class"), GymRepository.ScheduledClass::title, 1.2);
        TableFactory.addColumn(table, ctx.t("activity.title"), GymRepository.ScheduledClass::activity, 1);
        TableFactory.addColumn(table, ctx.t("coach.name"), GymRepository.ScheduledClass::coach, 1);
        TableFactory.addColumn(table, ctx.t("schedule.day"), row -> ctx.t("day." + row.dayOfWeek()), .8);
        TableFactory.addColumn(table, ctx.t("schedule.time"), row -> row.startTime() + "–" + row.endTime(), 1);
        TableFactory.addColumn(table, ctx.t("schedule.capacity"), row -> Integer.toString(row.capacity()), .7);

        // Actions Wiring
        addBtn.setOnAction(e -> {
            try {
                Optional<ScheduledClassForm.ScheduledClassData> data = ScheduledClassForm.showAdd(
                        ctx.repository().activities(),
                        ctx.repository().coaches(),
                        ctx.isDarkMode(),
                        ctx.isArabic(),
                        ctx.tFunction()
                );
                data.ifPresent(d -> {
                    try {
                        service.addClass(d.title(), d.activityId(), d.coachId(), d.dayOfWeek(), d.startTime(), d.endTime(), d.capacity());
                        refreshTable.run();
                    } catch (Exception ex) {
                        ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                    }
                });
            } catch (SQLException ex) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + ex.getMessage());
            }
        });

        editBtn.setOnAction(e -> {
            GymRepository.ScheduledClass selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("schedule.select"));
                return;
            }
            try {
                Optional<ScheduledClassForm.ScheduledClassData> data = ScheduledClassForm.showEdit(
                        selected,
                        ctx.repository().activities(),
                        ctx.repository().coaches(),
                        ctx.isDarkMode(),
                        ctx.isArabic(),
                        ctx.tFunction()
                );
                data.ifPresent(d -> {
                    try {
                        service.updateClass(selected.id(), d.title(), d.activityId(), d.coachId(), d.dayOfWeek(), d.startTime(), d.endTime(), d.capacity());
                        refreshTable.run();
                    } catch (Exception ex) {
                        ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                    }
                });
            } catch (SQLException ex) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + ex.getMessage());
            }
        });

        deleteBtn.setOnAction(e -> {
            GymRepository.ScheduledClass selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("schedule.select"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("schedule.confirmDelete")))) {
                try {
                    service.deleteClass(selected.id());
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