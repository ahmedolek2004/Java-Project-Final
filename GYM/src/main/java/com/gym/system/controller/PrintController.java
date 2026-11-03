package com.gym.system.controller;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.controller.helper.DocumentFactory;
import com.gym.system.util.AppearanceSettings;
import com.gym.system.util.ImageFiles;
import com.gym.system.util.PrintService;
import com.gym.system.util.QrCodeService;
import com.google.zxing.WriterException;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleFunction;
import java.util.function.Function;

public final class PrintController {
    private final GymDataAccess repository;
    private final Stage stage;
    private final Function<String, String> translate;
    private final DoubleFunction<String> formatMoney;
    private final BooleanSupplier arabic;
    private final BiConsumer<String, Exception> showError;

    public PrintController(GymDataAccess repository, Stage stage,
            Function<String, String> translate, DoubleFunction<String> formatMoney,
            BooleanSupplier arabic, BiConsumer<String, Exception> showError) {
        this.repository = repository;
        this.stage = stage;
        this.translate = translate;
        this.formatMoney = formatMoney;
        this.arabic = arabic;
        this.showError = showError;
    }

    public void showMemberCard(GymRepository.MemberRow member) {
        try {
            Image image = ImageFiles.render(documents().memberCard(member), DocumentFactory.SCALE);
            DocumentPreview.show(text("card.dialogTitle"), image,
                    "member-card-" + QrCodeService.memberCode(member.id()), darkMode(), arabic.getAsBoolean(),
                    translate, showError);
        } catch (WriterException | RuntimeException exception) {
            showError.accept(text("doc.renderFailed"), exception);
        }
    }

    public void showReceipt(int subscriptionId) {
        try {
            GymRepository.SubscriptionInvoice invoice = repository.subscriptionInvoice(subscriptionId);
            Image image = ImageFiles.render(documents().receipt(invoice), DocumentFactory.SCALE);
            String number = invoice.paymentId() > 0
                    ? QrCodeService.receiptCode(invoice.paymentId()) : "INV-" + invoice.subscriptionId();
            DocumentPreview.show(text("receipt.dialogTitle"), image, "receipt-" + number, darkMode(),
                    arabic.getAsBoolean(), translate, showError);
        } catch (SQLException | WriterException | RuntimeException exception) {
            showError.accept(text("doc.renderFailed"), exception);
        }
    }

    public void printDashboardReport() {
        try {
            GymRepository.Dashboard stats = repository.dashboard();
            List<String> details = new ArrayList<>(List.of(
                    text("print.generated") + ": " + LocalDate.now(),
                    text("metric.members") + ": " + stats.members(),
                    text("metric.active") + ": " + stats.activeMembers(),
                    text("metric.checkins") + ": " + stats.checkInsToday(),
                    text("metric.revenue") + ": " + formatMoney.apply(stats.revenue()),
                    text("metric.expenses") + ": " + formatMoney.apply(stats.expenses()),
                    text("metric.net") + ": " + formatMoney.apply(stats.revenue() - stats.expenses())));
            for (GymRepository.ActivityDemand activity : repository.activityDemand()) {
                details.add((arabic.getAsBoolean() ? activity.nameArabic() : activity.nameEnglish())
                        + "  •  " + text("report.members") + ": " + activity.members()
                        + "  •  " + text("report.checkins") + ": " + activity.checkIns());
            }
            sendToPrinter(createPrintDocument(text("print.report"), details, null));
        } catch (SQLException | IllegalStateException exception) {
            showError.accept(text("print.failed"), exception);
        }
    }

    public void printFinancialReport() {
        try {
            GymRepository.Dashboard stats = repository.dashboard();
            List<String> details = new ArrayList<>(List.of(
                    text("print.generated") + ": " + LocalDate.now(),
                    text("metric.revenue") + ": " + formatMoney.apply(stats.revenue()),
                    text("metric.expenses") + ": " + formatMoney.apply(stats.expenses()),
                    text("metric.net") + ": " + formatMoney.apply(stats.revenue() - stats.expenses()),
                    "",
                    text("report.popularActivities")));
            for (GymRepository.ActivityDemand activity : repository.activityDemand()) {
                details.add((arabic.getAsBoolean() ? activity.nameArabic() : activity.nameEnglish())
                        + "  •  " + text("report.members") + ": " + activity.members()
                        + "  •  " + text("report.checkins") + ": " + activity.checkIns()
                        + "  •  " + text("report.revenue") + ": " + formatMoney.apply(activity.revenue()));
            }
            details.add("");
            details.add(text("print.paymentHistory"));
            for (GymRepository.Payment payment : repository.payments()) {
                details.add((payment.date().isBlank() ? "-" : payment.date()) + "  •  " + payment.member() + "  •  "
                        + payment.description() + "  •  " + formatMoney.apply(payment.amount())
                        + "  •  " + text("finance.status") + ": " + text("status." + payment.status()));
            }
            sendToPrinter(createPrintDocument(text("print.financialReport"), details, null));
        } catch (SQLException | IllegalStateException exception) {
            showError.accept(text("print.failed"), exception);
        }
    }

    private VBox createPrintDocument(String title, List<String> details, Image qrImage) {
        VBox document = new VBox(12);
        document.setStyle("-fx-background-color: white; -fx-padding: 28px; -fx-font-family: 'Segoe UI', 'Arial';");
        document.setMinWidth(0);
        applyDirection(document);
        Label heading = new Label(text("app.club"));
        heading.setStyle("-fx-text-fill: #F47721; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #172033; -fx-font-size: 22px; -fx-font-weight: bold;");
        document.getChildren().addAll(heading, titleLabel, new javafx.scene.control.Separator());
        for (String detail : details) {
            Label line = new Label(detail);
            line.setWrapText(true);
            line.setMaxWidth(Double.MAX_VALUE);
            line.setStyle("-fx-text-fill: #273449; -fx-font-size: 13px; -fx-padding: 3px 0;");
            document.getChildren().add(line);
        }
        if (qrImage != null) {
            ImageView qr = new ImageView(qrImage);
            qr.setFitWidth(150);
            qr.setFitHeight(150);
            qr.setPreserveRatio(true);
            document.getChildren().add(qr);
        }
        return document;
    }

    private void sendToPrinter(VBox document) {
        try {
            PrintService.print(stage, document);
        } catch (IllegalStateException exception) {
            showError.accept(text("print.failed"), exception);
        }
    }

    private DocumentFactory documents() {
        return new DocumentFactory(translate, formatMoney, arabic.getAsBoolean());
    }

    private static boolean darkMode() {
        return AppearanceSettings.load().isDarkMode();
    }

    private String text(String key) {
        return translate.apply(key);
    }

    private void applyDirection(Node node) {
        node.setNodeOrientation(arabic.getAsBoolean()
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT);
    }
}
