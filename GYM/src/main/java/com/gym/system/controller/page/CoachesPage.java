package com.gym.system.controller.page;

import com.gym.system.controller.form.CoachForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Coach;
import com.gym.system.service.CoachService;
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

public class CoachesPage {
    private final CoachService service;
    private final PageContext ctx;

    public CoachesPage(CoachService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        Button addBtn = new Button(ctx.t("coach.add"));
        addBtn.getStyleClass().add("primary-button");

        Button editBtn = new Button(ctx.t("button.editCoach"));
        Button deleteBtn = new Button(ctx.t("button.delete"));

        HBox toolbar = new HBox(10, search, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, editBtn, deleteBtn);

        TableView<GymRepository.CoachRow> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.CoachRow> masterList = FXCollections.observableArrayList(service.getAllCoaches());
        FilteredList<GymRepository.CoachRow> filtered = new FilteredList<>(masterList, c -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getAllCoaches());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(c -> q.isEmpty()
                    || c.name().toLowerCase().contains(q)
                    || c.phone().toLowerCase().contains(q)
                    || c.activity().toLowerCase().contains(q));
        });

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("coach.name"), GymRepository.CoachRow::name, 1.2);
        TableFactory.addColumn(table, ctx.t("members.phone"), GymRepository.CoachRow::phone, 1);
        TableFactory.addColumn(table, ctx.t("activity.title"), GymRepository.CoachRow::activity, 1);
        TableFactory.addColumn(table, ctx.t("team.salary"), row -> ctx.money(row.salary()), .9);
        TableFactory.addColumn(table, ctx.t("coach.personal"),
                row -> row.personalTrainer() ? ctx.t("common.yes") : ctx.t("common.no"), 1);

        // Actions Wiring
        addBtn.setOnAction(e -> {
            try {
                Optional<Coach> coach = CoachForm.showAdd(
                        ctx.repository().activities(),
                        ctx.isDarkMode(),
                        ctx.isArabic(),
                        ctx.tFunction()
                );
                coach.ifPresent(c -> {
                    try {
                        service.addCoach(c);
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
            GymRepository.CoachRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("coach.select"));
                return;
            }
            try {
                Optional<Coach> coach = CoachForm.showEdit(
                        selected,
                        ctx.repository().activities(),
                        ctx.isDarkMode(),
                        ctx.isArabic(),
                        ctx.tFunction()
                );
                coach.ifPresent(c -> {
                    try {
                        service.updateCoach(selected.id(), c);
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
            GymRepository.CoachRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("coach.select"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("coach.confirmDelete")))) {
                try {
                    service.deleteCoach(selected.id());
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