package com.gym.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

final class PlanRepository implements PlanDAO {

    @Override
    public List<GymRepository.PlanRow> plans() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT id, name, plan_type, COALESCE(duration_months, 0) duration_months,
                               COALESCE(total_sessions, 0) total_sessions, price
                        FROM plans ORDER BY plan_type, COALESCE(duration_months, total_sessions), name
                        """)) {
            List<GymRepository.PlanRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.PlanRow(result.getInt("id"), result.getString("name"),
                        result.getString("plan_type"), result.getInt("duration_months"),
                        result.getInt("total_sessions"), result.getDouble("price")));
            }
            return rows;
        }
    }

    @Override
    public void addPlan(String name, String planType, int durationOrSessions, double price) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO plans(name, plan_type, duration_months, total_sessions, price)
                        VALUES(?, ?, ?, ?, ?)
                        """)) {
            bind(statement, name, planType, durationOrSessions, price);
            statement.executeUpdate();
        }
    }

    @Override
    public void updatePlan(int id, String name, String planType, int durationOrSessions, double price)
            throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        UPDATE plans SET name=?, plan_type=?, duration_months=?, total_sessions=?, price=?
                        WHERE id=?
                        """)) {
            bind(statement, name, planType, durationOrSessions, price);
            statement.setInt(6, id);
            statement.executeUpdate();
        }
    }

    @Override
    public void deletePlan(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM plans WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, String name, String planType, int durationOrSessions,
            double price) throws SQLException {
        boolean sessionBased = "SESSION_BASED".equals(planType);
        statement.setString(1, name.trim());
        statement.setString(2, planType);
        statement.setObject(3, sessionBased ? null : durationOrSessions);
        statement.setObject(4, sessionBased ? durationOrSessions : null);
        statement.setDouble(5, price);
    }
}
