package com.gym.system.controller.helper;

import com.google.zxing.WriterException;
import com.gym.system.dao.GymRepository;
import com.gym.system.util.QrCodeService;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

import java.util.function.DoubleFunction;
import java.util.function.Function;

public final class DocumentFactory {
    public static final double SCALE = 2.0;
    private static final double QR_SIZE = 200;
    private static final String FONT = "-fx-font-family: 'Segoe UI', 'Tahoma', 'Arial', sans-serif;";

    private final Function<String, String> t;
    private final DoubleFunction<String> money;
    private final boolean arabic;

    public DocumentFactory(Function<String, String> t, DoubleFunction<String> money, boolean arabic) {
        this.t = t;
        this.money = money;
        this.arabic = arabic;
    }

    public Parent memberCard(GymRepository.MemberRow member) throws WriterException {
        String code = QrCodeService.memberCode(member.id());
        VBox card = page(360);

        VBox band = new VBox(2,
                text(t.apply("app.club"), "-fx-text-fill: #fed7aa; -fx-font-size: 11px; -fx-font-weight: bold;"),
                text(t.apply("card.title"), "-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold;"));
        band.setPadding(new Insets(18, 24, 16, 24));
        band.setStyle("-fx-background-color: #f47721;");

        VBox body = new VBox(8);
        body.setPadding(new Insets(20, 24, 24, 24));
        body.setAlignment(Pos.TOP_CENTER);
        body.getChildren().addAll(
                text(member.fullName(), "-fx-text-fill: #0f172a; -fx-font-size: 22px; -fx-font-weight: bold;"),
                text(member.phone(), "-fx-text-fill: #475569; -fx-font-size: 13px;"));
        String planLine = planLine(member);
        if (!planLine.isBlank()) {
            body.getChildren().add(text(planLine, "-fx-text-fill: #475569; -fx-font-size: 12px;"));
        }
        ImageView qr = qr(code);
        VBox.setMargin(qr, new Insets(10, 0, 4, 0));
        body.getChildren().addAll(qr,
                text(code, "-fx-text-fill: #0f172a; -fx-font-size: 16px; -fx-font-weight: bold;"
                        + " -fx-font-family: 'Consolas', 'Courier New', monospace;"),
                text(t.apply("card.hint"), "-fx-text-fill: #64748b; -fx-font-size: 11px;"));

        card.getChildren().addAll(band, body);
        return card;
    }

