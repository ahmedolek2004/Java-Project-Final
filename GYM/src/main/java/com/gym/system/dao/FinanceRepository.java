package com.gym.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQL access for payments, expenses and the dashboard totals. */
final class FinanceRepository implements FinanceDAO {

    @Override
    public List<GymRepository.Payment> payments() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT COALESCE(m.full_name, '-') member, p.description, p.amount,
                            CASE WHEN p.status='PAID' THEN p.payment_date ELSE '' END payment_date, p.status
                        FROM payments p LEFT JOIN members m ON m.id=p.member_id ORDER BY p.id DESC
                        """)) {
            List<GymRepository.Payment> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.Payment(result.getString("member"), result.getString("description"),
                        result.getDouble("amount"), result.getString("payment_date"), result.getString("status")));
            }
            return rows;
        }
    }

    @Override
    public void confirmOfflineSubscriptionPayment(int subscriptionId) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(SqlClock.apply("""
                        UPDATE payments SET status='PAID', payment_date={TODAY}
                        WHERE subscription_id=? AND status='PENDING'
                        """))) {
            statement.setInt(1, subscriptionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("No pending payment exists for the selected subscription.");
            }
        }
    }

    @Override
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

    @Override
    public GymRepository.Dashboard dashboard() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(SubscriptionRules.apply(
                        """
                                SELECT (SELECT count(*) FROM members) members,
                                       (SELECT count(DISTINCT member_id) FROM subscriptions
                                        WHERE status='ACTIVE' AND {USABLE}) active_members,
                                       (SELECT COALESCE(sum(amount),0) FROM payments
                                        WHERE status='PAID' AND strftime('%Y-%m', payment_date)={THIS_MONTH}) revenue,
                                       (SELECT COALESCE(sum(amount),0) FROM expenses
                                        WHERE strftime('%Y-%m', expense_date)={THIS_MONTH}) expenses,
                                       (SELECT count(*) FROM attendance WHERE date(check_in_time,'localtime')={TODAY}) check_ins
                                """, ""))) {
            result.next();
            return new GymRepository.Dashboard(result.getInt("members"), result.getInt("active_members"),
                    result.getDouble("revenue"), result.getDouble("expenses"), result.getInt("check_ins"));
        }
    }
}
