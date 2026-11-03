package com.gym.system.controller.page;

import com.gym.system.controller.form.MemberWizard;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.helper.TableFactory;
import com.gym.system.dao.GymRepository;
import com.gym.system.service.AttendanceService;
import com.gym.system.service.MemberService;
import com.gym.system.service.SubscriptionService;
import com.gym.system.util.QrCodeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.function.IntConsumer;

public final class MemberDetailPanel {
    private final MemberService memberService;
    private final SubscriptionService subscriptionService;
    private final AttendanceService attendanceService;
    private final PageContext ctx;
    private final IntConsumer onChanged;
    private String pendingMessage;
    private boolean pendingError;
    private final VBox box = new VBox(14);
    private final ScrollPane root = new ScrollPane(box);

    public MemberDetailPanel(MemberService memberService, SubscriptionService subscriptionService,
                             AttendanceService attendanceService, PageContext ctx, IntConsumer onChanged) {
        this.memberService = memberService;
        this.subscriptionService = subscriptionService;
        this.attendanceService = attendanceService;
        this.ctx = ctx;
        this.onChanged = onChanged;
        box.setPadding(new Insets(18));
        box.getStyleClass().add("detail-card");
        root.setFitToWidth(true);
        root.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.getStyleClass().addAll("detail-scroll");
        root.setMinWidth(320);
        root.setPrefWidth(360);
        root.setMaxWidth(440);
        show(null);
    }

    public Node node() {
        return root;
    }

    public void announce(String message, boolean error) {
        pendingMessage = message;
        pendingError = error;
    }

    public static String checkInSummary(PageContext ctx, GymRepository.CheckInResult result) {
        String message = result.memberName() + " • " + ctx.t("subscription.plan") + ": " + result.planName()
                + " (" + ctx.t("subscription." + result.planType()) + ")";
        if (result.sessionBased()) {
            return message + " • " + ctx.t("attendance.sessionsLeft",
                    result.remainingSessions() + " / " + result.totalSessions());
        }
        return message + " • " + ctx.t("subscription.end") + ": " + result.endDate();
    }

