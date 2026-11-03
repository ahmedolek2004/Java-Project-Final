package com.gym.system.controller.page;

import com.gym.system.controller.form.ExpenseForm;
import com.gym.system.controller.helper.DataExportHelper;
import com.gym.system.controller.helper.MetricCardFactory;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.FinanceService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class FinancePage {
    private final FinanceService service;
    private final PageContext ctx;

    public FinancePage(FinanceService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        GymRepository.Dashboard stats = ctx.repository().dashboard();
        HBox metrics = new HBox(12,
                MetricCardFactory.createCard(ctx.t("metric.revenue"), ctx.money(stats.revenue()), ctx.t("metric.month")),
                MetricCardFactory.createCard(ctx.t("metric.expenses"), ctx.money(stats.expenses()), ctx.t("metric.month")),
                MetricCardFactory.createCard(ctx.t("metric.net"), ctx.money(stats.revenue() - stats.expenses()), ctx.t("metric.month"))
        );
        metrics.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        Button addExpenseBtn = new Button(ctx.t("finance.addExpense"));
        addExpenseBtn.getStyleClass().add("primary-button");

        Button payrollBtn = new Button(ctx.t("nav.payroll"));
        Button printReportBtn = new Button(ctx.t("print.financialReport"));
        Button exportBtn = new Button("📥 " + (ctx.isArabic() ? "تصدير إلى CSV" : "Export CSV"));

        HBox toolbar = new HBox(10, search, addExpenseBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        HBox actionsBar = new HBox(10, payrollBtn, printReportBtn, exportBtn);

        TableView<GymRepository.Payment> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.Payment> masterList = FXCollections.observableArrayList(service.getPayments());
        FilteredList<GymRepository.Payment> filtered = new FilteredList<>(masterList, p -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getPayments());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(p -> q.isEmpty()
                    || p.member().toLowerCase().contains(q)
                    || p.description().toLowerCase().contains(q)
                    || p.date().toLowerCase().contains(q));
        });

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.Payment::member, 1);
        TableFactory.addColumn(table, ctx.t("finance.description"), GymRepository.Payment::description, 1.2);
        TableFactory.addColumn(table, ctx.t("finance.amount"), row -> ctx.money(row.amount()), .8);
        TableFactory.addColumn(table, ctx.t("finance.date"), GymRepository.Payment::date, .8);
        TableFactory.addColumn(table, ctx.t("finance.status"), row -> ctx.t("status." + row.status()), .8);

        // Actions Wiring
        addExpenseBtn.setOnAction(e -> {
            Optional<ExpenseForm.ExpenseData> exp = ExpenseForm.show(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            exp.ifPresent(d -> {
                try {
                    service.addExpense(d.description(), d.amount(), d.date());
                    refreshTable.run();
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        payrollBtn.setOnAction(e -> ctx.navigateTo().accept("payroll"));
        printReportBtn.setOnAction(e -> ctx.printController().printFinancialReport());

        exportBtn.setOnAction(e -> {
            DataExportHelper.exportPayments(
                    exportBtn.getScene().getWindow(),
                    filtered,
                    ctx.tFunction(),
                    path -> ctx.showInfo().accept((ctx.isArabic() ? "تم التصدير بنجاح:\n" : "Exported successfully:\n") + path),
                    err -> ctx.showError().accept(err)
            );
        });

        Label historyTitle = new Label(ctx.t("print.paymentHistory"));
        historyTitle.getStyleClass().add("section-title");

        content.getChildren().addAll(metrics, toolbar, actionsBar, historyTitle, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }
}