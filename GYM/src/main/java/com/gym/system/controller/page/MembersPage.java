package com.gym.system.controller.page;

import com.gym.system.controller.form.MemberWizard;
import com.gym.system.controller.helper.DataExportHelper;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.AttendanceService;
import com.gym.system.service.MemberService;
import com.gym.system.service.SubscriptionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class MembersPage {
    private static final List<String> STATUS_FILTERS = List.of("ALL", "ACTIVE", "FROZEN", "UNPAID", "EXPIRED", "NONE");

    private final MemberService memberService;
    private final SubscriptionService subscriptionService;
    private final AttendanceService attendanceService;
    private final PageContext ctx;
    private final boolean openAddWizard;

    public MembersPage(MemberService memberService, SubscriptionService subscriptionService,
                       AttendanceService attendanceService, PageContext ctx, boolean openAddWizard) {
        this.memberService = memberService;
        this.subscriptionService = subscriptionService;
        this.attendanceService = attendanceService;
        this.ctx = ctx;
        this.openAddWizard = openAddWizard;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        ComboBox<String> statusFilter = new ComboBox<>(FXCollections.observableArrayList(
                ctx.t("common.all"),
                ctx.t("status.ACTIVE"),
                ctx.t("status.FROZEN"),
                ctx.t("status.UNPAID"),
                ctx.t("status.EXPIRED"),
                ctx.t("status.NONE")
        ));
        statusFilter.getSelectionModel().selectFirst();

        Button addBtn = new Button(ctx.t("members.add"));
        addBtn.getStyleClass().add("primary-button");
        Button exportBtn = new Button("📥 " + (ctx.isArabic() ? "تصدير إلى CSV" : "Export CSV"));

        HBox toolbar = new HBox(10, search, statusFilter, addBtn, exportBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        TextField scanField = new TextField();
        scanField.setPromptText(ctx.t("attendance.scanPrompt"));
        Button scanBtn = new Button("▦ " + ctx.t("attendance.scan"));
        HBox scanBar = new HBox(10, scanField, scanBtn);
        HBox.setHgrow(scanField, Priority.ALWAYS);
        Label scanResult = new Label();
        scanResult.setWrapText(true);
        scanResult.setMaxWidth(Double.MAX_VALUE);
        scanResult.getStyleClass().add("scan-result");
        scanResult.setManaged(false);
        scanResult.setVisible(false);
        VBox scanBox = new VBox(8, scanBar, scanResult);

        TableView<GymRepository.MemberRow> table = TableFactory.createTable(ctx.t("table.empty"));
        ObservableList<GymRepository.MemberRow> masterList =
                FXCollections.observableArrayList(memberService.getAllMembers());
        FilteredList<GymRepository.MemberRow> filtered = new FilteredList<>(masterList, member -> true);
        table.setItems(filtered);
        table.setMinHeight(180);
        table.getStyleClass().add("members-table");

        MemberDetailPanel detail = new MemberDetailPanel(memberService, subscriptionService, attendanceService, ctx,
                memberId -> refresh(masterList, table, memberId));

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.MemberRow::fullName, 1.6);
        TableFactory.addColumn(table, ctx.t("members.phone"), GymRepository.MemberRow::phone, 1.35);
        TableFactory.addColumn(table, ctx.t("members.class"), this::classText, 1.2);
        TableFactory.addColumn(table, ctx.t("members.left"), GymRepository.MemberRow::validity, 1.1);
        TableFactory.addColumn(table, ctx.t("members.paymentStatus"), this::paymentText, .9);
        TableFactory.addColumn(table, ctx.t("members.status"), row -> ctx.t("status." + row.status()), 1.1);
        table.getColumns().forEach(column -> column.setMinWidth(70));

        Runnable updateFilter = () -> {
            String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            int index = Math.max(0, statusFilter.getSelectionModel().getSelectedIndex());
            String status = STATUS_FILTERS.get(index);
            filtered.setPredicate(member -> {
                boolean matchesQuery = query.isEmpty()
                        || member.fullName().toLowerCase().contains(query)
                        || member.phone().toLowerCase().contains(query)
                        || member.email().toLowerCase().contains(query)
                        || member.plan().toLowerCase().contains(query)
                        || classText(member).toLowerCase().contains(query);
                boolean matchesStatus = "ALL".equals(status) || status.equals(member.status());
                return matchesQuery && matchesStatus;
            });
        };
        search.textProperty().addListener((obs, oldValue, newValue) -> updateFilter.run());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> updateFilter.run());

        table.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> detail.show(newValue));

        addBtn.setOnAction(event -> {
            try {
                MemberWizard.showAdd(ctx.repository().plans(), memberService.getAvailableClasses(null),
                        ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction(), ctx::money).ifPresent(request -> {
                    try {
                        int memberId = memberService.registerMember(request.member(), request.subscription(),
                                request.paidNow());
                        search.clear();
                        statusFilter.getSelectionModel().selectFirst();
                        refresh(masterList, table, memberId);
                    } catch (IllegalStateException exception) {
                        ctx.showError().accept(ctx.t("wizard.classFull"));
                    } catch (Exception exception) {
                        ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
                    }
                });
            } catch (SQLException exception) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
            }
        });

        Runnable scan = () -> {
            String code = scanField.getText().trim();
            scanField.clear();
            if (code.isEmpty()) {
                scanField.requestFocus();
                return;
            }
            try {
                GymRepository.CheckInResult result = attendanceService.checkInByQrCode(code);
                String summary = MemberDetailPanel.checkInSummary(ctx, result);
                detail.announce(summary, false);
                setScanResult(scanResult, summary, false);
                search.clear();
                statusFilter.getSelectionModel().selectFirst();
                refresh(masterList, table, result.memberId());
            } catch (IllegalArgumentException | IllegalStateException | SQLException exception) {
                setScanResult(scanResult, MemberDetailPanel.checkInProblem(ctx, exception), true);
            } finally {
                scanField.requestFocus();
            }
        };
        scanBtn.setOnAction(event -> scan.run());
        scanField.setOnAction(event -> scan.run());

        exportBtn.setOnAction(event -> DataExportHelper.exportMembers(
                exportBtn.getScene().getWindow(),
                filtered,
                ctx.tFunction(),
                path -> ctx.showInfo().accept((ctx.isArabic() ? "تم التصدير بنجاح:\n" : "Exported successfully:\n") + path),
                error -> ctx.showError().accept(error)
        ));

        HBox body = new HBox(14, table, detail.node());
        HBox.setHgrow(table, Priority.ALWAYS);
        content.getChildren().addAll(toolbar, scanBox, body);
        VBox.setVgrow(body, Priority.ALWAYS);
        if (openAddWizard) {
            Platform.runLater(addBtn::fire);
        }
        return content;
    }

    private String classText(GymRepository.MemberRow row) {
        if (row.subscriptionId() == 0) {
            return "-";
        }
        if (row.classId() != 0) {
            return row.classTitle();
        }
        String activity = ctx.isArabic() ? row.activityArabic() : row.activityEnglish();
        return activity == null || activity.isBlank() ? "-" : activity;
    }

    private String paymentText(GymRepository.MemberRow row) {
        return row.subscriptionId() == 0 ? "-" : ctx.t("status." + row.paymentStatus());
    }

    private void setScanResult(Label label, String message, boolean error) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
        label.getStyleClass().removeAll("scan-success", "scan-error");
        label.getStyleClass().add(error ? "scan-error" : "scan-success");
    }

    private void refresh(ObservableList<GymRepository.MemberRow> masterList,
                         TableView<GymRepository.MemberRow> table, int memberId) {
        try {
            masterList.setAll(memberService.getAllMembers());
            for (GymRepository.MemberRow row : table.getItems()) {
                if (row.id() == memberId) {
                    table.getSelectionModel().select(row);
                    table.scrollTo(row);
                    break;
                }
            }
        } catch (SQLException exception) {
            ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
        }
    }
}
