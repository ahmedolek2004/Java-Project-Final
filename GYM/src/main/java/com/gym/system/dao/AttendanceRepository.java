package com.gym.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access for check-ins and the daily attendance list. */
final class AttendanceRepository implements AttendanceDAO {

    @Override
    public GymRepository.CheckInResult checkIn(int memberId) throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement find = connection.prepareStatement(SubscriptionRules.apply("""
                    SELECT s.id, m.full_name, s.plan_name, s.plan_type, s.remaining_sessions,
                           s.total_sessions, s.start_date, s.end_date
                    FROM subscriptions s JOIN members m ON m.id=s.member_id
                    WHERE s.member_id=? AND s.status='ACTIVE'
                      AND {USABLE}
                    ORDER BY s.id DESC LIMIT 1
                    """, "s."))) {
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
                try (PreparedStatement insert = connection.prepareStatement(SqlClock.apply(
                        "INSERT INTO attendance(member_id, subscription_id, check_in_date) VALUES(?, ?, {TODAY})"))) {
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
                return new GymRepository.CheckInResult(memberId, memberName, planName, type,
                        "SESSION_BASED".equals(type) ? remaining - 1 : 0, total, startDate, endDate);
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public GymRepository.CheckInResult checkInByQrCode(String value) throws SQLException {
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

    @Override
    public List<GymRepository.Attendance> attendance() throws SQLException {
        String sql = """
                SELECT m.full_name, m.phone, %s AS check_in_time, s.plan_name
                FROM attendance a JOIN members m ON m.id=a.member_id
                JOIN subscriptions s ON s.id=a.subscription_id
                WHERE %s = {TODAY} ORDER BY a.check_in_time DESC
                """.formatted(SqlClock.localDateTime("a.check_in_time"), SqlClock.localDate("a.check_in_time"));
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(SqlClock.apply(sql))) {
            List<GymRepository.Attendance> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.Attendance(result.getString("full_name"), result.getString("phone"),
                        result.getString("check_in_time"), result.getString("plan_name")));
            }
            return rows;
        }
    }
}
