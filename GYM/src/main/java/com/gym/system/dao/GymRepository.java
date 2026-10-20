package com.gym.system.dao;

import com.gym.system.model.Activity;
import com.gym.system.model.Coach;
import com.gym.system.model.Member;
import com.gym.system.model.Subscription;
import com.gym.system.model.User;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GymRepository implements GymDataAccess {
    private final UserRepository userRepository = new UserRepository();
    private final ClubRepository clubRepository = new ClubRepository();

    public record MemberRow(int id, String fullName, String phone, String email, String gender,
            String medicalNotes, String plan, String status) {
    }

    public record SubscriptionRow(int id, String member, String plan, String type, String startDate,
            String endDate, String remaining, double price, String status,
            String paymentStatus) {
    }

    public record CheckInResult(int memberId, String memberName, String planName, String planType,
            int remainingSessions, int totalSessions, String startDate, String endDate) {
        public boolean sessionBased() {
            return "SESSION_BASED".equals(planType);
        }
    }

    public record SubscriptionInvoice(int subscriptionId, int memberId, String member, String phone,
            String planName, String planType, int totalSessions,
            int remainingSessions, String startDate, String endDate,
            double amount, String paymentDate, String paymentStatus) {
    }

    public record Attendance(String member, String phone, String checkedInAt, String plan) {
    }

    public record Payment(String member, String description, double amount, String date, String status) {
    }

    public record ActivityRow(int id, String nameArabic, String nameEnglish) {
    }

    public record CoachRow(int id, String name, String phone, String activity, double salary, boolean personalTrainer) {
    }

    public record ScheduledClass(int id, String title, String activity, String coach, int dayOfWeek,
            String startTime, String endTime, int capacity) {
    }

    public record Payroll(int id, String employeeType, String employeeName, String role,
            double monthlySalary, boolean paid, String paidAt) {
    }

    public record ActivityDemand(String nameArabic, String nameEnglish, int members, int checkIns, double revenue) {
    }

    public record Dashboard(int members, int activeMembers, double revenue, double expenses, int checkInsToday) {
    }

    public GymRepository() throws SQLException, GeneralSecurityException {
        userRepository.seedDefaults();
        clubRepository.seedActivities();
    }

    @Override
    public Optional<User> authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        return userRepository.authenticate(username, password);
    }

    public List<MemberRow> members() throws SQLException {
        String sql = """
                SELECT m.id, m.full_name, m.phone, COALESCE(m.email, '') AS email,
                       COALESCE(m.gender, '') AS gender, COALESCE(m.medical_notes, '') AS medical_notes,
                       COALESCE((SELECT s.plan_name FROM subscriptions s
                                 WHERE s.member_id = m.id AND s.status = 'ACTIVE'
                                   AND (s.plan_type='SESSION_BASED' AND s.remaining_sessions > 0
                                        OR s.plan_type='TIME_BASED' AND s.start_date <= date('now')
                                           AND s.end_date >= date('now'))
                                 ORDER BY s.id DESC LIMIT 1), '-') AS plan,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM subscriptions s WHERE s.member_id = m.id
                             AND s.status = 'ACTIVE' AND
                             (s.plan_type = 'SESSION_BASED' AND s.remaining_sessions > 0
                              OR s.plan_type = 'TIME_BASED' AND s.start_date <= date('now')
                                 AND s.end_date >= date('now'))
                       ) THEN 'ACTIVE' ELSE 'EXPIRED' END AS status
                FROM members m ORDER BY m.id DESC
                """;
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            List<MemberRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new MemberRow(result.getInt("id"), result.getString("full_name"),
                        result.getString("phone"), result.getString("email"),
                        result.getString("gender"), result.getString("medical_notes"),
                        result.getString("plan"), result.getString("status")));
            }
            return rows;
        }
    }

    public void addMember(Member member) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO members(full_name, phone, email, gender, medical_notes) VALUES(?, ?, ?, ?, ?)")) {
            statement.setString(1, member.getFullName());
            statement.setString(2, member.getPhone());
            statement.setString(3, emptyToNull(member.getEmail()));
            statement.setString(4, emptyToNull(member.getGender()));
            statement.setString(5, emptyToNull(member.getMedicalNotes()));
            statement.executeUpdate();
        }
    }

    public void updateMember(int id, Member member) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE members SET full_name=?, phone=?, email=?, gender=?, medical_notes=? WHERE id=?")) {
            statement.setString(1, member.getFullName());
            statement.setString(2, member.getPhone());
            statement.setString(3, emptyToNull(member.getEmail()));
            statement.setString(4, emptyToNull(member.getGender()));
            statement.setString(5, emptyToNull(member.getMedicalNotes()));
            statement.setInt(6, id);
            statement.executeUpdate();
        }
    }

    public void addSubscription(Subscription subscription) throws SQLException {
        boolean sessionBased = subscription.getPlanType() == Subscription.PlanType.SESSION_BASED;
        int durationOrSessions = sessionBased ? subscription.getTotalSessions() : subscription.getDurationMonths();
        LocalDate startDate = subscription.getStartDate();
        String endDate = sessionBased ? null : startDate.plusMonths(durationOrSessions).toString();
        String sql = """
                INSERT INTO subscriptions(member_id, plan_name, plan_type, duration_months, total_sessions,
                                           remaining_sessions, start_date, end_date, price, discount_applied, status,
                                           activity_id)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)
                """;
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement expire = connection.prepareStatement(
                    "UPDATE subscriptions SET status='EXPIRED' WHERE member_id=? AND status='ACTIVE'");
                    PreparedStatement insert = connection.prepareStatement(sql);
                    PreparedStatement payment = connection.prepareStatement(
                            "INSERT INTO payments(member_id, description, amount, payment_date, status, subscription_id) "
                                    + "VALUES(?, ?, ?, ?, 'PENDING', ?)")) {
                expire.setInt(1, subscription.getMemberId());
                expire.executeUpdate();
                insert.setInt(1, subscription.getMemberId());
                insert.setString(2, subscription.getPlanName());
                insert.setString(3, sessionBased ? "SESSION_BASED" : "TIME_BASED");
                insert.setObject(4, sessionBased ? null : durationOrSessions);
                insert.setObject(5, sessionBased ? durationOrSessions : null);
                insert.setObject(6, sessionBased ? durationOrSessions : null);
                insert.setString(7, startDate.toString());
                insert.setString(8, endDate);
                insert.setDouble(9, subscription.getPrice() * (1 - subscription.getDiscountApplied() / 100));
                insert.setDouble(10, subscription.getDiscountApplied());
                if (subscription.getActivityId() == null) {
                    insert.setNull(11, java.sql.Types.INTEGER);
                } else {
                    insert.setInt(11, subscription.getActivityId());
                }
                insert.executeUpdate();
                int subscriptionId;
                try (Statement identity = connection.createStatement();
                        ResultSet result = identity.executeQuery("SELECT last_insert_rowid()")) {
                    if (!result.next()) {
                        throw new SQLException("Could not determine the new subscription ID.");
                    }
                    subscriptionId = result.getInt(1);
                }
                payment.setInt(1, subscription.getMemberId());
                payment.setString(2, subscription.getPlanName());
                payment.setDouble(3, subscription.getPrice() * (1 - subscription.getDiscountApplied() / 100));
                payment.setString(4, "");
                payment.setInt(5, subscriptionId);
                payment.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<SubscriptionRow> subscriptions() throws SQLException {
        String sql = """
                SELECT s.id, m.full_name, s.plan_name, s.plan_type, s.start_date, COALESCE(s.end_date, '-') end_date,
                       CASE WHEN s.plan_type='SESSION_BASED'
                            THEN printf('%d / %d', s.remaining_sessions, s.total_sessions)
                            ELSE '-' END remaining,
                       s.price, COALESCE((SELECT p.status FROM payments p WHERE p.subscription_id=s.id LIMIT 1),
                                         'NOT_RECORDED') payment_status,
                       CASE WHEN s.status='ACTIVE' AND s.plan_type='TIME_BASED'
                                           AND s.end_date < date('now') THEN 'EXPIRED' ELSE s.status END status
                FROM subscriptions s JOIN members m ON m.id=s.member_id ORDER BY s.id DESC
                """;
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            List<SubscriptionRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new SubscriptionRow(result.getInt("id"), result.getString("full_name"),
                        result.getString("plan_name"), result.getString("plan_type"),
                        result.getString("start_date"), result.getString("end_date"),
                        result.getString("remaining"), result.getDouble("price"), result.getString("status"),
                        result.getString("payment_status")));
            }
            return rows;
        }
    }

    public void setSubscriptionStatus(int subscriptionId, String status) throws SQLException {
        if (!"ACTIVE".equals(status) && !"FROZEN".equals(status)) {
            throw new IllegalArgumentException("Only active or frozen subscriptions can be switched.");
        }
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE subscriptions SET status=? WHERE id=? AND status IN ('ACTIVE', 'FROZEN')
                          AND (plan_type='SESSION_BASED' AND remaining_sessions > 0
                               OR plan_type='TIME_BASED' AND start_date <= date('now') AND end_date >= date('now'))
                        """)) {
            statement.setString(1, status);
            statement.setInt(2, subscriptionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The subscription is no longer active or frozen.");
            }
        }
    }

    public CheckInResult checkIn(int memberId) throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement find = connection.prepareStatement("""
                    SELECT s.id, m.full_name, s.plan_name, s.plan_type, s.remaining_sessions,
                           s.total_sessions, s.start_date, s.end_date
                    FROM subscriptions s JOIN members m ON m.id=s.member_id
                    WHERE s.member_id=? AND s.status='ACTIVE'
                      AND (s.plan_type='SESSION_BASED' AND s.remaining_sessions > 0
                           OR s.plan_type='TIME_BASED' AND s.start_date <= date('now') AND s.end_date >= date('now'))
                    ORDER BY s.id DESC LIMIT 1
                    """)) {
                find.setInt(1, memberId);
                int subscriptionId;
                String memberName;
                String planName;
                String type;
                int remaining;
                int total;
                String startDate;
                String endDate;
                try (ResultSet result = find.executeQuery()) {
                    if (!result.next()) {
                        throw new IllegalStateException("NO_VALID_SUBSCRIPTION");
                    }
                    subscriptionId = result.getInt("id");
                    memberName = result.getString("full_name");
                    planName = result.getString("plan_name");
                    type = result.getString("plan_type");
                    remaining = result.getInt("remaining_sessions");
                    total = result.getInt("total_sessions");
                    startDate = result.getString("start_date");
                    endDate = result.getString("end_date");
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO attendance(member_id, subscription_id) VALUES(?, ?)")) {
                    insert.setInt(1, memberId);
                    insert.setInt(2, subscriptionId);
                    insert.executeUpdate();
                }
                if ("SESSION_BASED".equals(type)) {
                    try (PreparedStatement update = connection.prepareStatement("""
                            UPDATE subscriptions SET remaining_sessions=remaining_sessions-1,
                              status=CASE WHEN remaining_sessions <= 1 THEN 'EXPIRED' ELSE status END
                            WHERE id=?
                            """)) {
                        update.setInt(1, subscriptionId);
                        update.executeUpdate();
                    }
                }
                connection.commit();
                return new CheckInResult(memberId, memberName, planName, type,
                        "SESSION_BASED".equals(type) ? remaining - 1 : 0, total, startDate, endDate);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public CheckInResult checkInByQrCode(String value) throws SQLException {
        if (value == null || !value.trim().matches("MEM-\\d{4,}")) {
            throw new IllegalArgumentException("The scanned QR code is not a member code.");
        }
        int memberId;
        try {
            memberId = Integer.parseInt(value.trim().substring(4));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("The scanned member code is invalid.", exception);
        }
        return checkIn(memberId);
    }

    public SubscriptionInvoice subscriptionInvoice(int subscriptionId) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT s.id, m.id member_id, m.full_name, m.phone, s.plan_name, s.plan_type,
                               COALESCE(s.total_sessions, 0) total_sessions,
                               COALESCE(s.remaining_sessions, 0) remaining_sessions,
                               s.start_date, COALESCE(s.end_date, '') end_date, s.price,
                            CASE WHEN p.status='PAID' THEN p.payment_date ELSE '' END payment_date,
                               COALESCE(p.status, 'NOT_RECORDED') payment_status
                        FROM subscriptions s JOIN members m ON m.id=s.member_id
                        LEFT JOIN payments p ON p.subscription_id=s.id
                        WHERE s.id=?
                        """)) {
            statement.setInt(1, subscriptionId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The selected subscription no longer exists.");
                }
                return new SubscriptionInvoice(result.getInt("id"), result.getInt("member_id"),
                        result.getString("full_name"), result.getString("phone"),
                        result.getString("plan_name"), result.getString("plan_type"),
                        result.getInt("total_sessions"), result.getInt("remaining_sessions"),
                        result.getString("start_date"), result.getString("end_date"),
                        result.getDouble("price"), result.getString("payment_date"),
                        result.getString("payment_status"));
            }
        }
    }

    public List<Attendance> attendance() throws SQLException {
        String sql = """
                SELECT m.full_name, m.phone, a.check_in_time, s.plan_name
                FROM attendance a JOIN members m ON m.id=a.member_id
                JOIN subscriptions s ON s.id=a.subscription_id
                WHERE date(a.check_in_time)=date('now') ORDER BY a.check_in_time DESC
                """;
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            List<Attendance> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new Attendance(result.getString("full_name"), result.getString("phone"),
                        result.getString("check_in_time"), result.getString("plan_name")));
            }
            return rows;
        }
    }

    public List<Payment> payments() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT COALESCE(m.full_name, '-') member, p.description, p.amount,
                            CASE WHEN p.status='PAID' THEN p.payment_date ELSE '' END payment_date, p.status
                        FROM payments p LEFT JOIN members m ON m.id=p.member_id ORDER BY p.id DESC
                        """)) {
            List<Payment> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new Payment(result.getString("member"), result.getString("description"),
                        result.getDouble("amount"), result.getString("payment_date"), result.getString("status")));
            }
            return rows;
        }
    }

    public void confirmOfflineSubscriptionPayment(int subscriptionId) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE payments SET status='PAID', payment_date=date('now')
                        WHERE subscription_id=? AND status='PENDING'
                        """)) {
            statement.setInt(1, subscriptionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No pending payment exists for the selected subscription.");
            }
        }
    }

    public void addExpense(String description, double amount, LocalDate date) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO expenses(description, amount, expense_date) VALUES(?, ?, ?)")) {
            statement.setString(1, description.trim());
            statement.setDouble(2, amount);
            statement.setString(3, date.toString());
            statement.executeUpdate();
        }
    }

    public Dashboard dashboard() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        """
                                SELECT (SELECT count(*) FROM members) members,
                                       (SELECT count(DISTINCT member_id) FROM subscriptions
                                        WHERE status='ACTIVE' AND (plan_type='SESSION_BASED' AND remaining_sessions > 0
                                          OR plan_type='TIME_BASED' AND start_date <= date('now')
                                             AND end_date >= date('now'))) active_members,
                                       (SELECT COALESCE(sum(amount),0) FROM payments
                                        WHERE status='PAID' AND strftime('%Y-%m', payment_date)=strftime('%Y-%m','now')) revenue,
                                       (SELECT COALESCE(sum(amount),0) FROM expenses
                                        WHERE strftime('%Y-%m', expense_date)=strftime('%Y-%m','now')) expenses,
                                       (SELECT count(*) FROM attendance WHERE date(check_in_time)=date('now')) check_ins
                                """)) {
            result.next();
            return new Dashboard(result.getInt("members"), result.getInt("active_members"),
                    result.getDouble("revenue"), result.getDouble("expenses"), result.getInt("check_ins"));
        }
    }

    @Override
    public List<User> users() throws SQLException {
        return userRepository.users();
    }

    @Override
    public void addUser(String username, char[] password, String fullName, String role, String phone, double salary)
            throws SQLException, GeneralSecurityException {
        userRepository.addUser(username, password, fullName, role, phone, salary);
    }

    @Override
    public void updateUser(int id, String fullName, String role, String phone, double salary) throws SQLException {
        userRepository.updateUser(id, fullName, role, phone, salary);
    }

    @Override
    public void deactivateUser(int id) throws SQLException {
        userRepository.deactivateUser(id);
    }

    @Override
    public void updatePassword(int userId, char[] password)
            throws SQLException, GeneralSecurityException {
        userRepository.updatePassword(userId, password);
    }

    public void addActivity(Activity activity) throws SQLException {
        clubRepository.addActivity(activity);
    }

    public void updateActivity(int id, Activity activity) throws SQLException {
        clubRepository.updateActivity(id, activity);
    }

    public void deleteActivity(int id) throws SQLException {
        clubRepository.deleteActivity(id);
    }

    public List<ActivityRow> activities() throws SQLException {
        return clubRepository.activities();
    }

    public void addCoach(Coach coach) throws SQLException {
        clubRepository.addCoach(coach);
    }

    public void updateCoach(int id, Coach coach) throws SQLException {
        clubRepository.updateCoach(id, coach);
    }

    public void deleteCoach(int id) throws SQLException {
        clubRepository.deleteCoach(id);
    }

    public List<CoachRow> coaches() throws SQLException {
        return clubRepository.coaches();
    }

    public void addScheduledClass(String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException {
        clubRepository.addScheduledClass(title, activityId, coachId, dayOfWeek,
                startTime, endTime, capacity);
    }

    public void updateScheduledClass(int id, String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException {
        clubRepository.updateScheduledClass(id, title, activityId, coachId, dayOfWeek,
                startTime, endTime, capacity);
    }

    public void deleteScheduledClass(int id) throws SQLException {
        clubRepository.deleteScheduledClass(id);
    }

    public List<ScheduledClass> scheduledClasses() throws SQLException {
        return clubRepository.scheduledClasses();
    }

    public List<Payroll> payroll(YearMonth month) throws SQLException {
        return clubRepository.payroll(month);
    }

    public void paySalary(String employeeType, int employeeId, YearMonth month) throws SQLException {
        clubRepository.paySalary(employeeType, employeeId, month);
    }

    public List<ActivityDemand> activityDemand() throws SQLException {
        return clubRepository.activityDemand();
    }

    public String language() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT setting_value FROM app_settings WHERE setting_key='language'");
                ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : "en";
        }
    }

    public void setLanguage(String language) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO app_settings(setting_key, setting_value) VALUES('language', ?)
                        ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value
                        """)) {
            statement.setString(1, language);
            statement.executeUpdate();
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
