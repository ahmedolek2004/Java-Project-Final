package com.gym.system.controller.page;

import com.gym.system.controller.form.CheckInForm;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.AttendanceService;
import javafx.application.Platform;
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

public class AttendancePage {
    private final AttendanceService service;
    private final PageContext ctx;

    public AttendancePage(AttendanceService service, PageContext ctx) {
        this.service = service;
        this.ctx = ctx;
    }

    public Node render() throws SQLException {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");

        // QR Scanner Box
        TextField qrInput = new TextField();
        qrInput.setPromptText(ctx.t("attendance.scanPrompt"));

        Label scanResult = new Label(ctx.t("attendance.scanReady"));
        scanResult.getStyleClass().add("scan-result");

        Button scanBtn = new Button(ctx.t("attendance.scan"));
        scanBtn.getStyleClass().add("primary-button");

        HBox scanner = new HBox(10, qrInput, scanBtn);
        HBox.setHgrow(qrInput, Priority.ALWAYS);

        Label scanTitle = new Label(ctx.t("attendance.scanTitle"));
        scanTitle.getStyleClass().add("scan-title");

        VBox scanPanel = new VBox(8, scanTitle, scanner, scanResult);
        scanPanel.getStyleClass().add("scan-panel");

        // History Search & Manual Check-in
        TextField search = new TextField();
        search.setPromptText(ctx.t("members.search"));

        Button manualCheckInBtn = new Button("👤 " + ctx.t("attendance.checkin"));
        manualCheckInBtn.getStyleClass().add("primary-button");

        HBox toolbar = new HBox(10, search, manualCheckInBtn);
        HBox.setHgrow(search, Priority.ALWAYS);

        TableView<GymRepository.Attendance> table = TableFactory.createTable(ctx.t("table.empty"));
        javafx.collections.ObservableList<GymRepository.Attendance> masterList = FXCollections.observableArrayList(service.getAllAttendance());
        FilteredList<GymRepository.Attendance> filtered = new FilteredList<>(masterList, a -> true);

        Runnable refreshTable = () -> {
            try {
                masterList.setAll(service.getAllAttendance());
            } catch (SQLException e) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + e.getMessage());
            }
        };

        search.textProperty().addListener((obs, oldVal, query) -> {
            String q = query == null ? "" : query.trim().toLowerCase();
            filtered.setPredicate(a -> q.isEmpty()
                    || a.member().toLowerCase().contains(q)
                    || a.phone().toLowerCase().contains(q)
                    || a.plan().toLowerCase().contains(q));
        });

        table.setItems(filtered);

        TableFactory.addColumn(table, ctx.t("members.name"), GymRepository.Attendance::member, 1.2);
        TableFactory.addColumn(table, ctx.t("members.phone"), GymRepository.Attendance::phone, 1);
        TableFactory.addColumn(table, ctx.t("subscription.plan"), GymRepository.Attendance::plan, 1);
        TableFactory.addColumn(table, ctx.t("attendance.time"), GymRepository.Attendance::checkedInAt, 1.3);

        // QR Scan Handler
        Runnable handleScan = () -> {
            String code = qrInput.getText().trim();
            qrInput.clear();
            if (code.isEmpty()) {
                qrInput.requestFocus();
                return;
            }
            try {
                GymRepository.CheckInResult result = service.checkInByQrCode(code);
                scanResult.setText(checkInSummary(result));
                scanResult.getStyleClass().remove("scan-error");
                if (!scanResult.getStyleClass().contains("scan-success")) {
                    scanResult.getStyleClass().add("scan-success");
                }
                refreshTable.run();
            } catch (IllegalArgumentException e) {
                scanResult.setText(ctx.t("attendance.qrInvalid"));
                setScanError(scanResult);
            } catch (IllegalStateException e) {
                scanResult.setText(ctx.t("attendance.invalid"));
                setScanError(scanResult);
            } catch (SQLException e) {
                if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                    scanResult.setText(ctx.t("error.checkin"));
                } else {
                    scanResult.setText(ctx.t("error.checkin") + " (" + e.getMessage() + ")");
                }
                setScanError(scanResult);
            } finally {
                qrInput.requestFocus();
            }
        };

        scanBtn.setOnAction(e -> handleScan.run());
        qrInput.setOnAction(e -> handleScan.run());

        // Manual Check-in Handler
        manualCheckInBtn.setOnAction(e -> {
            try {
                Optional<Integer> memberId = CheckInForm.show(
                        ctx.repository().members(),
                        ctx.isDarkMode(),
                        ctx.isArabic(),
                        ctx.tFunction()
                );
                memberId.ifPresent(id -> {
                    try {
                        GymRepository.CheckInResult result = service.checkIn(id);
                        ctx.showInfo().accept(checkInSummary(result));
                        refreshTable.run();
                    } catch (IllegalStateException ex) {
                        ctx.showInfo().accept(ctx.t("attendance.invalid"));
                    } catch (SQLException ex) {
                        ctx.showError().accept(ctx.t("error.checkin") + "\n" + ex.getMessage());
                    }
                });
            } catch (SQLException ex) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + ex.getMessage());
            }
        });

        content.getChildren().addAll(scanPanel, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        Platform.runLater(qrInput::requestFocus);
        return content;
    }

    private String checkInSummary(GymRepository.CheckInResult result) {
        String msg = result.memberName() + " • " + ctx.t("subscription.plan") + ": " + result.planName()
                + " (" + ctx.t("subscription." + result.planType()) + ")";
        if (result.sessionBased()) {
            return msg + " • " + ctx.t("attendance.sessionsLeft",
                    result.remainingSessions() + " / " + result.totalSessions());
        }
        return msg + " • " + ctx.t("subscription.end") + ": " + result.endDate();
    }

    private void setScanError(Label label) {
        label.getStyleClass().remove("scan-success");
        if (!label.getStyleClass().contains("scan-error")) {
            label.getStyleClass().add("scan-error");
        }
    }
}