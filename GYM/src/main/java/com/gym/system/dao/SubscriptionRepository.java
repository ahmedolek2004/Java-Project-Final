package com.gym.system.dao;

import com.gym.system.model.Subscription;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQL access for subscriptions: selling, listing, freezing and invoicing. */
final class SubscriptionRepository implements SubscriptionDAO {

    @Override
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
                    insert.setNull(11, Types.INTEGER);
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

    @Override
    public List<GymRepository.SubscriptionRow> subscriptions() throws SQLException {
        String sql = """
                SELECT s.id, m.full_name, s.plan_name, s.plan_type, s.start_date, COALESCE(s.end_date, '-') end_date,
                       CASE WHEN s.plan_type='SESSION_BASED'
                            THEN printf('%d / %d', s.remaining_sessions, s.total_sessions)
                            ELSE '-' END remaining,
                       s.price, COALESCE((SELECT p.status FROM payments p WHERE p.subscription_id=s.id LIMIT 1),
                                         'NOT_RECORDED') payment_status,
                       CASE WHEN s.status='ACTIVE' AND s.plan_type='TIME_BASED'
                                           AND s.end_date < {TODAY} THEN 'EXPIRED' ELSE s.status END status
                FROM subscriptions s JOIN members m ON m.id=s.member_id ORDER BY s.id DESC
                """;
        try (Connection connection = DatabaseConnection.open();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(SqlClock.apply(sql))) {
            List<GymRepository.SubscriptionRow> rows = new ArrayList<>();
            while (result.next()) {
                rows.add(new GymRepository.SubscriptionRow(result.getInt("id"), result.getString("full_name"),
                        result.getString("plan_name"), result.getString("plan_type"),
                        result.getString("start_date"), result.getString("end_date"),
                        result.getString("remaining"), result.getDouble("price"), result.getString("status"),
                        result.getString("payment_status")));
            }
            return rows;
        }
    }

    @Override
    public void setSubscriptionStatus(int subscriptionId, String status) throws SQLException {
        if (!"ACTIVE".equals(status) && !"FROZEN".equals(status)) {
            throw new IllegalArgumentException("Only active or frozen subscriptions can be switched.");
        }
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(SubscriptionRules.apply("""
                        UPDATE subscriptions SET status=? WHERE id=? AND status IN ('ACTIVE', 'FROZEN')
                          AND {USABLE}
                        """, ""))) {
            statement.setString(1, status);
            statement.setInt(2, subscriptionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The subscription is no longer active or frozen.");
            }
        }
    }

    @Override
    public GymRepository.SubscriptionInvoice subscriptionInvoice(int subscriptionId) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT s.id, COALESCE(p.id, 0) payment_id, m.id member_id, m.full_name, m.phone,
                               s.plan_name, s.plan_type,
                               COALESCE(a.name_ar, '') activity_ar, COALESCE(a.name_en, '') activity_en,
                               COALESCE(s.total_sessions, 0) total_sessions,
                               COALESCE(s.remaining_sessions, 0) remaining_sessions,
                               s.start_date, COALESCE(s.end_date, '') end_date, s.price,
                            CASE WHEN p.status='PAID' THEN p.payment_date ELSE '' END payment_date,
                               COALESCE(p.status, 'NOT_RECORDED') payment_status
                        FROM subscriptions s JOIN members m ON m.id=s.member_id
                        LEFT JOIN payments p ON p.subscription_id=s.id
                        LEFT JOIN activities a ON a.id=s.activity_id
                        WHERE s.id=?
                        """)) {
            statement.setInt(1, subscriptionId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The selected subscription no longer exists.");
                }
                return new GymRepository.SubscriptionInvoice(result.getInt("id"), result.getInt("payment_id"),
                        result.getInt("member_id"),
                        result.getString("full_name"), result.getString("phone"),
                        result.getString("plan_name"), result.getString("plan_type"),
                        result.getInt("total_sessions"), result.getInt("remaining_sessions"),
                        result.getString("start_date"), result.getString("end_date"),
                        result.getDouble("price"), result.getString("payment_date"),
                        result.getString("payment_status"), result.getString("activity_ar"),
                        result.getString("activity_en"));
            }
        }
    }
}
