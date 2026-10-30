package com.gym.system.dao;

import com.gym.system.model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQL access for members. */
final class MemberRepository implements MemberDAO {

    @Override
    public List<GymRepository.MemberRow> members() throws SQLException {
        String sql = """
                SELECT m.id, m.full_name, m.phone, COALESCE(m.email, '') AS email,
                       COALESCE(m.gender, '') AS gender, COALESCE(m.medical_notes, '') AS medical_notes,
                       COALESCE(s.plan_name, '-') AS plan,
                       CASE WHEN s.id IS NULL THEN 'NONE'
                            WHEN s.status = 'FROZEN' AND %2$s THEN 'FROZEN'
                            WHEN s.status = 'ACTIVE' AND %2$s THEN 'ACTIVE'
                            ELSE 'EXPIRED' END AS status,
                       COALESCE(s.id, 0) AS subscription_id, COALESCE(s.plan_type, '') AS plan_type,
                       COALESCE(a.name_ar, '') AS activity_ar, COALESCE(a.name_en, '') AS activity_en,
                       COALESCE(s.start_date, '') AS start_date, COALESCE(s.end_date, '') AS end_date,
                       CASE WHEN s.plan_type = 'SESSION_BASED'
                            THEN printf('%%d / %%d', s.remaining_sessions, s.total_sessions)
                            ELSE '-' END AS remaining,
                       COALESCE(s.price, 0) AS price,
                       COALESCE((SELECT p.status FROM payments p WHERE p.subscription_id = s.id LIMIT 1),
                                'NOT_RECORDED') AS payment_status
                FROM members m
                LEFT JOIN subscriptions s ON s.id = %1$s
                LEFT JOIN activities a ON a.id = s.activity_id
                ORDER BY m.id DESC
                """.formatted(currentSubscriptionId("m.id"), SubscriptionRules.usableCondition("s."));
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            List<GymRepository.MemberRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.MemberRow(result.getInt("id"), result.getString("full_name"),
                        result.getString("phone"), result.getString("email"),
                        result.getString("gender"), result.getString("medical_notes"),
                        result.getString("plan"), result.getString("status"),
                        result.getInt("subscription_id"), result.getString("plan_type"),
                        result.getString("activity_ar"), result.getString("activity_en"),
                        result.getString("start_date"), result.getString("end_date"),
                        result.getString("remaining"), result.getDouble("price"),
                        result.getString("payment_status")));
            }
            return rows;
        }
    }

    @Override
    public List<GymRepository.MemberSubscription> memberSubscriptions(int memberId) throws SQLException {
        String sql = """
                SELECT s.id, s.plan_name, s.plan_type, COALESCE(a.name_ar, '') AS activity_ar,
                       COALESCE(a.name_en, '') AS activity_en, s.start_date, COALESCE(s.end_date, '-') AS end_date,
                       CASE WHEN s.plan_type = 'SESSION_BASED'
                            THEN printf('%d / %d', s.remaining_sessions, s.total_sessions)
                            ELSE '-' END AS remaining,
                       s.price,
                       CASE WHEN s.status = 'ACTIVE' AND s.plan_type = 'TIME_BASED'
                                 AND s.end_date < {TODAY} THEN 'EXPIRED' ELSE s.status END AS status,
                       COALESCE((SELECT p.status FROM payments p WHERE p.subscription_id = s.id LIMIT 1),
                                'NOT_RECORDED') AS payment_status
                FROM subscriptions s LEFT JOIN activities a ON a.id = s.activity_id
                WHERE s.member_id = ? ORDER BY s.id DESC
                """;
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(SqlClock.apply(sql))) {
            statement.setInt(1, memberId);
            try (ResultSet result = statement.executeQuery()) {
                List<GymRepository.MemberSubscription> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new GymRepository.MemberSubscription(result.getInt("id"),
                            result.getString("plan_name"), result.getString("plan_type"),
                            result.getString("activity_ar"), result.getString("activity_en"),
                            result.getString("start_date"), result.getString("end_date"),
                            result.getString("remaining"), result.getDouble("price"),
                            result.getString("status"), result.getString("payment_status")));
                }
                return rows;
            }
        }
    }

    @Override
    public List<GymRepository.MemberClass> memberClasses(int memberId) throws SQLException {
        String sql = """
                SELECT c.title, c.day_of_week, c.start_time, c.end_time, COALESCE(co.name, '-') AS coach
                FROM class_schedule c LEFT JOIN coaches co ON co.id = c.coach_id
                WHERE c.active = 1 AND c.activity_id = (
                    SELECT s.activity_id FROM subscriptions s WHERE s.id = %s)
                ORDER BY c.day_of_week, c.start_time, c.title
                """.formatted(currentSubscriptionId("?"));
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, memberId);
            statement.setInt(2, memberId);
            try (ResultSet result = statement.executeQuery()) {
                List<GymRepository.MemberClass> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new GymRepository.MemberClass(result.getString("title"), result.getInt("day_of_week"),
                            result.getString("start_time"), result.getString("end_time"),
                            result.getString("coach")));
                }
                return rows;
            }
        }
    }

    @Override
    public List<GymRepository.MemberVisit> memberVisits(int memberId, int limit) throws SQLException {
        String sql = """
                SELECT %s AS check_in_time, s.plan_name
                FROM attendance a JOIN subscriptions s ON s.id = a.subscription_id
                WHERE a.member_id = ? ORDER BY a.check_in_time DESC LIMIT ?
                """.formatted(SqlClock.localDateTime("a.check_in_time"));
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, memberId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet result = statement.executeQuery()) {
                List<GymRepository.MemberVisit> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(new GymRepository.MemberVisit(result.getString("check_in_time"),
                            result.getString("plan_name")));
                }
                return rows;
            }
        }
    }

    @Override
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

    @Override
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

    private static String currentSubscriptionId(String memberReference) {
        return """
                COALESCE((SELECT s2.id FROM subscriptions s2
                          WHERE s2.member_id = %1$s AND s2.status IN ('ACTIVE', 'FROZEN') AND %2$s
                          ORDER BY s2.id DESC LIMIT 1),
                         (SELECT s3.id FROM subscriptions s3 WHERE s3.member_id = %1$s
                          ORDER BY s3.id DESC LIMIT 1))
                """.formatted(memberReference, SubscriptionRules.usableCondition("s2."));
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