    public static String checkInProblem(PageContext ctx, Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            return ctx.t("attendance.qrInvalid");
        }
        if (exception instanceof IllegalStateException state) {
            return "PAYMENT_PENDING".equals(state.getMessage())
                    ? ctx.t("attendance.paymentPending") : ctx.t("attendance.invalid");
        }
        return ctx.t("error.checkin");
    }

    public void show(GymRepository.MemberRow member) {
        box.getChildren().clear();
        if (member == null) {
            Label prompt = new Label(ctx.t("members.selectPrompt"));
            prompt.setWrapText(true);
            prompt.getStyleClass().add("detail-placeholder");
            box.getChildren().add(prompt);
            return;
        }
        try {
            List<GymRepository.MemberSubscription> subscriptions = memberService.getSubscriptions(member.id());
            List<GymRepository.MemberVisit> visits = memberService.getRecentVisits(member.id(), 5);
            build(member, subscriptions, visits);
        } catch (SQLException exception) {
            ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
        }
    }

    private void build(GymRepository.MemberRow member, List<GymRepository.MemberSubscription> subscriptions,
                       List<GymRepository.MemberVisit> visits) {
        box.getChildren().add(header(member));

        Button checkInBtn = new Button("✓ " + ctx.t("attendance.scan"));
        checkInBtn.getStyleClass().add("primary-button");
        checkInBtn.setDisable(member.subscriptionId() == 0);
        Button editBtn = new Button(ctx.t("members.edit"));
        Button deleteBtn = new Button(ctx.t("members.delete"));
        deleteBtn.setVisible(ctx.isAdmin());
        deleteBtn.setManaged(ctx.isAdmin());
        Button addSubscriptionBtn = new Button(ctx.t("subscription.add"));
        Button toggleBtn = new Button(ctx.t("subscription.freeze"));
        Button payBtn = new Button(ctx.t("finance.confirmReceived"));
        Button receiptBtn = new Button("🧾 " + ctx.t("receipt.view"));
        Button cardBtn = new Button("▦ " + ctx.t("card.view"));
        FlowPane actions = new FlowPane(8, 8, checkInBtn, addSubscriptionBtn, editBtn, deleteBtn, toggleBtn, payBtn,
                receiptBtn, cardBtn);
        box.getChildren().add(actions);
        if (pendingMessage != null) {
            Label banner = new Label(pendingMessage);
            banner.setWrapText(true);
            banner.setMaxWidth(Double.MAX_VALUE);
            banner.getStyleClass().addAll("scan-result", pendingError ? "scan-error" : "scan-success");
            box.getChildren().add(banner);
            pendingMessage = null;
        }

        box.getChildren().add(heading(ctx.t("members.membership")));
        if (member.subscriptionId() == 0) {
            Label none = new Label(ctx.t("members.noSubscription"));
            none.setWrapText(true);
            none.getStyleClass().add("detail-placeholder");
            box.getChildren().add(none);
        } else {
            box.getChildren().add(membershipGrid(member));
        }

        TableView<GymRepository.MemberSubscription> history = null;
        if (!subscriptions.isEmpty()) {
            box.getChildren().add(heading(ctx.t("members.history")));
            history = historyTable(subscriptions, member.subscriptionId());
            box.getChildren().add(history);
        }

        box.getChildren().add(heading(ctx.t("members.visits")));
        if (visits.isEmpty()) {
            box.getChildren().add(mutedLine(ctx.t("members.noVisits")));
        } else {
            for (GymRepository.MemberVisit visit : visits) {
                box.getChildren().add(line(visit.checkedInAt() + "  •  " + visit.plan()));
            }
        }

        wireActions(member, history, checkInBtn, editBtn, deleteBtn, addSubscriptionBtn, toggleBtn, payBtn, receiptBtn,
                cardBtn);
    }

    private void wireActions(GymRepository.MemberRow member,
                             TableView<GymRepository.MemberSubscription> history,
                             Button checkInBtn, Button editBtn, Button deleteBtn, Button addSubscriptionBtn,
                             Button toggleBtn, Button payBtn, Button receiptBtn, Button cardBtn) {
        Runnable updateButtons = () -> {
            GymRepository.MemberSubscription selected = history == null ? null
                    : history.getSelectionModel().getSelectedItem();
            boolean switchable = selected != null
                    && ("ACTIVE".equals(selected.status()) || "FROZEN".equals(selected.status()));
            toggleBtn.setDisable(!switchable);
            toggleBtn.setText(selected != null && "FROZEN".equals(selected.status())
                    ? ctx.t("subscription.unfreeze") : ctx.t("subscription.freeze"));
            payBtn.setDisable(selected == null || !"PENDING".equalsIgnoreCase(selected.paymentStatus()));
            receiptBtn.setDisable(selected == null);
        };
        if (history != null) {
            history.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> updateButtons.run());
        }
        updateButtons.run();

        checkInBtn.setOnAction(event -> {
            try {
                announce(checkInSummary(ctx, attendanceService.checkIn(member.id())), false);
            } catch (IllegalStateException | IllegalArgumentException exception) {
                announce(checkInProblem(ctx, exception), true);
            } catch (SQLException exception) {
                announce(checkInProblem(ctx, exception), true);
            }
            onChanged.accept(member.id());
        });

        editBtn.setOnAction(event -> {
            try {
                Integer currentClass = member.classId() == 0 ? null : member.classId();
                MemberWizard.showEdit(member, memberService.getAvailableClasses(currentClass),
                        ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction(), ctx::money).ifPresent(edit -> {
                    try {
                        memberService.updateMember(member.id(), edit.member());
                        if (edit.classChanged()) {
                            subscriptionService.changeClass(member.subscriptionId(), edit.classId());
                        }
                        onChanged.accept(member.id());
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

        deleteBtn.setOnAction(event -> {
            String prompt = ctx.t("members.deleteConfirm", member.fullName());
            if (!Boolean.TRUE.equals(ctx.confirm().apply(prompt))) {
                return;
            }
            try {
                memberService.deleteMember(member.id());
                onChanged.accept(-1);
                ctx.showInfo().accept(ctx.t("members.deleted"));
            } catch (SQLException | RuntimeException exception) {
                ctx.showError().accept(ctx.t("error.delete") + "\n" + exception.getMessage());
            }
        });

        addSubscriptionBtn.setOnAction(event -> {
            try {
                Integer currentClass = member.classId() == 0 ? null : member.classId();
                MemberWizard.showRenew(member, ctx.repository().plans(), memberService.getAvailableClasses(currentClass),
                        ctx.isDarkMode(), ctx.isArabic(), ctx.tFunction(), ctx::money).ifPresent(renewal -> {
                    try {
                        subscriptionService.addSubscription(renewal.subscription(), renewal.paidNow());
                        onChanged.accept(member.id());
                    } catch (IllegalStateException exception) {
                        ctx.showError().accept(ctx.t("wizard.classFull"));
                    } catch (Exception exception) {
                        ctx.showError().accept(ctx.t("error.subscription") + "\n" + exception.getMessage());
                    }
                });
            } catch (SQLException exception) {
                ctx.showError().accept(ctx.t("error.load") + "\n" + exception.getMessage());
            }
        });

        toggleBtn.setOnAction(event -> {
            GymRepository.MemberSubscription selected = history.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            String next = "ACTIVE".equals(selected.status()) ? "FROZEN" : "ACTIVE";
            try {
                subscriptionService.setSubscriptionStatus(selected.id(), next);
                onChanged.accept(member.id());
            } catch (Exception exception) {
                ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
            }
        });

        payBtn.setOnAction(event -> {
            GymRepository.MemberSubscription selected = history.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            if (Boolean.TRUE.equals(ctx.confirm().apply(ctx.t("finance.confirmReceivedPrompt")))) {
                try {
                    subscriptionService.confirmPayment(selected.id());
                    onChanged.accept(member.id());
                } catch (Exception exception) {
                    ctx.showError().accept(ctx.t("error.save") + "\n" + exception.getMessage());
                }
            }
        });

        receiptBtn.setOnAction(event -> {
            GymRepository.MemberSubscription selected = history.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ctx.printController().showReceipt(selected.id());
            }
        });

        cardBtn.setOnAction(event -> ctx.printController().showMemberCard(member));
    }

    private Node header(GymRepository.MemberRow member) {
        Label avatar = new Label(initials(member.fullName()));
        avatar.getStyleClass().add("detail-avatar");
        Label name = new Label(member.fullName());
        name.setWrapText(true);
        name.getStyleClass().add("detail-name");
        String contact = member.email().isBlank() ? member.phone() : member.phone() + "  •  " + member.email();
        Label details = new Label(contact);
        details.setWrapText(true);
        details.getStyleClass().add("detail-contact");
        VBox identity = new VBox(3, name, details);
        identity.setMinWidth(0);
        HBox.setHgrow(identity, Priority.ALWAYS);
        Label pill = pill(member.status());
        pill.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = new HBox(12, avatar, identity, pill);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node membershipGrid(GymRepository.MemberRow member) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(7);
        int row = 0;
        addRow(grid, row++, ctx.t("members.code"), QrCodeService.memberCode(member.id()));
        addRow(grid, row++, ctx.t("subscription.plan"),
                member.plan() + " (" + ctx.t("subscription." + member.planType()) + ")");
        addRow(grid, row++, ctx.t("members.class"), classLabel(member));
        if ("SESSION_BASED".equals(member.planType())) {
            addRow(grid, row++, ctx.t("members.sessions"), member.remaining());
        } else {
            addRow(grid, row++, ctx.t("members.period"), member.startDate() + "  →  " + member.endDate());
        }
        addRow(grid, row++, ctx.t("members.price"), ctx.money(member.price()));
        addRow(grid, row++, ctx.t("members.paymentStatus"), ctx.t("status." + member.paymentStatus()));
        if (member.medicalNotes() != null && !member.medicalNotes().isBlank()) {
            addRow(grid, row, ctx.t("members.notes"), member.medicalNotes());
        }
        return grid;
    }

    private TableView<GymRepository.MemberSubscription> historyTable(
            List<GymRepository.MemberSubscription> subscriptions, int currentId) {
        TableView<GymRepository.MemberSubscription> table = TableFactory.createTable(ctx.t("table.empty"));
        table.getStyleClass().add("compact-table");
        table.getItems().setAll(subscriptions);
        TableFactory.addColumn(table, ctx.t("subscription.plan"), GymRepository.MemberSubscription::plan, 1.2);
        TableFactory.addColumn(table, ctx.t("members.class"),
                row -> row.classTitle().isBlank()
                        ? (ctx.isArabic() ? row.activityArabic() : row.activityEnglish()) : row.classTitle(), 1);
        TableFactory.addColumn(table, ctx.t("members.status"), row -> ctx.t("status." + row.status()), .9);
        TableFactory.addColumn(table, ctx.t("members.paymentStatus"), row -> ctx.t("status." + row.paymentStatus()), .9);
        table.getColumns().forEach(column -> column.setMinWidth(55));
        table.setFixedCellSize(34);
        double height = 46 + 34 * Math.min(subscriptions.size(), 4) + 4;
        table.setMinHeight(height);
        table.setPrefHeight(height);
        table.setMaxHeight(height);
        for (GymRepository.MemberSubscription item : subscriptions) {
            if (item.id() == currentId) {
                table.getSelectionModel().select(item);
                break;
            }
        }
        if (table.getSelectionModel().getSelectedItem() == null) {
            table.getSelectionModel().selectFirst();
        }
        return table;
    }

    private String classLabel(GymRepository.MemberRow member) {
        if (member.classId() == 0) {
            String activity = ctx.isArabic() ? member.activityArabic() : member.activityEnglish();
            return activity == null || activity.isBlank() ? "-" : activity;
        }
        return member.classTitle() + "  •  " + ctx.t("day." + member.classDay()) + " " + member.classStart() + "–"
                + member.classEnd();
    }

    private Label pill(String status) {
        Label pill = new Label(ctx.t("status." + status));
        pill.getStyleClass().addAll("status-pill", "status-" + status);
        return pill;
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("detail-heading");
        return label;
    }

    private Label line(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("detail-line");
        return label;
    }

    private Label mutedLine(String text) {
        Label label = line(text);
        label.getStyleClass().add("detail-placeholder");
        return label;
    }

    private void addRow(GridPane grid, int row, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("detail-key");
        Label valueLabel = new Label(value);
        valueLabel.setWrapText(true);
        valueLabel.getStyleClass().add("detail-value");
        GridPane.setHgrow(valueLabel, Priority.ALWAYS);
        grid.add(keyLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private String initials(String name) {
        String[] words = name.trim().split("\\s+");
        String first = words[0].isEmpty() ? "?" : words[0].substring(0, 1);
        return words.length > 1 ? (first + words[1].substring(0, 1)).toUpperCase() : first.toUpperCase();
    }
}
