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
                       COALESCE((SELECT s.plan_name FROM subscriptions s
                                 WHERE s.member_id = m.id AND s.status = 'ACTIVE'
                                   AND {USABLE}
                                 ORDER BY s.id DESC LIMIT 1), '-') AS plan,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM subscriptions s WHERE s.member_id = m.id
                             AND s.status = 'ACTIVE' AND {USABLE}
                       ) THEN 'ACTIVE' ELSE 'EXPIRED' END AS status
                FROM members m ORDER BY m.id DESC
                """;
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(SubscriptionRules.apply(sql, "s."))) {
            List<GymRepository.MemberRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.MemberRow(result.getInt("id"), result.getString("full_name"),
                        result.getString("phone"), result.getString("email"),
                        result.getString("gender"), result.getString("medical_notes"),
                        result.getString("plan"), result.getString("status")));
            }
            return rows;
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

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
