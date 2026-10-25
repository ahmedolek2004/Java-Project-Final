package com.gym.system.controller.page;

import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.PayrollService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.YearMonth;

public class PayrollPage {
    private final PayrollService service;
    private final PageContext ctx;

    public PayrollPage(PayrollService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField monthField = new TextField(YearMonth.now().toString());
        Button refreshBtn = new Button(ctx.t("payroll.refresh"));
        HBox monthBar = new HBox(10, new Label(ctx.t("payroll.month")), monthField, refreshBtn);
        monthBar.setAlignment(Pos.CENTER_LEFT);

        TableView<GymRepository.Payroll> table = TableFactory.createTable(ctx.t("table.empty"));

        Runnable loadPayroll = () -> {
            try {
                YearMonth ym = YearMonth.parse(monthField.getText().trim());
                table.getItems().setAll(service.getPayroll(ym));
            } catch (Exception e) {
                ctx.showError().accept(ctx.t("error.payroll") + "\n" + e.getMessage());
            }
        };

        refreshBtn.setOnAction(e -> loadPayroll.run());
        monthField.setOnAction(e -> loadPayroll.run());

        loadPayroll.run();

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.Payroll::employeeName, 1.2);
        TableFactory.addColumn(table, ctx.t("team.role"),
                row -> "COACH".equals(row.employeeType()) ? ctx.t("coach.name") : row.role(), 1);
        TableFactory.addColumn(table, ctx.t("team.salary"), row -> ctx.money(row.monthlySalary()), .9);
        TableFactory.addColumn(table, ctx.t("payroll.status"),
                row -> row.paid() ? ctx.t("payroll.paid") : ctx.t("payroll.unpaid"), .8);
        TableFactory.addColumn(table, ctx.t("payroll.paidAt"),
                row -> row.paidAt() == null ? "-" : row.paidAt(), 1.2);

        Button payBtn = new Button(ctx.t("payroll.pay"));
        payBtn.getStyleClass().add("primary-button");

        payBtn.setOnAction(e -> {
            GymRepository.Payroll selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                ctx.showInfo().accept(ctx.t("payroll.select"));
                return;
            }
            if (selected.paid()) {
                ctx.showInfo().accept(ctx.t("payroll.alreadyPaid"));
                return;
            }

            try {
                YearMonth ym = YearMonth.parse(monthField.getText().trim());
                if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("payroll.confirm", selected.employeeName())))) {
                    service.paySalary(selected.employeeType(), selected.id(), ym);
                    loadPayroll.run();
                }
            } catch (Exception ex) {
                ctx.showError().accept(ctx.t("error.payroll") + "\n" + ex.getMessage());
            }
        });

        content.getChildren().addAll(monthBar, payBtn, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }
}