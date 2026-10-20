package com.gym.system;

import javafx.application.Application;
import com.gym.system.controller.DashboardController;
import com.gym.system.controller.FormDialog;
import com.gym.system.controller.FormDialog.Field;
import com.gym.system.controller.LoginController;
import com.gym.system.controller.PrintController;
import com.gym.system.dao.DatabaseConnection;
import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.User;
import com.gym.system.model.Activity;
import com.gym.system.model.Coach;
import com.gym.system.model.Member;
import com.gym.system.model.Subscription;
import com.gym.system.util.AlertUtils;
import com.gym.system.util.AppearanceSettings;
import com.gym.system.util.LanguageManager;
import com.gym.system.util.QrCodeService;
import com.gym.system.util.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public class GymApplication extends Application {
    private static final String[] SECTIONS = {
            "dashboard", "members", "subscriptions", "attendance", "schedule",
            "coaches", "activities", "finance", "payroll", "team"
    };
    private GymDataAccess repository;
    private User currentUser;
    private Stage stage;
    private LanguageManager languageManager;
    private SessionManager sessionManager;
    private AppearanceSettings appearance;
    private DashboardController dashboardController;
    private LoginController loginController;
    private PrintController printController;
    private String activeSection = "dashboard";

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            languageManager = LanguageManager.instance();
            sessionManager = new SessionManager();
            appearance = AppearanceSettings.load();
            DatabaseConnection.initialize();
            repository = new GymRepository();
            languageManager.setLanguage(repository.language());
            printController = new PrintController(repository, stage, this::t, this::money,
                    this::isArabic, this::showError);
            stage.setTitle(t("app.title"));
            stage.setMinWidth(900);
            stage.setMinHeight(620);
            showLogin();
            stage.show();
        } catch (SQLException | java.io.IOException | GeneralSecurityException exception) {
            showError(t("error.startup"), exception);
        }
    }

    private ComboBox<String> genderChoices() {
        return combo(t("gender.unspecified"), t("gender.female"), t("gender.male"));
    }

    private String genderCode(String localizedGender) {
        if (localizedGender.equals(t("gender.female")))
            return "FEMALE";
        if (localizedGender.equals(t("gender.male")))
            return "MALE";
        return "UNSPECIFIED";
    }

    private String genderLabel(String storedGender) {
        return switch (storedGender) {
            case "FEMALE" -> t("gender.female");
            case "MALE" -> t("gender.male");
            default -> t("gender.unspecified");
        };
    }

    private void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(GymApplication.class.getResource("views/fxml/login.fxml"));
        Parent root = loader.load();
        loginController = loader.getController();
        loginController.configure(t("login.title"), t("login.subtitle"), t("login.username"),
                t("login.password"), t("login.button"), languageButtonText(),
                isArabic() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT,
                this::authenticate, this::toggleLanguage);
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(GymApplication.class.getResource("views/css/style.css").toExternalForm());
        applyAppearance(scene.getRoot());
        stage.setScene(scene);
    }

    private void authenticate(String username, char[] password) {
        try {
            Optional<User> user = repository.authenticate(username, password);
            if (user.isEmpty()) {
                loginController.showError(t("login.invalid"));
                return;
            }
            currentUser = user.get();
            sessionManager.signIn(currentUser);
            showDashboard();
        } catch (SQLException | GeneralSecurityException | IOException exception) {
            showError(t("error.login"), exception);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void logout() {
        currentUser = null;
        sessionManager.signOut();
        try {
            showLogin();
        } catch (IOException exception) {
            showError(t("error.load"), exception);
        }
    }

    private void showDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(GymApplication.class.getResource("views/fxml/dashboard.fxml"));
        Parent root = loader.load();
        dashboardController = loader.getController();
        dashboardController.configure(t("app.club"), t("nav.workspace"), initials(currentUser.fullName()),
                currentUser.fullName(),
                "ADMIN".equals(currentUser.role()) ? t("role.admin") : t("role.receptionist"),
                languageButtonText(), themeButtonText(), t("appearance.accent"), t("nav.logout"),
                appearance.isDarkMode(), appearance.accentColor(),
                isArabic() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT,
                navigationItems(), this::showSection, this::toggleLanguage, this::setDarkMode,
                this::setAccentColor, this::logout);
        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(GymApplication.class.getResource("views/css/style.css").toExternalForm());
        applyAppearance(scene.getRoot());
        stage.setScene(scene);
        stage.setTitle(t("app.title"));
        showSection(activeSection);
    }

    private List<DashboardController.NavigationItem> navigationItems() {
        List<DashboardController.NavigationItem> items = new ArrayList<>();
        for (String section : SECTIONS) {
            if (("team".equals(section) || "finance".equals(section) || "payroll".equals(section))
                    && !"ADMIN".equals(currentUser.role()))
                continue;
            items.add(new DashboardController.NavigationItem(
                    section, t("nav." + section), sectionIcon(section)));
        }
        return items;
    }

    private void showSection(String section) {
        activeSection = section;
        Node page;
        try {
            page = switch (section) {
                case "members" -> membersPage();
                case "subscriptions" -> subscriptionsPage();
                case "attendance" -> attendancePage();
                case "schedule" -> schedulePage();
                case "coaches" -> coachesPage();
                case "activities" -> activitiesPage();
                case "finance" -> financePage();
                case "payroll" -> payrollPage();
                case "team" -> teamPage();
                default -> dashboardPage();
            };
        } catch (SQLException exception) {
            showError(t("error.load"), exception);
            page = new Label(t("error.load"));
        }
        dashboardController.showPage(t("nav." + section), t("subtitle." + section), page,
                section, navigationItems());
    }

    private Node dashboardPage() throws SQLException {
        GymRepository.Dashboard stats = repository.dashboard();
        VBox content = contentColumn();
        FlowPane metrics = new FlowPane(12, 12);
        metrics.setPrefWrapLength(1000);
        metrics.getChildren().addAll(
                metricCard(t("metric.members"), Integer.toString(stats.members()), t("metric.all")),
                metricCard(t("metric.active"), Integer.toString(stats.activeMembers()), t("metric.membership")),
                metricCard(t("metric.checkins"), Integer.toString(stats.checkInsToday()), t("metric.today")),
                metricCard(t("metric.revenue"), money(stats.revenue()), t("metric.month")));
        Label balance = new Label(t("metric.expenses") + ": " + money(stats.expenses())
                + "    " + t("metric.net") + ": " + money(stats.revenue() - stats.expenses()));
        balance.getStyleClass().add("section-title");
        Button add = primaryButton(t("members.add"));
        add.setOnAction(event -> addMember());
        VBox welcome = new VBox(8, new Label(t("app.title")),
                new Label(t("dashboard.welcome", currentUser.fullName())),
                new Label(t("dashboard.help")), add);
        welcome.getStyleClass().add("welcome-card");
        welcome.getStyleClass().add("welcome-copy");
        Button report = new Button(t("print.report"));
        report.setOnAction(event -> printController.printDashboardReport());
        report.getStyleClass().add("welcome-report-button");
        welcome.getChildren().add(report);
        content.getChildren().addAll(welcome, metrics, balance);
        return content;
    }

    private Node membersPage() throws SQLException {
        VBox content = contentColumn();
        TextField search = new TextField();
        search.setPromptText(t("members.search"));
        HBox toolbar = new HBox(10, search, primaryButton(t("members.add")));
        HBox.setHgrow(search, Priority.ALWAYS);
        Button add = (Button) toolbar.getChildren().get(1);
        add.setOnAction(event -> addMember());
        TableView<GymRepository.MemberRow> table = table();
        FilteredList<GymRepository.MemberRow> filtered = new FilteredList<>(
                FXCollections.observableArrayList(repository.members()), member -> true);
        search.textProperty().addListener((observable, old, value) -> {
            String query = value.trim().toLowerCase(languageManager.locale());
            filtered.setPredicate(member -> query.isEmpty()
                    || member.fullName().toLowerCase(languageManager.locale()).contains(query)
                    || member.phone().toLowerCase(languageManager.locale()).contains(query)
                    || member.email().toLowerCase(languageManager.locale()).contains(query)
                    || member.plan().toLowerCase(languageManager.locale()).contains(query));
        });
        table.setItems(filtered);
        column(table, t("members.name"), row -> row.fullName(), 1.4);
        column(table, t("members.phone"), row -> row.phone(), 1);
        column(table, t("members.email"), row -> row.email(), 1.3);
        column(table, t("member.qrCode"), row -> QrCodeService.memberCode(row.id()), .9);
        column(table, t("members.plan"), row -> row.plan(), 1);
        column(table, t("members.status"), row -> t("status." + row.status()), .8);
        Button edit = new Button(t("button.edit"));
        edit.setOnAction(event -> {
            GymRepository.MemberRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("members.select"));
            } else {
                editMember(selected);
            }
        });
        Button printCard = new Button(t("print.memberCard"));
        printCard.setOnAction(event -> {
            GymRepository.MemberRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("members.select"));
            } else {
                printController.printMemberCard(selected);
            }
        });
        content.getChildren().addAll(toolbar, new HBox(10, edit, printCard), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node subscriptionsPage() throws SQLException {
        VBox content = contentColumn();
        TableView<GymRepository.SubscriptionRow> table = table();
        table.getItems().setAll(repository.subscriptions());
        column(table, t("members.name"), row -> row.member(), 1.2);
        column(table, t("subscription.plan"), row -> row.plan(), 1.1);
        column(table, t("subscription.type"), row -> t("subscription." + row.type()), 1);
        column(table, t("subscription.start"), GymRepository.SubscriptionRow::startDate, 1);
        column(table, t("subscription.end"), GymRepository.SubscriptionRow::endDate, 1);
        column(table, t("subscription.sessions"), GymRepository.SubscriptionRow::remaining, .8);
        column(table, t("finance.amount"), row -> money(row.price()), .9);
        column(table, t("members.status"), row -> t("status." + row.status()), .8);
        column(table, t("finance.status"), row -> t("status." + row.paymentStatus()), .9);
        Button add = primaryButton(t("subscription.add"));
        add.setOnAction(event -> addSubscription());
        Button toggleStatus = new Button(t("subscription.toggle"));
        toggleStatus.setOnAction(event -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("subscription.select"));
            } else if ("ACTIVE".equals(selected.status()) || "FROZEN".equals(selected.status())) {
                String nextStatus = "ACTIVE".equals(selected.status()) ? "FROZEN" : "ACTIVE";
                execute(() -> repository.setSubscriptionStatus(selected.id(), nextStatus));
            } else {
                showInfo(t("subscription.cannotToggle"));
            }
        });
        Button confirmPayment = new Button(t("finance.confirmReceived"));
        confirmPayment.setOnAction(event -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("subscription.select"));
            } else if (!"PENDING".equals(selected.paymentStatus())) {
                showInfo(t("finance.paymentNotPending"));
            } else if (confirm(t("finance.confirmReceivedPrompt"))) {
                execute(() -> repository.confirmOfflineSubscriptionPayment(selected.id()));
            }
        });
        Button printInvoice = new Button(t("print.invoice"));
        printInvoice.setOnAction(event -> {
            GymRepository.SubscriptionRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("subscription.select"));
            } else {
                printController.printSubscriptionInvoice(selected.id());
            }
        });
        content.getChildren().addAll(new HBox(10, add, toggleStatus, confirmPayment, printInvoice), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node attendancePage() throws SQLException {
        VBox content = contentColumn();
        TableView<GymRepository.Attendance> table = table();
        table.getItems().setAll(repository.attendance());
        column(table, t("members.name"), GymRepository.Attendance::member, 1.2);
        column(table, t("members.phone"), GymRepository.Attendance::phone, 1);
        column(table, t("subscription.plan"), GymRepository.Attendance::plan, 1);
        column(table, t("attendance.time"), GymRepository.Attendance::checkedInAt, 1.3);
        TextField qrInput = text();
        qrInput.setPromptText(t("attendance.scanPrompt"));
        qrInput.setAccessibleText(t("attendance.scanPrompt"));
        Label scanResult = new Label(t("attendance.scanReady"));
        scanResult.getStyleClass().add("scan-result");
        Button scanButton = primaryButton(t("attendance.scan"));
        scanButton.setOnAction(event -> processQrScan(qrInput, scanResult, table));
        qrInput.setOnAction(event -> processQrScan(qrInput, scanResult, table));
        HBox scanner = new HBox(10, qrInput, scanButton);
        HBox.setHgrow(qrInput, Priority.ALWAYS);
        VBox scanPanel = new VBox(8, new Label(t("attendance.scanTitle")), scanner, scanResult);
        scanPanel.getStyleClass().add("scan-panel");
        Button checkIn = primaryButton(t("attendance.checkin"));
        checkIn.setOnAction(event -> checkIn());
        content.getChildren().addAll(scanPanel, checkIn, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        javafx.application.Platform.runLater(qrInput::requestFocus);
        return content;
    }

    private Node schedulePage() throws SQLException {
        VBox content = contentColumn();
        Map<String, String> activityNames = new HashMap<>();
        repository.activities().forEach(activity -> activityNames.put(activity.nameEnglish(), activity.nameArabic()));
        TableView<GymRepository.ScheduledClass> table = table();
        table.getItems().setAll(repository.scheduledClasses());
        column(table, t("schedule.class"), GymRepository.ScheduledClass::title, 1.2);
        column(table, t("activity.title"), row -> isArabic()
                ? activityNames.getOrDefault(row.activity(), row.activity())
                : row.activity(), 1);
        column(table, t("coach.name"), GymRepository.ScheduledClass::coach, 1);
        column(table, t("schedule.day"), row -> t("day." + row.dayOfWeek()), .8);
        column(table, t("schedule.time"), row -> row.startTime() + "–" + row.endTime(), 1);
        column(table, t("schedule.capacity"), row -> Integer.toString(row.capacity()), .7);
        Button add = primaryButton(t("schedule.add"));
        add.setOnAction(event -> addScheduledClass());
        Button edit = new Button(t("schedule.edit"));
        edit.setOnAction(event -> {
            GymRepository.ScheduledClass selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showInfo(t("schedule.select"));
            else
                editScheduledClass(selected);
        });
        Button delete = new Button(t("button.delete"));
        delete.setOnAction(event -> {
            GymRepository.ScheduledClass selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showInfo(t("schedule.select"));
            else if (confirm(t("schedule.confirmDelete"))) {
                execute(() -> repository.deleteScheduledClass(selected.id()));
            }
        });
        content.getChildren().addAll(new HBox(10, add, edit, delete), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node coachesPage() throws SQLException {
        VBox content = contentColumn();
        TableView<GymRepository.CoachRow> table = table();
        table.getItems().setAll(repository.coaches());
        column(table, t("coach.name"), GymRepository.CoachRow::name, 1.2);
        column(table, t("members.phone"), GymRepository.CoachRow::phone, 1);
        column(table, t("activity.title"), GymRepository.CoachRow::activity, 1);
        column(table, t("team.salary"), row -> money(row.salary()), .9);
        column(table, t("coach.personal"), row -> row.personalTrainer() ? t("common.yes") : t("common.no"), 1);
        Button add = primaryButton(t("coach.add"));
        add.setOnAction(event -> addCoach());
        Button edit = new Button(t("button.editCoach"));
        edit.setOnAction(event -> {
            GymRepository.CoachRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showInfo(t("coach.select"));
            else
                editCoach(selected);
        });
        Button delete = new Button(t("button.delete"));
        delete.setOnAction(event -> {
            GymRepository.CoachRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showInfo(t("coach.select"));
            else if (confirm(t("coach.confirmDelete")))
                execute(() -> repository.deleteCoach(selected.id()));
        });
        content.getChildren().addAll(new HBox(10, add, edit, delete), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node activitiesPage() throws SQLException {
        VBox content = contentColumn();
        TableView<GymRepository.ActivityRow> table = table();
        table.getItems().setAll(repository.activities());
        column(table, t("activity.arabic"), GymRepository.ActivityRow::nameArabic, 1);
        column(table, t("activity.english"), GymRepository.ActivityRow::nameEnglish, 1);
        Button add = primaryButton(t("activity.add"));
        add.setOnAction(event -> addActivity());
        Button edit = new Button(t("button.editActivity"));
        edit.setOnAction(event -> {
            GymRepository.ActivityRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showInfo(t("activity.select"));
            else
                editActivity(selected);
        });
        Button delete = new Button(t("button.delete"));
        delete.setOnAction(event -> {
            GymRepository.ActivityRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null)
                showInfo(t("activity.select"));
            else if (confirm(t("activity.confirmDelete")))
                execute(() -> repository.deleteActivity(selected.id()));
        });
        content.getChildren().addAll(new HBox(10, add, edit, delete), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node financePage() throws SQLException {
        VBox content = contentColumn();
        GymRepository.Dashboard stats = repository.dashboard();
        HBox metrics = new HBox(12,
                metricCard(t("metric.revenue"), money(stats.revenue()), t("metric.month")),
                metricCard(t("metric.expenses"), money(stats.expenses()), t("metric.month")),
                metricCard(t("metric.net"), money(stats.revenue() - stats.expenses()), t("metric.month")));
        metrics.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        TableView<GymRepository.Payment> table = table();
        table.getItems().setAll(repository.payments());
        column(table, t("members.name"), GymRepository.Payment::member, 1);
        column(table, t("finance.description"), GymRepository.Payment::description, 1.2);
        column(table, t("finance.amount"), row -> money(row.amount()), .8);
        column(table, t("finance.date"), GymRepository.Payment::date, .8);
        column(table, t("finance.status"), row -> t("status." + row.status()), .8);
        Button expense = primaryButton(t("finance.addExpense"));
        expense.setOnAction(event -> addExpense());
        Button payroll = new Button(t("nav.payroll"));
        payroll.setOnAction(event -> showSection("payroll"));
        Button printReport = new Button(t("print.financialReport"));
        printReport.setOnAction(event -> printController.printFinancialReport());
        TableView<GymRepository.ActivityDemand> demand = table();
        demand.getItems().setAll(repository.activityDemand());
        column(demand, t("activity.title"), row -> isArabic()
                ? row.nameArabic()
                : row.nameEnglish(), 1.2);
        column(demand, t("report.members"), row -> Integer.toString(row.members()), .8);
        column(demand, t("report.checkins"), row -> Integer.toString(row.checkIns()), .8);
        column(demand, t("report.revenue"), row -> money(row.revenue()), .8);
        VBox reports = new VBox(10, new Label(t("report.popularActivities")), demand);
        reports.getStyleClass().add("section-card");
        content.getChildren().addAll(metrics, new HBox(10, expense, payroll, printReport), table, reports);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node payrollPage() throws SQLException {
        VBox content = contentColumn();
        TextField monthField = text(YearMonth.now().toString());
        Button refresh = new Button(t("payroll.refresh"));
        HBox monthBar = new HBox(10, new Label(t("payroll.month")), monthField, refresh);
        monthBar.setAlignment(Pos.CENTER_LEFT);
        TableView<GymRepository.Payroll> table = table();
        Runnable reload = () -> {
            try {
                YearMonth month = YearMonth.parse(monthField.getText().trim());
                table.getItems().setAll(repository.payroll(month));
            } catch (java.time.DateTimeException | SQLException exception) {
                showError(t("error.payroll"), exception);
            }
        };
        refresh.setOnAction(event -> reload.run());
        monthField.setOnAction(event -> reload.run());
        reload.run();
        column(table, t("members.name"), GymRepository.Payroll::employeeName, 1.2);
        column(table, t("team.role"), row -> "COACH".equals(row.employeeType()) ? t("coach.name")
                : "ADMIN".equals(row.role()) ? t("role.admin") : t("role.receptionist"), 1);
        column(table, t("team.salary"), row -> money(row.monthlySalary()), .9);
        column(table, t("payroll.status"), row -> row.paid() ? t("payroll.paid") : t("payroll.unpaid"), .8);
        column(table, t("payroll.paidAt"), row -> row.paidAt() == null ? "-" : row.paidAt(), 1.2);
        Button pay = primaryButton(t("payroll.pay"));
        pay.setOnAction(event -> {
            GymRepository.Payroll selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("payroll.select"));
            } else if (selected.paid()) {
                showInfo(t("payroll.alreadyPaid"));
            } else {
                try {
                    YearMonth month = YearMonth.parse(monthField.getText().trim());
                    if (confirm(t("payroll.confirm", selected.employeeName()))) {
                        repository.paySalary(selected.employeeType(), selected.id(), month);
                        reload.run();
                    }
                } catch (SQLException | java.time.DateTimeException exception) {
                    showError(t("error.payroll"), exception);
                }
            }
        });
        content.getChildren().addAll(monthBar, pay, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private Node teamPage() throws SQLException {
        VBox content = contentColumn();
        TableView<User> table = table();
        table.getItems().setAll(repository.users());
        column(table, t("login.username"), User::username, 1);
        column(table, t("members.name"), User::fullName, 1.2);
        column(table, t("team.role"), row -> "ADMIN".equals(row.role()) ? t("role.admin") : t("role.receptionist"), 1);
        column(table, t("members.phone"), row -> row.phone() == null ? "" : row.phone(), 1);
        column(table, t("team.salary"), row -> money(row.salary()), .9);
        Button add = primaryButton(t("team.add"));
        add.setOnAction(event -> addUser());
        Button changePassword = new Button(t("team.changePassword"));
        changePassword.setOnAction(event -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("team.select"));
            } else {
                changePassword(selected);
            }
        });
        Button edit = new Button(t("button.editStaff"));
        edit.setOnAction(event -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("team.select"));
            } else {
                editUser(selected);
            }
        });
        Button deactivate = new Button(t("team.deactivate"));
        deactivate.setOnAction(event -> {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo(t("team.select"));
            } else if (selected.id() == currentUser.id()) {
                showInfo(t("team.cannotDeactivateSelf"));
            } else if (confirm(t("team.confirmDeactivate"))) {
                execute(() -> repository.deactivateUser(selected.id()));
            }
        });
        content.getChildren().addAll(new HBox(10, add, edit, changePassword, deactivate), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return content;
    }

    private void addMember() {
        List<String> values = form(t("members.add"), field(t("members.name"), text()),
                field(t("members.phone"), text()), field(t("login.email"), text()),
                field(t("members.gender"), combo(t("gender.unspecified"), t("gender.female"), t("gender.male"))),
                field(t("members.notes"), text()));
        if (values.isEmpty())
            return;
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            showInfo(t("validation.required"));
            return;
        }
        Member member = new Member(0, values.get(0), values.get(1), values.get(2),
                genderCode(values.get(3)), values.get(4));
        execute(() -> repository.addMember(member));
    }

    private void editMember(GymRepository.MemberRow member) {
        ComboBox<String> gender = genderChoices();
        if (!member.gender().isBlank())
            gender.getSelectionModel().select(genderLabel(member.gender()));
        List<String> values = form(t("members.edit"),
                field(t("members.name"), text(member.fullName())),
                field(t("members.phone"), text(member.phone())),
                field(t("login.email"), text(member.email())),
                field(t("members.gender"), gender),
                field(t("members.notes"), text(member.medicalNotes())));
        if (values.isEmpty())
            return;
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            showInfo(t("validation.required"));
            return;
        }
        Member updated = new Member(member.id(), values.get(0), values.get(1), values.get(2),
                genderCode(values.get(3)), values.get(4));
        execute(() -> repository.updateMember(member.id(), updated));
    }

    private void addSubscription() {
        try {
            List<GymRepository.MemberRow> members = repository.members();
            if (members.isEmpty()) {
                showInfo(t("members.none"));
                return;
            }
            ComboBox<String> memberBox = new ComboBox<>();
            members.forEach(member -> memberBox.getItems().add(member.id() + " | " + member.fullName()
                    + " (" + member.phone() + ")"));
            memberBox.getSelectionModel().selectFirst();
            ComboBox<String> activityBox = activityChoices();
            ComboBox<String> typeBox = combo(t("subscription.TIME_BASED"), t("subscription.SESSION_BASED"));
            DatePicker start = new DatePicker(LocalDate.now());
            List<String> values = form(t("subscription.add"),
                    field(t("members.name"), memberBox),
                    field(t("subscription.plan"), text()),
                    field(t("subscription.type"), typeBox),
                    field(t("subscription.duration"), text("1")),
                    field(t("finance.amount"), text()),
                    field(t("subscription.discount"), text("0")),
                    field(t("subscription.start"), start),
                    field(t("activity.title"), activityBox));
            if (values.isEmpty())
                return;
            if (memberBox.getValue() == null || values.get(1).isBlank()) {
                showInfo(t("validation.required"));
                return;
            }
            int memberId = Integer.parseInt(memberBox.getValue().substring(0, memberBox.getValue().indexOf(" | ")));
            boolean sessionBased = t("subscription.SESSION_BASED").equals(typeBox.getValue());
            int duration = Integer.parseInt(values.get(3));
            double price = Double.parseDouble(values.get(4));
            double discount = Double.parseDouble(values.get(5));
            if (duration <= 0 || price < 0 || discount < 0 || discount > 100) {
                showInfo(t("validation.subscription"));
                return;
            }
            Integer activityId = parseActivityId(values.get(7));
            Subscription subscription = new Subscription(0, memberId, activityId, values.get(1),
                    sessionBased ? Subscription.PlanType.SESSION_BASED : Subscription.PlanType.TIME_BASED,
                    sessionBased ? null : duration, sessionBased ? duration : null, sessionBased ? duration : null,
                    LocalDate.parse(values.get(6)),
                    sessionBased ? null : LocalDate.parse(values.get(6)).plusMonths(duration),
                    price, discount, Subscription.Status.ACTIVE);
            repository.addSubscription(subscription);
            showSection(activeSection);
        } catch (NumberFormatException | java.time.DateTimeException | SQLException exception) {
            showError(t("error.subscription"), exception);
        }
    }

    private void checkIn() {
        try {
            List<GymRepository.MemberRow> members = repository.members();
            if (members.isEmpty()) {
                showInfo(t("members.none"));
                return;
            }
            ComboBox<String> memberBox = new ComboBox<>();
            members.forEach(member -> memberBox.getItems().add(member.id() + " | " + member.fullName()
                    + " (" + member.phone() + ")"));
            memberBox.getSelectionModel().selectFirst();
            List<String> values = form(t("attendance.checkin"), field(t("members.name"), memberBox));
            if (values.isEmpty() || memberBox.getValue() == null)
                return;
            int memberId = Integer.parseInt(memberBox.getValue().substring(0, memberBox.getValue().indexOf(" | ")));
            GymRepository.CheckInResult result = repository.checkIn(memberId);
            showInfo(checkInMessage(result));
            showSection("attendance");
        } catch (SQLException | NumberFormatException exception) {
            showError(t("error.checkin"), exception);
        } catch (IllegalStateException exception) {
            showInfo(t("attendance.invalid"));
        }
    }

    private void processQrScan(TextField input, Label feedback,
            TableView<GymRepository.Attendance> attendanceTable) {
        String code = input.getText().trim();
        input.clear();
        if (code.isEmpty()) {
            input.requestFocus();
            return;
        }
        try {
            GymRepository.CheckInResult result = repository.checkInByQrCode(code);
            feedback.setText(checkInMessage(result));
            feedback.getStyleClass().remove("scan-error");
            feedback.getStyleClass().add("scan-success");
            try {
                attendanceTable.getItems().setAll(repository.attendance());
            } catch (SQLException exception) {
                showError(t("error.load"), exception);
            }
        } catch (IllegalArgumentException exception) {
            showInfo(t("attendance.qrInvalid"));
            feedback.setText(t("attendance.qrInvalid"));
            setScanFeedbackError(feedback);
        } catch (IllegalStateException exception) {
            showInfo(t("attendance.invalid"));
            feedback.setText(t("attendance.invalid"));
            setScanFeedbackError(feedback);
        } catch (SQLException exception) {
            showError(t("error.checkin"), exception);
            feedback.setText(t("error.checkin"));
            setScanFeedbackError(feedback);
        } finally {
            input.requestFocus();
        }
    }

    private String checkInMessage(GymRepository.CheckInResult result) {
        String message = result.memberName() + "  •  " + t("subscription.plan") + ": " + result.planName()
                + " (" + t("subscription." + result.planType()) + ")";
        if (result.sessionBased()) {
            return message + "  •  " + t("attendance.sessionsLeft",
                    result.remainingSessions() + " / " + result.totalSessions());
        }
        return message + "  •  " + t("subscription.end") + ": " + result.endDate();
    }

    private void setScanFeedbackError(Label feedback) {
        feedback.getStyleClass().remove("scan-success");
        if (!feedback.getStyleClass().contains("scan-error")) {
            feedback.getStyleClass().add("scan-error");
        }
    }

    private void addCoach() {
        try {
            ComboBox<String> activity = activityChoices();
            CheckBox personal = new CheckBox(t("coach.personal"));
            List<String> values = form(t("coach.add"),
                    field(t("coach.name"), text()),
                    field(t("members.phone"), text()),
                    field(t("team.salary"), text("0")),
                    field(t("activity.title"), activity),
                    field(t("coach.personal"), personal));
            if (values.isEmpty())
                return;
            if (values.get(0).isBlank()) {
                showInfo(t("validation.required"));
                return;
            }
            Integer activityId = parseActivityId(values.get(3));
            double salary = Double.parseDouble(values.get(2));
            if (salary < 0) {
                showInfo(t("validation.salary"));
                return;
            }
            repository.addCoach(new Coach(0, values.get(0), values.get(1), activityId, salary,
                    Boolean.parseBoolean(values.get(4))));
            showSection("coaches");
        } catch (SQLException | NumberFormatException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void addScheduledClass() {
        try {
            ComboBox<String> activity = activityChoices();
            ComboBox<String> coach = coachChoices();
            ComboBox<String> day = combo(dayNames());
            List<String> values = form(t("schedule.add"),
                    field(t("schedule.class"), text()),
                    field(t("activity.title"), activity),
                    field(t("coach.name"), coach),
                    field(t("schedule.day"), day),
                    field(t("schedule.start"), text("09:00")),
                    field(t("schedule.end"), text("10:00")),
                    field(t("schedule.capacity"), text("16")));
            if (values.isEmpty())
                return;
            saveScheduledClass(null, values);
        } catch (SQLException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void editScheduledClass(GymRepository.ScheduledClass scheduledClass) {
        try {
            ComboBox<String> activity = activityChoices();
            String selectedActivity = repository.activities().stream()
                    .filter(item -> item.nameEnglish().equals(scheduledClass.activity()))
                    .map(item -> item.id() + " | " + displayActivity(item))
                    .findFirst().orElse(t("common.none"));
            activity.getSelectionModel().select(selectedActivity);
            ComboBox<String> coach = coachChoices();
            if (!scheduledClass.coach().equals("-")) {
                repository.coaches().stream()
                        .filter(item -> item.name().equals(scheduledClass.coach()))
                        .findFirst().ifPresent(item -> coach.getSelectionModel()
                                .select(item.id() + " | " + item.name()));
            }
            ComboBox<String> day = combo(dayNames());
            day.getSelectionModel().select(scheduledClass.dayOfWeek() - 1);
            List<String> values = form(t("schedule.edit"),
                    field(t("schedule.class"), text(scheduledClass.title())),
                    field(t("activity.title"), activity),
                    field(t("coach.name"), coach),
                    field(t("schedule.day"), day),
                    field(t("schedule.start"), text(scheduledClass.startTime())),
                    field(t("schedule.end"), text(scheduledClass.endTime())),
                    field(t("schedule.capacity"), text(Integer.toString(scheduledClass.capacity()))));
            if (values.isEmpty())
                return;
            saveScheduledClass(scheduledClass.id(), values);
        } catch (SQLException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void saveScheduledClass(Integer id, List<String> values) {
        try {
            if (values.get(0).isBlank() || values.get(1).equals(t("common.none"))) {
                showInfo(t("validation.required"));
                return;
            }
            int activityId = parseActivityId(values.get(1));
            Integer coachId = parseCoachId(values.get(2));
            int dayOfWeek = java.util.Arrays.asList(dayNames()).indexOf(values.get(3)) + 1;
            int capacity = Integer.parseInt(values.get(6));
            if (id == null) {
                repository.addScheduledClass(values.get(0), activityId, coachId, dayOfWeek,
                        values.get(4), values.get(5), capacity);
            } else {
                repository.updateScheduledClass(id, values.get(0), activityId, coachId, dayOfWeek,
                        values.get(4), values.get(5), capacity);
            }
            showSection("schedule");
        } catch (SQLException | IllegalArgumentException exception) {
            showError(t("validation.schedule"), exception);
        }
    }

    private ComboBox<String> coachChoices() throws SQLException {
        ComboBox<String> coach = new ComboBox<>();
        coach.getItems().add(t("common.none"));
        repository.coaches().forEach(item -> coach.getItems().add(item.id() + " | " + item.name()));
        coach.getSelectionModel().selectFirst();
        return coach;
    }

    private Integer parseCoachId(String choice) {
        return choice.equals(t("common.none")) ? null
                : Integer.parseInt(choice.substring(0, choice.indexOf(" | ")));
    }

    private String[] dayNames() {
        return new String[] { t("day.1"), t("day.2"), t("day.3"), t("day.4"),
                t("day.5"), t("day.6"), t("day.7") };
    }

    private void editCoach(GymRepository.CoachRow coach) {
        try {
            ComboBox<String> activity = activityChoices();
            String currentActivity = repository.activities().stream()
                    .filter(item -> item.nameEnglish().equals(coach.activity()))
                    .map(item -> item.id() + " | " + displayActivity(item))
                    .findFirst().orElse(t("common.none"));
            activity.getSelectionModel().select(currentActivity);
            CheckBox personal = new CheckBox(t("coach.personal"));
            personal.setSelected(coach.personalTrainer());
            List<String> values = form(t("coach.edit"),
                    field(t("coach.name"), text(coach.name())),
                    field(t("members.phone"), text(coach.phone())),
                    field(t("team.salary"), text(Double.toString(coach.salary()))),
                    field(t("activity.title"), activity),
                    field(t("coach.personal"), personal));
            if (values.isEmpty())
                return;
            if (values.get(0).isBlank()) {
                showInfo(t("validation.required"));
                return;
            }
            double salary = Double.parseDouble(values.get(2));
            if (salary < 0) {
                showInfo(t("validation.salary"));
                return;
            }
            repository.updateCoach(coach.id(), new Coach(coach.id(), values.get(0), values.get(1),
                    parseActivityId(values.get(3)), salary, Boolean.parseBoolean(values.get(4))));
            showSection("coaches");
        } catch (SQLException | NumberFormatException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void addActivity() {
        List<String> values = form(t("activity.add"),
                field(t("activity.arabic"), text()), field(t("activity.english"), text()));
        if (values.isEmpty())
            return;
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            showInfo(t("validation.required"));
            return;
        }
        execute(() -> repository.addActivity(new Activity(0, values.get(0), values.get(1), null)));
    }

    private void editActivity(GymRepository.ActivityRow activity) {
        List<String> values = form(t("activity.edit"),
                field(t("activity.arabic"), text(activity.nameArabic())),
                field(t("activity.english"), text(activity.nameEnglish())));
        if (values.isEmpty())
            return;
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            showInfo(t("validation.required"));
            return;
        }
        execute(() -> repository.updateActivity(activity.id(),
                new Activity(activity.id(), values.get(0), values.get(1), null)));
    }

    private void addUser() {
        List<String> values = form(t("team.add"),
                field(t("login.username"), text()),
                field(t("team.password"), new PasswordField()),
                field(t("members.name"), text()),
                field(t("team.role"), combo(t("role.receptionist"), t("role.admin"))),
                field(t("members.phone"), text()),
                field(t("team.salary"), text("0")));
        if (values.isEmpty())
            return;
        if (values.get(0).isBlank() || values.get(1).length() < 8 || values.get(2).isBlank()) {
            showInfo(t("validation.user"));
            return;
        }
        String role = values.get(3).equals(t("role.admin")) ? "ADMIN" : "RECEPTIONIST";
        try {
            double salary = Double.parseDouble(values.get(5));
            if (salary < 0) {
                showInfo(t("validation.salary"));
                return;
            }
            repository.addUser(values.get(0), values.get(1).toCharArray(), values.get(2), role, values.get(4), salary);
            showSection("team");
        } catch (SQLException | GeneralSecurityException | NumberFormatException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void editUser(User user) {
        ComboBox<String> role = combo(t("role.receptionist"), t("role.admin"));
        role.getSelectionModel().select("ADMIN".equals(user.role()) ? t("role.admin") : t("role.receptionist"));
        List<String> values = form(t("team.edit"),
                field(t("members.name"), text(user.fullName())),
                field(t("team.role"), role),
                field(t("members.phone"), text(user.phone() == null ? "" : user.phone())),
                field(t("team.salary"), text(Double.toString(user.salary()))));
        if (values.isEmpty())
            return;
        if (values.get(0).isBlank()) {
            showInfo(t("validation.required"));
            return;
        }
        try {
            double salary = Double.parseDouble(values.get(3));
            if (salary < 0) {
                showInfo(t("validation.salary"));
                return;
            }
            String roleValue = values.get(1).equals(t("role.admin")) ? "ADMIN" : "RECEPTIONIST";
            repository.updateUser(user.id(), values.get(0), roleValue, values.get(2), salary);
            showSection("team");
        } catch (SQLException | NumberFormatException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void changePassword(User user) {
        PasswordField password = new PasswordField();
        List<String> values = form(t("team.changePassword"), field(t("team.password"), password));
        if (values.isEmpty())
            return;
        if (values.get(0).length() < 8) {
            showInfo(t("validation.user"));
            return;
        }
        try {
            repository.updatePassword(user.id(), values.get(0).toCharArray());
            showInfo(t("team.passwordChanged"));
        } catch (SQLException | GeneralSecurityException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void addExpense() {
        List<String> values = form(t("finance.addExpense"),
                field(t("finance.description"), text()),
                field(t("finance.amount"), text()),
                field(t("finance.date"), new DatePicker(LocalDate.now())));
        if (values.isEmpty())
            return;
        try {
            double amount = Double.parseDouble(values.get(1));
            if (values.get(0).isBlank() || amount < 0) {
                showInfo(t("validation.required"));
                return;
            }
            repository.addExpense(values.get(0), amount, LocalDate.parse(values.get(2)));
            showSection("finance");
        } catch (SQLException | NumberFormatException exception) {
            showError(t("error.save"), exception);
        }
    }

    private List<String> form(String title, Field... fields) {
        return FormDialog.show(title, appearance.isDarkMode(), isArabic(), fields);
    }

    private Field field(String label, Control input) {
        return FormDialog.field(label, input);
    }

    private TextField text() {
        return new TextField();
    }

    private TextField text(String value) {
        return new TextField(value);
    }

    private ComboBox<String> combo(String... values) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.getSelectionModel().selectFirst();
        return combo;
    }

    private ComboBox<String> activityChoices() throws SQLException {
        ComboBox<String> activity = new ComboBox<>();
        activity.getItems().add(t("common.none"));
        repository.activities().forEach(item -> activity.getItems().add(item.id() + " | " + displayActivity(item)));
        activity.getSelectionModel().selectFirst();
        return activity;
    }

    private Integer parseActivityId(String choice) {
        return choice.equals(t("common.none")) ? null
                : Integer.parseInt(choice.substring(0, choice.indexOf(" | ")));
    }

    private <T> TableView<T> table() {
        TableView<T> table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label(t("table.empty")));
        return table;
    }

    private <T> void column(TableView<T> table, String title, Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        column.setMinWidth(85 * width);
        table.getColumns().add(column);
    }

    private VBox contentColumn() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 28, 28, 28));
        content.getStyleClass().add("page-content");
        return content;
    }

    private VBox metricCard(String heading, String value, String caption) {
        Label title = new Label(heading);
        title.getStyleClass().add("section-action");
        Label amount = new Label(value);
        amount.getStyleClass().add("metric-value");
        Label detail = new Label(caption);
        detail.getStyleClass().add("muted-label");
        VBox card = new VBox(9, title, amount, detail);
        card.getStyleClass().add("metric-card");
        card.setMinWidth(150);
        card.setPrefWidth(220);
        return card;
    }

    private Button primaryButton(String title) {
        Button button = new Button(title);
        button.getStyleClass().add("primary-button");
        return button;
    }

    private void execute(SqlAction action) {
        try {
            action.run();
            showSection(activeSection);
        } catch (SQLException exception) {
            showError(t("error.save"), exception);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private void toggleLanguage() {
        try {
            String language = isArabic() ? "en" : "ar";
            languageManager.setLanguage(language);
            repository.setLanguage(language);
            if (currentUser == null) {
                showLogin();
            } else {
                showDashboard();
            }
        } catch (SQLException | IOException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void setDarkMode(boolean darkMode) {
        try {
            appearance = AppearanceSettings.create(darkMode, appearance.accentColor());
            appearance.save();
            showDashboard();
        } catch (IOException | java.util.prefs.BackingStoreException
                | IllegalArgumentException | SecurityException exception) {
            showError(t("error.save"), exception);
        }
    }

    private void setAccentColor(Color color) {
        try {
            appearance = AppearanceSettings.create(appearance.isDarkMode(), colorHex(color));
            appearance.save();
            if (stage.getScene() != null) {
                applyAppearance(stage.getScene().getRoot());
            }
        } catch (java.util.prefs.BackingStoreException
                | IllegalArgumentException | SecurityException exception) {
            showError(t("error.save"), exception);
        }
    }

    private String themeButtonText() {
        return appearance.isDarkMode() ? t("appearance.lightMode") : t("appearance.darkMode");
    }

    private void applyAppearance(Node root) {
        root.getStyleClass().remove("dark-theme");
        if (appearance.isDarkMode()) {
            root.getStyleClass().add("dark-theme");
        }
        Color accent = Color.web(appearance.accentColor());
        root.setStyle("-gym-accent: " + appearance.accentColor()
                + "; -gym-accent-hover: " + colorHex(accent.deriveColor(0, 0.9, 0.82, 1))
                + "; -gym-accent-soft: " + colorHex(mixWithWhite(accent, 0.9)) + ";");
    }

    private Color mixWithWhite(Color color, double whiteAmount) {
        return new Color(color.getRed() * (1 - whiteAmount) + whiteAmount,
                color.getGreen() * (1 - whiteAmount) + whiteAmount,
                color.getBlue() * (1 - whiteAmount) + whiteAmount, 1);
    }

    private String colorHex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }

    private void applyDirection(Node node) {
        node.setNodeOrientation(isArabic()
                ? NodeOrientation.RIGHT_TO_LEFT
                : NodeOrientation.LEFT_TO_RIGHT);
    }

    private String languageButtonText() {
        return isArabic() ? "English" : "العربية";
    }

    private boolean isArabic() {
        return languageManager.locale().getLanguage().equals("ar");
    }

    private String displayActivity(GymRepository.ActivityRow activity) {
        return isArabic() ? activity.nameArabic() : activity.nameEnglish();
    }

    private String money(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(languageManager.locale());
        format.setCurrency(java.util.Currency.getInstance("EGP"));
        return format.format(amount);
    }

    private String sectionIcon(String section) {
        return switch (section) {
            case "dashboard" -> "⌂";
            case "members" -> "♙";
            case "subscriptions" -> "▣";
            case "attendance" -> "✓";
            case "coaches" -> "♟";
            case "activities" -> "▦";
            case "finance" -> "$";
            default -> "⚙";
        };
    }

    private String initials(String name) {
        String[] words = name.trim().split("\\s+");
        return words.length > 1
                ? (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase(languageManager.locale())
                : name.substring(0, 1).toUpperCase(languageManager.locale());
    }

    private String t(String key) {
        return languageManager.text(key);
    }

    private String t(String key, String value) {
        return t(key).replace("{0}", value);
    }

    private void showInfo(String message) {
        AlertUtils.show(new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK), t("app.title"), null,
                isArabic(), appearance.isDarkMode());
    }

    private boolean confirm(String message) {
        return AlertUtils.confirm(t("app.title"), message, isArabic(), appearance.isDarkMode());
    }

    private void showError(String message, Exception exception) {
        AlertUtils.showError(t("error.title"), message, exception, isArabic(), appearance.isDarkMode());
    }

}
