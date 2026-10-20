package com.gym.system.dao;

import com.gym.system.model.Activity;
import com.gym.system.model.Coach;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

final class ClubRepository {
    List<GymRepository.ActivityRow> activities() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT id, name_ar, name_en FROM activities ORDER BY name_en")) {
            List<GymRepository.ActivityRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.ActivityRow(result.getInt("id"),
                        result.getString("name_ar"), result.getString("name_en")));
            }
            return rows;
        }
    }

    void addActivity(Activity activity) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO activities(name_ar, name_en) VALUES(?, ?)")) {
            statement.setString(1, activity.nameArabic());
            statement.setString(2, activity.nameEnglish());
            statement.executeUpdate();
        }
    }

    void updateActivity(int id, Activity activity) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE activities SET name_ar=?, name_en=? WHERE id=?")) {
            statement.setString(1, activity.nameArabic());
            statement.setString(2, activity.nameEnglish());
            statement.setInt(3, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The activity no longer exists.");
            }
        }
    }

    void deleteActivity(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM activities WHERE id=?")) {
            statement.setInt(1, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The activity no longer exists.");
            }
        }
    }

    List<GymRepository.CoachRow> coaches() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT c.id, c.name, COALESCE(c.phone, '') phone, COALESCE(a.name_en, '-') activity, c.salary,
                               c.is_personal_trainer
                        FROM coaches c LEFT JOIN activities a ON a.id=c.activity_id ORDER BY c.name
                        """)) {
            List<GymRepository.CoachRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.CoachRow(result.getInt("id"), result.getString("name"),
                        result.getString("phone"), result.getString("activity"), result.getDouble("salary"),
                        result.getBoolean("is_personal_trainer")));
            }
            return rows;
        }
    }

    void addCoach(Coach coach) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO coaches(name, phone, activity_id, salary, is_personal_trainer) VALUES(?, ?, ?, ?, ?)")) {
            statement.setString(1, coach.getName());
            statement.setString(2, emptyToNull(coach.getPhone()));
            setNullableInteger(statement, 3, coach.getActivityId());
            statement.setDouble(4, coach.getSalary());
            statement.setBoolean(5, coach.isPersonalTrainer());
            statement.executeUpdate();
        }
    }

    void updateCoach(int id, Coach coach) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE coaches SET name=?, phone=?, activity_id=?, salary=?, is_personal_trainer=?
                        WHERE id=?
                        """)) {
            statement.setString(1, coach.getName());
            statement.setString(2, emptyToNull(coach.getPhone()));
            setNullableInteger(statement, 3, coach.getActivityId());
            statement.setDouble(4, coach.getSalary());
            statement.setBoolean(5, coach.isPersonalTrainer());
            statement.setInt(6, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The coach no longer exists.");
            }
        }
    }

    void deleteCoach(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM coaches WHERE id=?")) {
            statement.setInt(1, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The coach no longer exists.");
            }
        }
    }

    void addScheduledClass(String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException {
        validateSchedule(dayOfWeek, startTime, endTime, capacity);
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        """
                                INSERT INTO class_schedule(title, activity_id, coach_id, day_of_week, start_time, end_time, capacity)
                                VALUES(?, ?, ?, ?, ?, ?, ?)
                                """)) {
            statement.setString(1, title.trim());
            statement.setInt(2, activityId);
            setNullableInteger(statement, 3, coachId);
            statement.setInt(4, dayOfWeek);
            statement.setString(5, startTime);
            statement.setString(6, endTime);
            statement.setInt(7, capacity);
            statement.executeUpdate();
        }
    }

    void updateScheduledClass(int id, String title, int activityId, Integer coachId, int dayOfWeek,
            String startTime, String endTime, int capacity) throws SQLException {
        validateSchedule(dayOfWeek, startTime, endTime, capacity);
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE class_schedule SET title=?, activity_id=?, coach_id=?, day_of_week=?,
                            start_time=?, end_time=?, capacity=? WHERE id=? AND active=1
                        """)) {
            statement.setString(1, title.trim());
            statement.setInt(2, activityId);
            setNullableInteger(statement, 3, coachId);
            statement.setInt(4, dayOfWeek);
            statement.setString(5, startTime);
            statement.setString(6, endTime);
            statement.setInt(7, capacity);
            statement.setInt(8, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The scheduled class no longer exists.");
            }
        }
    }

    void deleteScheduledClass(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE class_schedule SET active=0 WHERE id=? AND active=1")) {
            statement.setInt(1, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The scheduled class no longer exists.");
            }
        }
    }

    List<GymRepository.ScheduledClass> scheduledClasses() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT c.id, c.title, a.name_en activity, COALESCE(co.name, '-') coach,
                               c.day_of_week, c.start_time, c.end_time, c.capacity
                        FROM class_schedule c JOIN activities a ON a.id=c.activity_id
                        LEFT JOIN coaches co ON co.id=c.coach_id
                        WHERE c.active=1
                        ORDER BY c.day_of_week, c.start_time, c.title
                        """)) {
            List<GymRepository.ScheduledClass> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.ScheduledClass(result.getInt("id"), result.getString("title"),
                        result.getString("activity"), result.getString("coach"),
                        result.getInt("day_of_week"), result.getString("start_time"),
                        result.getString("end_time"), result.getInt("capacity")));
            }
            return rows;
        }
    }

    List<GymRepository.Payroll> payroll(YearMonth month) throws SQLException {
        String sql = """
                SELECT u.id, 'USER' employee_type, u.full_name employee_name, u.role,
                       u.salary monthly_salary, p.paid_at
                FROM users u LEFT JOIN payroll_payments p
                  ON p.employee_type='USER' AND p.employee_id=u.id AND p.pay_month=?
                WHERE u.active=1
                UNION ALL
                SELECT c.id, 'COACH', c.name, 'COACH', c.salary, p.paid_at
                FROM coaches c LEFT JOIN payroll_payments p
                  ON p.employee_type='COACH' AND p.employee_id=c.id AND p.pay_month=?
                ORDER BY employee_name
                """;
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, month.toString());
            statement.setString(2, month.toString());
            try (ResultSet result = statement.executeQuery()) {
                List<GymRepository.Payroll> rows = new ArrayList<>();
                while (result.next()) {
                    String paidAt = result.getString("paid_at");
                    rows.add(new GymRepository.Payroll(result.getInt("id"), result.getString("employee_type"),
                            result.getString("employee_name"), result.getString("role"),
                            result.getDouble("monthly_salary"), paidAt != null, paidAt));
                }
                return rows;
            }
        }
    }

    void paySalary(String employeeType, int employeeId, YearMonth month) throws SQLException {
        if (!"USER".equals(employeeType) && !"COACH".equals(employeeType)) {
            throw new IllegalArgumentException("Unknown employee type.");
        }
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                String employeeSql = "USER".equals(employeeType)
                        ? "SELECT full_name, salary FROM users WHERE id=? AND active=1"
                        : "SELECT name, salary FROM coaches WHERE id=?";
                String name;
                double salary;
                try (PreparedStatement employee = connection.prepareStatement(employeeSql)) {
                    employee.setInt(1, employeeId);
                    try (ResultSet result = employee.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("The employee no longer exists.");
                        }
                        name = result.getString(1);
                        salary = result.getDouble(2);
                    }
                }
                if (salary <= 0) {
                    throw new SQLException("Set a positive monthly salary before processing payroll.");
                }
                try (PreparedStatement payment = connection.prepareStatement("""
                        INSERT INTO payroll_payments(employee_type, employee_id, employee_name, pay_month, amount)
                        VALUES(?, ?, ?, ?, ?)
                        """)) {
                    payment.setString(1, employeeType);
                    payment.setInt(2, employeeId);
                    payment.setString(3, name);
                    payment.setString(4, month.toString());
                    payment.setDouble(5, salary);
                    payment.executeUpdate();
                }
                try (PreparedStatement expense = connection.prepareStatement("""
                        INSERT INTO expenses(description, amount, expense_date) VALUES(?, ?, ?)
                        """)) {
                    expense.setString(1, "Salary · " + name + " · " + month);
                    expense.setDouble(2, salary);
                    expense.setString(3, month.atDay(1).toString());
                    expense.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    List<GymRepository.ActivityDemand> activityDemand() throws SQLException {
        String sql = """
                SELECT a.name_en, a.name_ar, COALESCE(s.members, 0) members,
                       COALESCE(at.check_ins, 0) check_ins, COALESCE(s.revenue, 0) revenue
                FROM activities a
                LEFT JOIN (
                          SELECT s.activity_id, COUNT(DISTINCT s.member_id) members,
                              SUM(CASE WHEN p.status='PAID' THEN s.price ELSE 0 END) revenue
                          FROM subscriptions s LEFT JOIN payments p ON p.subscription_id=s.id
                          WHERE s.activity_id IS NOT NULL GROUP BY s.activity_id
                ) s ON s.activity_id=a.id
                LEFT JOIN (
                    SELECT s.activity_id, COUNT(at.id) check_ins
                    FROM subscriptions s JOIN attendance at ON at.subscription_id=s.id
                    WHERE s.activity_id IS NOT NULL GROUP BY s.activity_id
                ) at ON at.activity_id=a.id
                ORDER BY members DESC, check_ins DESC, revenue DESC, a.name_en
                """;
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            List<GymRepository.ActivityDemand> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.ActivityDemand(result.getString("name_ar"), result.getString("name_en"),
                        result.getInt("members"), result.getInt("check_ins"), result.getDouble("revenue")));
            }
            return rows;
        }
    }

    void seedActivities() throws SQLException {
        int activityCount;
        try (Connection connection = DatabaseConnection.open();
                Statement count = connection.createStatement();
                ResultSet result = count.executeQuery("SELECT count(*) FROM activities")) {
            result.next();
            activityCount = result.getInt(1);
        }
        if (activityCount == 0) {
            try (Connection connection = DatabaseConnection.open();
                    Statement insert = connection.createStatement()) {
                insert.executeUpdate("""
                        INSERT INTO activities(name_ar, name_en) VALUES
                        ('كمال الأجسام', 'Gym'),
                        ('الملاكمة', 'Boxing'),
                        ('الكاراتيه', 'Karate'),
                        ('اللياقة البدنية', 'Fitness')
                        """);
            }
        }
    }

    private static void validateSchedule(int dayOfWeek, String startTime, String endTime, int capacity) {
        if (dayOfWeek < 1 || dayOfWeek > 7 || capacity <= 0
                || startTime == null || !startTime.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")
                || endTime == null || !endTime.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")
                || startTime.compareTo(endTime) >= 0) {
            throw new IllegalArgumentException("Enter a valid day, capacity and time range.");
        }
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
