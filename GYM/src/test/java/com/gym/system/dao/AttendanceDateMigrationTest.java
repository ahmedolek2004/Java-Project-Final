package com.gym.system.dao;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies the move from the UTC-date unique index to the local {@code check_in_date} column. */
class AttendanceDateMigrationTest {

    private static final String LEGACY_TABLE = """
            CREATE TABLE attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                member_id INTEGER NOT NULL,
                subscription_id INTEGER NOT NULL,
                check_in_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)
            """;

    @Test
    void replacesTheOldIndexAndBackfillsLocalDates() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement()) {
            statement.execute(LEGACY_TABLE);
            statement.execute("CREATE UNIQUE INDEX one_checkin_per_member_day "
                    + "ON attendance(member_id, date(check_in_time))");
            statement.execute("INSERT INTO attendance(member_id, subscription_id, check_in_time) "
                    + "VALUES (1, 1, '2026-01-10 12:00:00'), (2, 1, '2026-01-10 12:00:00')");

            AttendanceDateMigration.apply(connection);

            assertEquals(0, count(statement, "SELECT count(*) FROM sqlite_master WHERE name = '"
                    + AttendanceDateMigration.OLD_INDEX + "'"));
            assertEquals(1, count(statement, "SELECT count(*) FROM sqlite_master WHERE name = '"
                    + AttendanceDateMigration.NEW_INDEX + "'"));
            assertEquals(2, count(statement, "SELECT count(*) FROM attendance WHERE check_in_date = "
                    + SqlClock.localDate("check_in_time")));
        }
    }

    @Test
    void keepsLegacyCollisionsAsHistoryAndStaysIdempotent() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement()) {
            statement.execute(LEGACY_TABLE); // no old index, so two rows can share one local day
            statement.execute("INSERT INTO attendance(member_id, subscription_id, check_in_time) "
                    + "VALUES (1, 1, '2026-01-10 12:00:00'), (1, 1, '2026-01-10 12:05:00')");

            AttendanceDateMigration.apply(connection);
            AttendanceDateMigration.apply(connection); // running twice must not fail or change data

            assertEquals(2, count(statement, "SELECT count(*) FROM attendance")); // no rows deleted
            assertNull(text(statement, "SELECT check_in_date FROM attendance WHERE id = 1"));
            assertNotNull(text(statement, "SELECT check_in_date FROM attendance WHERE id = 2"));
        }
    }

    @Test
    void rejectsASecondCheckInOnTheSameLocalDayOnly() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement()) {
            statement.execute(LEGACY_TABLE);
            AttendanceDateMigration.apply(connection);
            String insert = "INSERT INTO attendance(member_id, subscription_id, check_in_date) VALUES (1, 1, '%s')";

            statement.execute(insert.formatted("2026-01-10"));
            statement.execute(insert.formatted("2026-01-11")); // next day is allowed
            assertThrows(SQLException.class, () -> statement.execute(insert.formatted("2026-01-10")));
        }
    }

    private static int count(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String text(Statement statement, String sql) throws SQLException {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
