package com.gym.system.dao;

/**
 * The one place that defines what "today" and "this month" mean in SQL.
 *
 * <p>Business dates (subscription start/end, expenses) are stored as local dates, while SQLite's
 * {@code CURRENT_TIMESTAMP} is UTC. Every query therefore uses the {@code 'localtime'} modifier so
 * that "today" flips at local midnight, not at 02:00/03:00 local time.
 */
final class SqlClock {

    static final String TODAY_PLACEHOLDER = "{TODAY}";
    static final String THIS_MONTH_PLACEHOLDER = "{THIS_MONTH}";

    /** Current local date, e.g. {@code 2026-10-27}. */
    static final String TODAY = "date('now','localtime')";

    /** Current local year-month, e.g. {@code 2026-10}. */
    static final String THIS_MONTH = "strftime('%Y-%m','now','localtime')";

    private SqlClock() {
    }

    /** Local calendar date of a UTC timestamp column. */
    static String localDate(String utcColumn) {
        return "date(" + utcColumn + ",'localtime')";
    }

    /** Local date and time of a UTC timestamp column. */
    static String localDateTime(String utcColumn) {
        return "datetime(" + utcColumn + ",'localtime')";
    }

    /** Replaces the {@code {TODAY}} and {@code {THIS_MONTH}} placeholders in {@code sql}. */
    static String apply(String sql) {
        return sql.replace(TODAY_PLACEHOLDER, TODAY).replace(THIS_MONTH_PLACEHOLDER, THIS_MONTH);
    }
}
