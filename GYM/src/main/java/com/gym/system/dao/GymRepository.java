package com.gym.system.dao;

import com.gym.system.model.Activity;
import com.gym.system.model.Coach;
import com.gym.system.model.Member;
import com.gym.system.model.Subscription;
import com.gym.system.model.User;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Facade over the focused repositories. It owns no SQL itself; every call is delegated to the
 * repository responsible for that part of the data.
 */
public final class GymRepository implements GymDataAccess {
    private final UserRepository userRepository = new UserRepository();
    private final ClubRepository clubRepository = new ClubRepository();
    private final MemberRepository memberRepository = new MemberRepository();
    private final SubscriptionRepository subscriptionRepository = new SubscriptionRepository();
    private final AttendanceRepository attendanceRepository = new AttendanceRepository();
    private final FinanceRepository financeRepository = new FinanceRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();

    public GymRepository() throws SQLException, GeneralSecurityException {
        userRepository.seedDefaults();
        clubRepository.seedActivities();
    }

    @Override
    public Optional<User> authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        return userRepository.authenticate(username, password);
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

    @Override
    public void addActivity(Activity activity) throws SQLException {
        clubRepository.addActivity(activity);
    }

    @Override
    public void updateActivity(int id, Activity activity) throws SQLException {
        clubRepository.updateActivity(id, activity);
    }

    @Override
    public void deleteActivity(int id) throws SQLException {
        clubRepository.deleteActivity(id);
    }

    @Override
    public List<ActivityRow> activities() throws SQLException {
        return clubRepository.activities();
    }

    @Override
    public void addCoach(Coach coach) throws SQLException {
        clubRepository.addCoach(coach);
    }

    @Override
    public void updateCoach(int id, Coach coach) throws SQLException {
        clubRepository.updateCoach(id, coach);
    }

    @Override
    public void deleteCoach(int id) throws SQLException {
        clubRepository.deleteCoach(id);
    }

    @Override
    public List<CoachRow> coaches() throws SQLException {
        return clubRepository.coaches();
    }

    @Override
    public void addScheduledClass(String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException {
        clubRepository.addScheduledClass(title, activityId, coachId, dayOfWeek,
                startTime, endTime, capacity);
    }

    @Override
    public void updateScheduledClass(int id, String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException {
        clubRepository.updateScheduledClass(id, title, activityId, coachId, dayOfWeek,
                startTime, endTime, capacity);
    }

    @Override
    public void deleteScheduledClass(int id) throws SQLException {
        clubRepository.deleteScheduledClass(id);
    }

    @Override
    public List<ScheduledClass> scheduledClasses() throws SQLException {
        return clubRepository.scheduledClasses();
    }

    @Override
    public List<Payroll> payroll(YearMonth month) throws SQLException {
        return clubRepository.payroll(month);
    }

    @Override
    public void paySalary(String employeeType, int employeeId, YearMonth month) throws SQLException {
        clubRepository.paySalary(employeeType, employeeId, month);
    }

    @Override
    public List<ActivityDemand> activityDemand() throws SQLException {
        return clubRepository.activityDemand();
    }

    @Override
    public List<MemberRow> members() throws SQLException {
        return memberRepository.members();
    }

    @Override
    public void addMember(Member member) throws SQLException {
        memberRepository.addMember(member);
    }

    @Override
    public void updateMember(int id, Member member) throws SQLException {
        memberRepository.updateMember(id, member);
    }

    @Override
    public void addSubscription(Subscription subscription) throws SQLException {
        subscriptionRepository.addSubscription(subscription);
    }

    @Override
    public List<SubscriptionRow> subscriptions() throws SQLException {
        return subscriptionRepository.subscriptions();
    }

    @Override
    public void setSubscriptionStatus(int subscriptionId, String status) throws SQLException {
        subscriptionRepository.setSubscriptionStatus(subscriptionId, status);
    }

    @Override
    public SubscriptionInvoice subscriptionInvoice(int subscriptionId) throws SQLException {
        return subscriptionRepository.subscriptionInvoice(subscriptionId);
    }

    @Override
    public CheckInResult checkIn(int memberId) throws SQLException {
        return attendanceRepository.checkIn(memberId);
    }

    @Override
    public CheckInResult checkInByQrCode(String value) throws SQLException {
        return attendanceRepository.checkInByQrCode(value);
    }

    @Override
    public List<Attendance> attendance() throws SQLException {
        return attendanceRepository.attendance();
    }

    @Override
    public List<Payment> payments() throws SQLException {
        return financeRepository.payments();
    }

    @Override
    public void confirmOfflineSubscriptionPayment(int subscriptionId) throws SQLException {
        financeRepository.confirmOfflineSubscriptionPayment(subscriptionId);
    }

    @Override
    public void addExpense(String description, double amount, LocalDate date) throws SQLException {
        financeRepository.addExpense(description, amount, date);
    }

    @Override
    public Dashboard dashboard() throws SQLException {
        return financeRepository.dashboard();
    }

    @Override
    public String language() throws SQLException {
        return settingsRepository.language();
    }

    @Override
    public void setLanguage(String language) throws SQLException {
        settingsRepository.setLanguage(language);
    }

    /** Read-only row for the activities table. */
    public record ActivityRow(int id, String nameArabic, String nameEnglish) {}

    /** Read-only row for the coaches table. */
    public record CoachRow(int id, String name, String phone, String activity,
            double salary, boolean personalTrainer) {}

    /** Read-only row for the class schedule table. */
    public record ScheduledClass(int id, String title, String activity, String coach,
            int dayOfWeek, String startTime, String endTime, int capacity) {}

    /** Payroll line for one employee in a month. */
    public record Payroll(int id, String employeeType, String employeeName, String role,
            double monthlySalary, boolean paid, String paidAt) {}

    /** Popularity metrics for one activity. */
    public record ActivityDemand(String nameArabic, String nameEnglish,
            int members, int checkIns, double revenue) {}

    /** Read-only row for the members table. */
    public record MemberRow(int id, String fullName, String phone, String email, String gender,
            String medicalNotes, String plan, String status) {}

    /** Read-only row for the subscriptions table. */
    public record SubscriptionRow(int id, String member, String plan, String type,
            String startDate, String endDate, String remaining, double price,
            String status, String paymentStatus) {}

    /** Printable invoice data for one subscription. */
    public record SubscriptionInvoice(int subscriptionId, int memberId, String member, String phone,
            String planName, String planType, int totalSessions, int remainingSessions,
            String startDate, String endDate, double amount, String paymentDate,
            String paymentStatus) {}

    /** Check-in receipt for one visit. */
    public record CheckInResult(int memberId, String memberName, String planName, String planType,
            int remainingSessions, int totalSessions, String startDate, String endDate) {
        public boolean sessionBased() {
            return "SESSION_BASED".equals(planType);
        }
    }

    /** Read-only row for today's attendance list. */
    public record Attendance(String member, String phone, String checkedInAt, String plan) {}

    /** Read-only row for the payments table. */
    public record Payment(String member, String description, double amount,
            String date, String status) {}

    /** Aggregated numbers for the dashboard and finance pages. */
    public record Dashboard(int members, int activeMembers, double revenue,
            double expenses, int checkInsToday) {}
}
