package com.gym.system.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConnection {
    private static final Path DATABASE_PATH = Path.of(
            System.getProperty("user.home"), ".gym-system", "gym_system.db");
    private static final String URL = "jdbc:sqlite:" + DATABASE_PATH;

    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public static void initialize() throws SQLException, IOException {
        Files.createDirectories(DATABASE_PATH.getParent());
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        full_name TEXT NOT NULL,
                        role TEXT NOT NULL CHECK(role IN ('ADMIN', 'RECEPTIONIST')),
                        phone TEXT,
                        salary REAL NOT NULL DEFAULT 0 CHECK(salary >= 0),
                        active INTEGER NOT NULL DEFAULT 1,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS activities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name_ar TEXT NOT NULL,
                        name_en TEXT NOT NULL,
                        description TEXT
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS coaches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT,
                        activity_id INTEGER REFERENCES activities(id) ON DELETE SET NULL,
                        salary REAL NOT NULL DEFAULT 0 CHECK(salary >= 0),
                        is_personal_trainer INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS members (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        full_name TEXT NOT NULL,
                        phone TEXT NOT NULL UNIQUE,
                        email TEXT,
                        gender TEXT,
                        medical_notes TEXT,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS subscriptions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        member_id INTEGER NOT NULL REFERENCES members(id),
                        plan_name TEXT NOT NULL,
                        plan_type TEXT NOT NULL CHECK(plan_type IN ('TIME_BASED', 'SESSION_BASED')),
                        duration_months INTEGER,
                        total_sessions INTEGER,
                        remaining_sessions INTEGER,
                        start_date TEXT NOT NULL,
                        end_date TEXT,
                        price REAL NOT NULL CHECK(price >= 0),
                        discount_applied REAL NOT NULL DEFAULT 0 CHECK(discount_applied >= 0 AND discount_applied <= 100),
                        status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'EXPIRED', 'FROZEN'))
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS class_schedule (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        activity_id INTEGER NOT NULL REFERENCES activities(id),
                        coach_id INTEGER REFERENCES coaches(id) ON DELETE SET NULL,
                        day_of_week INTEGER NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
                        start_time TEXT NOT NULL,
                        end_time TEXT NOT NULL,
                        capacity INTEGER NOT NULL CHECK(capacity > 0),
                        active INTEGER NOT NULL DEFAULT 1,
                        CHECK(start_time < end_time)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS payroll_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        employee_type TEXT NOT NULL CHECK(employee_type IN ('USER', 'COACH')),
                        employee_id INTEGER NOT NULL,
                        employee_name TEXT NOT NULL,
                        pay_month TEXT NOT NULL,
                        amount REAL NOT NULL CHECK(amount > 0),
                        paid_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(employee_type, employee_id, pay_month)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS attendance (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        member_id INTEGER NOT NULL REFERENCES members(id),
                        subscription_id INTEGER NOT NULL REFERENCES subscriptions(id),
                        check_in_time TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS one_checkin_per_member_day
                    ON attendance(member_id, date(check_in_time))
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        member_id INTEGER REFERENCES members(id) ON DELETE SET NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL CHECK(amount >= 0),
                        payment_date TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PAID' CHECK(status IN ('PAID', 'PENDING'))
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL CHECK(amount >= 0),
                        expense_date TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        setting_key TEXT PRIMARY KEY,
                        setting_value TEXT NOT NULL
                    )
                    """);
            ensureColumn(connection, "users", "salary", "REAL NOT NULL DEFAULT 0 CHECK(salary >= 0)");
            ensureColumn(connection, "coaches", "salary", "REAL NOT NULL DEFAULT 0 CHECK(salary >= 0)");
            ensureColumn(connection, "subscriptions", "activity_id", "INTEGER REFERENCES activities(id) ON DELETE SET NULL");
            ensureColumn(connection, "payments", "subscription_id",
                    "INTEGER REFERENCES subscriptions(id) ON DELETE SET NULL");
        }
    }

    private static void ensureColumn(Connection connection, String table, String column, String definition)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equals(columns.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
