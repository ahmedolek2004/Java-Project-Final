package com.gym.system.controller.page;

import com.gym.system.controller.form.MemberForm;
import com.gym.system.controller.helper.DatabaseBackupHelper;
import com.gym.system.controller.helper.MetricCardFactory;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Member;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Optional;

public class DashboardPage {
    private final PageContext ctx;

    public DashboardPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        GymRepository.Dashboard stats = ctx.repository().dashboard();

        // 1. Welcome Card
        Label appTitle = new Label(ctx.t("app.title"));
        Label welcomeMsg = new Label(ctx.t("dashboard.welcome", ctx.currentUser() != null ? ctx.currentUser().fullName() : ""));
        Label helpText = new Label(ctx.t("dashboard.help"));
        appTitle.getStyleClass().add("welcome-eyebrow");
        welcomeMsg.getStyleClass().add("welcome-title");
        helpText.getStyleClass().add("welcome-text");

        Button addMemberBtn = new Button(ctx.t("members.add"));
        addMemberBtn.getStyleClass().add("primary-button");
        addMemberBtn.setOnAction(e -> {
            Optional<Member> member = MemberForm.showAdd(ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction());
            member.ifPresent(m -> {
                try {
                    ctx.repository().addMember(m);
                    ctx.showInfo().accept(ctx.t("attendance.success"));
                    ctx.navigateTo().accept("members");
                } catch (Exception ex) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + ex.getMessage());
                }
            });
        });

        Button printReportBtn = new Button(ctx.t("print.report"));
        printReportBtn.getStyleClass().add("welcome-report-button");
        printReportBtn.setOnAction(e -> ctx.printController().printDashboardReport());

        Button backupBtn = new Button("💾 " + (ctx.isArabic() ? "نسخ احتياطي للقاعدة" : "Backup Database"));
        backupBtn.setOnAction(e -> {
            DatabaseBackupHelper.backup(
                    addMemberBtn.getScene().getWindow(),
                    path -> ctx.showInfo().accept((ctx.isArabic() ? "تم إنشاء النسخة الاحتياطية بنجاح:\n" : "Backup created successfully:\n") + path),
                    err -> ctx.showError().accept(err)
            );
        });

        HBox welcomeActions = new HBox(10, addMemberBtn, printReportBtn, backupBtn);
        VBox welcomeCard = new VBox(8, appTitle, welcomeMsg, helpText, welcomeActions);
        welcomeCard.getStyleClass().add("welcome-card");

        // 2. Metrics FlowPane
        FlowPane metrics = new FlowPane(12, 12);
        metrics.setPrefWrapLength(1000);
        metrics.getChildren().addAll(
                MetricCardFactory.createCard(ctx.t("metric.members"), Integer.toString(stats.members()), ctx.t("metric.all")),
                MetricCardFactory.createCard(ctx.t("metric.active"), Integer.toString(stats.activeMembers()), ctx.t("metric.membership")),
                MetricCardFactory.createCard(ctx.t("metric.checkins"), Integer.toString(stats.checkInsToday()), ctx.t("metric.today")),
                MetricCardFactory.createCard(ctx.t("metric.revenue"), ctx.money(stats.revenue()), ctx.t("metric.month"))
        );

        // 3. Balance Banner
        Label balance = new Label(ctx.t("metric.expenses") + ": " + ctx.money(stats.expenses())
                + "    " + ctx.t("metric.net") + ": " + ctx.money(stats.revenue() - stats.expenses()));
        balance.getStyleClass().add("section-title");

        // 4. Activity Demand Overview
        Label activityHeading = new Label(ctx.t("report.popularActivities"));
        activityHeading.getStyleClass().add("section-title");

        TableView<GymRepository.ActivityDemand> demandTable = TableFactory.createTable(ctx.t("table.empty"));
        demandTable.getItems().setAll(ctx.repository().activityDemand());
        TableFactory.addColumn(demandTable, ctx.t("activity.title"),
                row -> ctx.isArabic() ? row.nameArabic() : row.nameEnglish(), 1.4);
        TableFactory.addColumn(demandTable, ctx.t("report.members"), row -> Integer.toString(row.members()), .8);
        TableFactory.addColumn(demandTable, ctx.t("report.checkins"), row -> Integer.toString(row.checkIns()), .8);
        TableFactory.addColumn(demandTable, ctx.t("report.revenue"), row -> ctx.money(row.revenue()), 1.1);

        content.getChildren().addAll(welcomeCard, metrics, balance, activityHeading, demandTable);
        VBox.setVgrow(demandTable, Priority.ALWAYS);
        return content;
    }
}
