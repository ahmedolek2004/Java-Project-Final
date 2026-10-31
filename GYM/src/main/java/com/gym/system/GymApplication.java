package com.gym.system;

import com.gym.system.controller.DashboardController;
import com.gym.system.controller.LoginController;
import com.gym.system.controller.PrintController;
import com.gym.system.controller.helper.AppearanceHelper;
import com.gym.system.controller.helper.NavigationHelper;
import com.gym.system.controller.helper.PageContext;
import com.gym.system.controller.page.*;
import com.gym.system.dao.DatabaseConnection;
import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.User;
import com.gym.system.service.*;
import com.gym.system.util.AlertUtils;
import com.gym.system.util.AppearanceSettings;
import com.gym.system.util.LanguageManager;
import com.gym.system.util.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

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

    // Domain Services
    private MemberService memberService;
    private SubscriptionService subscriptionService;
    private AttendanceService attendanceService;
    private ScheduleService scheduleService;
    private CoachService coachService;
    private ActivityService activityService;
    private FinanceService financeService;
    private PayrollService payrollService;
    private TeamService teamService;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            languageManager = LanguageManager.instance();
            sessionManager = new SessionManager();
            appearance = AppearanceSettings.load();
            DatabaseConnection.initialize();
            repository = new GymRepository();

            // Initialize Services (every service enforces the signed-in user's permissions)
            AccessControl accessControl = new AccessControl(sessionManager::currentUser);
            memberService = new MemberService(repository, accessControl);
            subscriptionService = new SubscriptionService(repository, accessControl);
            attendanceService = new AttendanceService(repository, accessControl);
            scheduleService = new ScheduleService(repository, accessControl);
            coachService = new CoachService(repository, accessControl);
            activityService = new ActivityService(repository, accessControl);
            financeService = new FinanceService(repository, accessControl);
            payrollService = new PayrollService(repository, accessControl);
            teamService = new TeamService(repository, accessControl);

            languageManager.setLanguage(repository.language());
            printController = new PrintController(repository, stage, languageManager::text, this::money,
                    this::isArabic, (msg, ex) -> showError(msg, ex));

            stage.setTitle(t("app.title"));
            stage.setMinWidth(900);
            stage.setMinHeight(620);
            showLogin();
            stage.show();
        } catch (SQLException | IOException | GeneralSecurityException exception) {
            showError(t("error.startup"), exception);
        }
    }

    public void showLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(GymApplication.class.getResource("views/fxml/login.fxml"));
        Parent root = loader.load();
        loginController = loader.getController();
        loginController.configure(
                t("login.title"), t("login.subtitle"), t("login.username"),
                t("login.password"), t("login.button"), AppearanceHelper.languageButtonText(isArabic()),
                isArabic() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT,
                this::authenticate, this::toggleLanguage
        );
        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(GymApplication.class.getResource("views/css/style.css").toExternalForm());
        AppearanceHelper.applyAppearance(scene.getRoot(), appearance);
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
        dashboardController.configure(
                t("app.club"), t("nav.workspace"), initials(currentUser.fullName()),
                currentUser.fullName(),
                "ADMIN".equals(currentUser.role()) ? t("role.admin") : t("role.receptionist"),
                AppearanceHelper.languageButtonText(isArabic()),
                AppearanceHelper.themeButtonText(appearance, this::t),
                t("appearance.accent"), t("nav.logout"),
                appearance.isDarkMode(), appearance.accentColor(),
                isArabic() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT,
                navigationItems(), this::showSection, this::toggleLanguage, this::setDarkMode,
                this::setAccentColor, this::logout
        );
        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(GymApplication.class.getResource("views/css/style.css").toExternalForm());
        AppearanceHelper.applyAppearance(scene.getRoot(), appearance);
        stage.setScene(scene);
        stage.setTitle(t("app.title"));
        showSection(activeSection);
    }

    private List<DashboardController.NavigationItem> navigationItems() {
        return NavigationHelper.createNavigationItems(
                currentUser != null ? currentUser.role() : "", this::t, SECTIONS);
    }

    private PageContext createPageContext() {
        return new PageContext(
                repository,
                currentUser,
                printController,
                this::t,
                this::money,
                isArabic(),
                appearance.isDarkMode(),
                this::showInfo,
                msg -> showError(msg, new RuntimeException(msg)),
                this::confirm,
                this::showSection
        );
    }

    private void showSection(String section) {
        // Enforce RBAC security
        if (("team".equals(section) || "finance".equals(section) || "payroll".equals(section))
                && (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.role()))) {
            section = "dashboard";
        }
        activeSection = section;

        PageContext ctx = createPageContext();
        Node page;
        try {
            page = switch (section) {
                case "members" -> new MembersPage(memberService, subscriptionService, ctx).render();
                case "subscriptions" -> new SubscriptionsPage(subscriptionService, ctx).render();
                case "attendance" -> new AttendancePage(attendanceService, ctx).render();
                case "schedule" -> new SchedulePage(scheduleService, ctx).render();
                case "coaches" -> new CoachesPage(coachService, ctx).render();
                case "activities" -> new ActivitiesPage(activityService, ctx).render();
                case "finance" -> new FinancePage(financeService, ctx).render();
                case "payroll" -> new PayrollPage(payrollService, ctx).render();
                case "team" -> new TeamPage(teamService, ctx).render();
                default -> new DashboardPage(ctx).render();
            };
        } catch (SQLException exception) {
            showError(t("error.load"), exception);
            page = new Label(t("error.load"));
        }
        dashboardController.showPage(t("nav." + section), t("subtitle." + section), page,
                section, navigationItems());
    }

    private void toggleLanguage() {
        try {
            String lang = isArabic() ? "en" : "ar";
            languageManager.setLanguage(lang);
            repository.setLanguage(lang);
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
        } catch (Exception exception) {
            showError(t("error.save"), exception);
        }
    }

    private void setAccentColor(Color color) {
        try {
            appearance = AppearanceSettings.create(appearance.isDarkMode(), AppearanceHelper.colorHex(color));
            appearance.save();
            if (stage.getScene() != null) {
                AppearanceHelper.applyAppearance(stage.getScene().getRoot(), appearance);
            }
        } catch (Exception exception) {
            showError(t("error.save"), exception);
        }
    }

    private boolean isArabic() {
        return languageManager.locale().getLanguage().equals("ar");
    }

    private String money(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(languageManager.locale());
        format.setCurrency(Currency.getInstance("EGP"));
        return format.format(amount);
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

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        AlertUtils.show(alert, t("app.title"), null, isArabic(), appearance.isDarkMode());
    }

    private boolean confirm(String message) {
        return AlertUtils.confirm(t("app.title"), message, isArabic(), appearance.isDarkMode());
    }

    private void showError(String msg, Exception e) {
        AlertUtils.showError(t("error.title"), msg, e, isArabic(), appearance.isDarkMode());
    }
}