    public Parent receipt(GymRepository.SubscriptionInvoice invoice) throws WriterException {
        boolean paid = "PAID".equals(invoice.paymentStatus());
        String code = invoice.paymentId() > 0
                ? QrCodeService.receiptCode(invoice.paymentId())
                : "INV-" + invoice.subscriptionId();
        VBox page = page(440);

        VBox band = new VBox(2,
                text(t.apply("app.club"), "-fx-text-fill: #fed7aa; -fx-font-size: 11px; -fx-font-weight: bold;"),
                text(t.apply(paid ? "receipt.title" : "receipt.pendingTitle"),
                        "-fx-text-fill: #ffffff; -fx-font-size: 20px; -fx-font-weight: bold;"));
        band.setPadding(new Insets(18, 24, 16, 24));
        band.setStyle("-fx-background-color: #f47721;");

        VBox body = new VBox(7);
        body.setPadding(new Insets(18, 24, 22, 24));
        body.getChildren().add(row(t.apply("receipt.number"), code));
        if (!invoice.paymentDate().isBlank()) {
            body.getChildren().add(row(t.apply("receipt.date"), invoice.paymentDate()));
        }
        body.getChildren().addAll(divider(),
                row(t.apply("receipt.member"), invoice.member()),
                row(t.apply("members.phone"), invoice.phone()),
                divider(),
                row(t.apply("receipt.item"), invoice.planName()
                        + " (" + t.apply("subscription." + invoice.planType()) + ")"));
        String activity = arabic ? invoice.activityArabic() : invoice.activityEnglish();
        if (activity != null && !activity.isBlank()) {
            body.getChildren().add(row(t.apply("receipt.class"), activity));
        }
        if ("SESSION_BASED".equals(invoice.planType())) {
            body.getChildren().add(row(t.apply("receipt.sessions"),
                    invoice.remainingSessions() + " / " + invoice.totalSessions()));
        } else {
            body.getChildren().add(row(t.apply("receipt.period"),
                    invoice.startDate() + "  →  " + invoice.endDate()));
        }
        body.getChildren().add(divider());

        Label total = text(money.apply(invoice.amount()),
                "-fx-text-fill: #0f172a; -fx-font-size: 24px; -fx-font-weight: bold;");
        Label status = text(t.apply("status." + invoice.paymentStatus()), paid
                ? "-fx-text-fill: #047857; -fx-background-color: #d1fae5; -fx-font-size: 12px; -fx-font-weight: bold;"
                        + " -fx-padding: 4 12 4 12; -fx-background-radius: 12;"
                : "-fx-text-fill: #92400e; -fx-background-color: #fde68a; -fx-font-size: 12px; -fx-font-weight: bold;"
                        + " -fx-padding: 4 12 4 12; -fx-background-radius: 12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox totalRow = new HBox(8,
                text(t.apply("receipt.total"), "-fx-text-fill: #475569; -fx-font-size: 13px; -fx-font-weight: bold;"),
                spacer, total);
        totalRow.setAlignment(Pos.CENTER_LEFT);
        HBox statusRow = new HBox(status);
        statusRow.setAlignment(Pos.CENTER_RIGHT);
        body.getChildren().addAll(totalRow, statusRow);

        ImageView qr = qr(code);
        VBox qrBox = new VBox(6, qr, text(code, "-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-weight: bold;"
                + " -fx-font-family: 'Consolas', 'Courier New', monospace;"));
        qrBox.setAlignment(Pos.CENTER);
        VBox.setMargin(qrBox, new Insets(10, 0, 0, 0));
        body.getChildren().addAll(qrBox,
                centered(text(t.apply("receipt.thanks"), "-fx-text-fill: #64748b; -fx-font-size: 12px;")));

        page.getChildren().addAll(band, body);
        return page;
    }

    private VBox page(double width) {
        VBox page = new VBox();
        page.setMinWidth(width);
        page.setPrefWidth(width);
        page.setMaxWidth(width);
        page.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-width: 1; " + FONT);
        page.setNodeOrientation(arabic ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        return page;
    }

    private ImageView qr(String code) throws WriterException {
        ImageView view = new ImageView(QrCodeService.createImage(code, (int) (QR_SIZE * SCALE)));
        view.setFitWidth(QR_SIZE);
        view.setFitHeight(QR_SIZE);
        view.setPreserveRatio(true);
        view.setSmooth(false);
        return view;
    }

    private String planLine(GymRepository.MemberRow member) {
        if (member.subscriptionId() == 0) {
            return "";
        }
        String activity = arabic ? member.activityArabic() : member.activityEnglish();
        StringBuilder line = new StringBuilder(member.plan());
        if (activity != null && !activity.isBlank()) {
            line.append("  •  ").append(activity);
        }
        return line.toString();
    }

    private HBox row(String label, String value) {
        Label key = text(label, "-fx-text-fill: #64748b; -fx-font-size: 12px;");
        key.setMinWidth(110);
        Label val = text(value, "-fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: bold;");
        val.setWrapText(true);
        val.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(val, Priority.ALWAYS);
        HBox row = new HBox(12, key, val);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private Region divider() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #e2e8f0;");
        VBox.setMargin(line, new Insets(4, 0, 4, 0));
        return line;
    }

    private HBox centered(Label label) {
        HBox box = new HBox(label);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label text(String value, String style) {
        Label label = new Label(value == null ? "" : value);
        label.setStyle(style);
        label.setWrapText(true);
        return label;
    }
}
