package com.gym.system.controller.page;

import com.gym.system.controller.form.SubscriptionForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Subscription;
import com.gym.system.service.SubscriptionService;
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

/** Lists every subscription and lets staff add, freeze/unfreeze, confirm payment and print a receipt. */
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

        Button addBtn = new Button(ctx.t("subscription.add"));
        addBtn.getStyleClass().add("primary-button");
        Button toggleBtn = new Button(ctx.t("subscription.toggle"));
        Button payBtn = new Button(ctx.t("finance.confirmReceived"));
        Button receiptBtn = new Button(ctx.t("receipt.view"));

        HBox toolbar = new HBox(10, search, addBtn);
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actionsBar = new HBox(10, toggleBtn, payBtn, receiptBtn);

        TableView<GymRepository.SubscriptionRow> table = TableFactory.createTable(ctx.t("table.empty"));
        ObservableList<GymRepository.SubscriptionRow> masterList =
                FXCollections.observableArrayList(service.getAllSubscriptions());
        FilteredList<GymRepository.SubscriptionRow> filtered = new FilteredList<>(masterList, row -> true);
        table.setItems(filtered);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getAllSubscriptions());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(row -> q.isEmpty()
                    || nullSafe(row.member()).contains(q)
                    || nullSafe(row.plan()).contains(q)
                    || nullSafe(row.status()).contains(q));
        });

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.SubscriptionRow::member, 1.3);
        TableFactory.addColumn(table, ctx.t("subscription.plan"), GymRepository.SubscriptionRow::plan, 1.2);
        TableFactory.addColumn(table, ctx.t("subscription.type"),
                row -> ctx.t("subscription." + row.type()), 1);
        TableFactory.addColumn(table, ctx.t("subscription.start"), GymRepository.SubscriptionRow::startDate, 1);
        TableFactory.addColumn(table, ctx.t("subscription.end"), GymRepository.SubscriptionRow::endDate, 1);
        TableFactory.addColumn(table, ctx.t("members.price"), row -> ctx.money(row.price()), .9);
        TableFactory.addColumn(table, ctx.t("members.status"), row -> ctx.t("status." + row.status()), .9);
        TableFactory.addColumn(table, ctx.t("members.paymentStatus"),
                row -> ctx.t("status." + row.paymentStatus()), .9);

        // Buttons follow the selected row.
        Runnable updateButtons = () -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            boolean switchable = selected != null
                    && ("ACTIVE".equals(selected.status()) || "FROZEN".equals(selected.status()));
            toggleBtn.setDisable(!switchable);
            toggleBtn.setText(selected != null && "FROZEN".equals(selected.status())
                    ? ctx.t("subscription.unfreeze") : ctx.t("subscription.freeze"));
            payBtn.setDisable(selected == null || !"PENDING".equalsIgnoreCase(selected.paymentStatus()));
            receiptBtn.setDisable(selected == null);
        };
        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateButtons.run());
        updateButtons.run();

        addBtn.setOnAction(event -> {
            try {
                Optional<Subscription> subscription = SubscriptionForm.show(
                        ctx.repository().members(), ctx.repository().activities(),
                        ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
                subscription.ifPresent(value -> {
                    try {
                        service.addSubscription(value);
                        refreshTable.run();
                    } catch (Exception exception) {
                        ctx.showError().accept(ctx.t("error.subscription") + "\n" + exception.getMessage());
                    }
                });
            } catch (SQLException exception) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
            }
        });

        toggleBtn.setOnAction(event -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("subscription.select"));
                return;
            }
            String next = "ACTIVE".equals(selected.status()) ? "FROZEN" : "ACTIVE";
            try {
                service.setSubscriptionStatus(selected.id(), next);
                refreshTable.run();
            } catch (Exception exception) {
                ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
            }
        });

        payBtn.setOnAction(event -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("subscription.select"));
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("finance.confirmReceivedPrompt")))) {
                try {
                    service.confirmPayment(selected.id());
                    refreshTable.run();
                } catch (Exception exception) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
                }
            }
        });

        receiptBtn.setOnAction(event -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("subscription.select"));
                return;
            }
            ctx.printController().showReceipt(selected.id());
        });

        content.getChildren().addAll(toolbar, actionsBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
