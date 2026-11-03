package com.gym.system.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Moves the "one check-in per member per day" rule from the UTC date of the timestamp to an explicit
 * local {@code check_in_date} column. SQLite cannot index {@code date(x,'localtime')} because the
 * function is not deterministic, so the local date is stored when the check-in is recorded.
 *
 * <p>The migration is idempotent and never deletes attendance rows. If legacy rows collide on the
 * same local day, only the latest one receives a date; the older rows keep a NULL date (history
 * only) so the unique index can still be created.
 */
final class AttendanceDateMigration {

    static final String OLD_INDEX = "one_checkin_per_member_day";
    static final String NEW_INDEX = "one_checkin_per_member_local_day";

    private AttendanceDateMigration() {
    }

    static void apply(Connection connection) throws SQLException {
        DatabaseConnection.ensureColumn(connection, "attendance", "check_in_date", "TEXT");
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX IF EXISTS " + OLD_INDEX);
            statement.execute("""
                    UPDATE attendance SET check_in_date = %s
                    WHERE check_in_date IS NULL
                      AND id IN (SELECT MAX(id) FROM attendance GROUP BY member_id, %s)
                    """.formatted(SqlClock.localDate("check_in_time"), SqlClock.localDate("check_in_time")));
            statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS " + NEW_INDEX
                    + " ON attendance(member_id, check_in_date)");
        }
    }
}
