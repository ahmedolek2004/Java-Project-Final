package com.gym.system.controller.page;

import com.gym.system.controller.form.SubscriptionForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Subscription;
import com.gym.system.service.SubscriptionService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class SubscriptionsPage {
    private final SubscriptionService service;
    private final PageContext ctx;

    public SubscriptionsPage(SubscriptionService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                ctx.t("common.none"),
                ctx.t("status.ACTIVE"),
                ctx.t("status.FROZEN"),
                ctx.t("status.EXPIRED")
        ));
        statusFilter.getSelectionModel().selectFirst();

        Button addBtn = new Button(ctx.t("subscription.add"));
        addBtn.getStyleClass().add("primary-button");

        Button toggleBtn = new Button(ctx.t("subscription.toggle"));
        Button confirmPayBtn = new Button(ctx.t("finance.confirmReceived"));
        Button printInvoiceBtn = new Button(ctx.t("print.invoice"));

        HBox toolbar = new HBox(10, search, statusFilter, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, toggleBtn, confirmPayBtn, printInvoiceBtn);

        TableView<GymRepository.SubscriptionRow> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.SubscriptionRow> masterList = FXCollections.observableArrayList(service.getAllSubscriptions());
        FilteredList<GymRepository.SubscriptionRow> filtered = new FilteredList<>(masterList, s -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getAllSubscriptions());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        Runnable updateFilter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String status = statusFilter.getValue();
            filtered.setPredicate(s -> {
                boolean matchesQuery = query.isEmpty()
                        || s.member().toLowerCase().contains(query)
                        || s.plan().toLowerCase().contains(query)
                        || s.type().toLowerCase().contains(query)
                        || s.paymentStatus().toLowerCase().contains(query);

                boolean matchesStatus = status == null || status.equals(ctx.t("common.none"))
                        || ctx.t("status." + s.status()).equalsIgnoreCase(status);

                return matchesQuery && matchesStatus;
            });
        };

        search.textProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> updateFilter.run());

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.SubscriptionRow::member, 1.2);
        TableFactory.addColumn(table, ctx.t("subscription.plan"), GymRepository.SubscriptionRow::plan, 1.1);
        TableFactory.addColumn(table, ctx.t("subscription.type"), row -> ctx.t("subscription." + row.type()), 1);
        TableFactory.addColumn(table, ctx.t("subscription.start"), GymRepository.SubscriptionRow::startDate, 1);
        TableFactory.addColumn(table, ctx.t("subscription.end"), GymRepository.SubscriptionRow::endDate, 1);
        TableFactory.addColumn(table, ctx.t("subscription.sessions"), GymRepository.SubscriptionRow::remaining, .8);
        TableFactory.addColumn(table, ctx.t("finance.amount"), row -> ctx.money(row.price()), .9);
        TableFactory.addColumn(table, ctx.t("members.status"), row -> ctx.t("status." + row.status()), .8);
        TableFactory.addColumn(table, ctx.t("finance.status"), row -> ctx.t("status." + row.paymentStatus()), .9);

        // Actions Wiring
        addBtn.setOnAction(e -> {
            try {
                Optional<Subscription> sub = SubscriptionForm.show(
                        ctx.repository().members(),
                        ctx.repository().activities(),
                        ctx.isDarkMode(),
                        ctx.isArabic(),
                        ctx.tFunction()
                );
                sub.ifPresent(s -> {
                    try {
                        service.addSubscription(s);
                        refreshTable.run();
                    } catch (Exception ex) {
                        ctx.showError().accept(ctx.t("error.subscription") + "\n" + ex.getMessage());
                    }
                });
            } catch (SQLException ex) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + ex.getMessage());
            }
        });

        toggleBtn.setOnAction(e -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("subscription.select"));
                return;
            }
            if (!"ACTIVE".equals(selected.status()) && !"FROZEN".equals(selected.status())) {
                ctx.showInfo().accept(ctx.t("subscription.cannotToggle"));
                return;
            }
            String nextStatus = "ACTIVE".equals(selected.status()) ? "FROZEN" : "ACTIVE";
            try {
                service.setSubscriptionStatus(selected.id(), nextStatus);
                refreshTable.run();
            } catch (Exception ex) {
                ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
            }
        });

        confirmPayBtn.setOnAction(e -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("subscription.select"));
                return;
            }
            if (!"PENDING".equalsIgnoreCase(selected.paymentStatus())) {
                ctx.showInfo().accept(ctx.t("finance.paymentNotPending"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("finance.confirmReceivedPrompt")))) {
                try {
                    service.confirmPayment(selected.id());
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            }
        });

        printInvoiceBtn.setOnAction(e -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("subscription.select"));
                return;
            }
            ctx.printController().printInvoice(selected.id());
        });

        content.getChildren().addAll(toolbar, actionsBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }
}