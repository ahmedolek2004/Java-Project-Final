package com.gym.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class ClassCapacity {

    private ClassCapacity() {
    }

    static int activityOf(Connection connection, int classId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT activity_id FROM class_schedule WHERE id=? AND active=1")) {
            statement.setInt(1, classId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("CLASS_NOT_FOUND");
                }
                return result.getInt(1);
            }
        }
    }

    static void requireFreeSeat(Connection connection, int classId, Integer ignoredSubscriptionId)
            throws SQLException {
        String sql = SubscriptionRules.apply("""
                SELECT c.capacity - (SELECT count(*) FROM subscriptions s
                                     WHERE s.class_id = c.id AND s.status IN ('ACTIVE', 'FROZEN')
                                       AND {USABLE} AND s.id <> ?) AS free
                FROM class_schedule c WHERE c.id = ? AND c.active = 1
                """, "s.");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ignoredSubscriptionId == null ? 0 : ignoredSubscriptionId);
            statement.setInt(2, classId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("CLASS_NOT_FOUND");
                }
                if (result.getInt("free") <= 0) {
                    throw new IllegalStateException("CLASS_FULL");
                }
            }
        }
    }
}
