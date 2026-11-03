package com.gym.system.controller.page;

import com.gym.system.controller.form.PlanForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.PlanService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class PlansPage {
    private final PlanService service;
    private final PageContext ctx;

    public PlansPage(PlanService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("plan.search"));
        Button addBtn = new Button(ctx.t("plan.add"));
        addBtn.getStyleClass().add("primary-button");
        Button editBtn = new Button(ctx.t("plan.edit"));
        Button deleteBtn = new Button(ctx.t("button.delete"));

        HBox toolbar = new HBox(10, search, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actionsBar = new HBox(10, editBtn, deleteBtn);

        TableView<GymRepository.PlanRow> table = TableFactory.createTable(ctx.t("table.empty"));
        ObservableList<GymRepository.PlanRow> masterList = FXCollections.observableArrayList(service.getAllPlans());
        FilteredList<GymRepository.PlanRow> filtered = new FilteredList<>(masterList, plan -> true);
        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("plan.name"), GymRepository.PlanRow::name, 1.4);
        TableFactory.addColumn(table, ctx.t("subscription.type"), row -> ctx.t("subscription." + row.type()), 1);
        TableFactory.addColumn(table, ctx.t("plan.length"), row -> row.sessionBased()
                ? row.totalSessions() + " " + ctx.t("wizard.sessionsUnit")
                : row.durationMonths() + " " + ctx.t("wizard.monthsUnit"), 1);
        TableFactory.addColumn(table, ctx.t("plan.price"), row -> ctx.money(row.price()), 1);

        Runnable refresh = () -> {
            try {
                masterList.setAll(service.getAllPlans());
            } catch (SQLException exception) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldValue, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(plan -> q.isEmpty() || plan.name().toLowerCase().contains(q));
        });

        addBtn.setOnAction(event -> save(PlanForm::showAdd, null, refresh));
        editBtn.setOnAction(event -> {
            GymRepository.PlanRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("plan.select"));
                return;
            }
            save((dark, arabic, t) -> PlanForm.showEdit(selected, dark, arabic, t), selected, refresh);
        });
        deleteBtn.setOnAction(event -> {
            GymRepository.PlanRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("plan.select"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("plan.confirmDelete")))) {
                try {
                    service.deletePlan(selected.id());
                    refresh.run();
                } catch (SQLException exception) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
                }
            }
        });

        content.getChildren().addAll(toolbar, actionsBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private interface FormOpener {
        Optional<PlanForm.PlanInput> open(boolean dark, boolean arabic, java.util.function.Function<String, String> t);
    }

    private void save(FormOpener opener, GymRepository.PlanRow existing, Runnable refresh) {
        try {
            Optional<PlanForm.PlanInput> input = opener.open(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            if (input.isEmpty()) {
                return;
            }
            PlanForm.PlanInput value = input.get();
            if (existing == null) {
                service.addPlan(value.name(), value.type(), value.amount(), value.price());
            } else {
                service.updatePlan(existing.id(), value.name(), value.type(), value.amount(), value.price());
            }
            refresh.run();
        } catch (IllegalArgumentException exception) {
            ctx.showError().accept(ctx.t("plan.invalid"));
        } catch (SQLException exception) {
            ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
        }
    }
}
