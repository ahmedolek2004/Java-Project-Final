package com.gym.system.dao;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Boundary tests for the shared "usable subscription" rule and the local-date policy. */
class SubscriptionRulesTest {

    @Test
    void todayInSqlMatchesTheLocalDateOfTheJvm() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(SqlClock.apply("SELECT {TODAY}"))) {
            result.next();
            assertEquals(LocalDate.now().toString(), result.getString(1));
        }
    }

    @Test
    void usableConditionHonoursInclusiveEndDateStartDateAndSessions() throws SQLException {
        LocalDate today = LocalDate.now();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
                Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE subscriptions (
                        id INTEGER PRIMARY KEY, plan_type TEXT, remaining_sessions INTEGER,
                        start_date TEXT, end_date TEXT)
                    """);
            insert(statement, 1, "TIME_BASED", null, today.minusDays(5), today.plusDays(5));
            insert(statement, 2, "TIME_BASED", null, today.minusDays(5), today); // last day is still valid
            insert(statement, 3, "TIME_BASED", null, today.minusDays(5), today.minusDays(1)); // ended
            insert(statement, 4, "TIME_BASED", null, today.plusDays(1), today.plusDays(30)); // not started
            insert(statement, 5, "TIME_BASED", null, today, today.plusDays(30)); // starts today
            insert(statement, 6, "SESSION_BASED", 3, today.minusDays(1), null);
            insert(statement, 7, "SESSION_BASED", 0, today.minusDays(1), null); // no sessions left

            assertEquals(List.of(1, 2, 5, 6), usableIds(statement, "s.", "s"));
            assertEquals(List.of(1, 2, 5, 6), usableIds(statement, "", ""));
        }
    }

    private static void insert(Statement statement, int id, String type, Integer sessions, LocalDate start,
            LocalDate end) throws SQLException {
        statement.execute("INSERT INTO subscriptions VALUES (%d, '%s', %s, '%s', %s)".formatted(id, type,
                sessions == null ? "NULL" : sessions, start, end == null ? "NULL" : "'" + end + "'"));
    }

    private static List<Integer> usableIds(Statement statement, String alias, String tableAlias)
            throws SQLException {
        String sql = "SELECT id FROM subscriptions " + tableAlias + " WHERE "
                + SubscriptionRules.USABLE_PLACEHOLDER + " ORDER BY id";
        List<Integer> ids = new ArrayList<>();
        try (ResultSet result = statement.executeQuery(SubscriptionRules.apply(sql, alias))) {
            while (result.next()) {
                ids.add(result.getInt(1));
            }
        }
        return ids;
    }
}
