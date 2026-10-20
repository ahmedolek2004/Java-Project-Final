package com.gym.system.dao;

import com.gym.system.model.Admin;
import com.gym.system.model.Receptionist;
import com.gym.system.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

final class UserRepository implements UserDAO {
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public Optional<User> authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        String sql = "SELECT id, username, full_name, role, phone, salary, password_hash FROM users "
                + "WHERE username = ? AND active = 1";
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !verifyPassword(password, result.getString("password_hash"))) {
                    return Optional.empty();
                }
                int id = result.getInt("id");
                String account = result.getString("username");
                String name = result.getString("full_name");
                String role = result.getString("role");
                String phone = result.getString("phone");
                double salary = result.getDouble("salary");
                return Optional.of("ADMIN".equals(role)
                        ? new Admin(id, account, name, phone, salary)
                        : new Receptionist(id, account, name, phone, salary));
            }
        }
    }

    @Override
    public List<User> users() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT id, username, full_name, role, phone, salary FROM users WHERE active=1 ORDER BY full_name")) {
            List<User> rows = new ArrayList<>();
            while (result.next()) {
                int id = result.getInt("id");
                String username = result.getString("username");
                String fullName = result.getString("full_name");
                String role = result.getString("role");
                String phone = result.getString("phone");
                double salary = result.getDouble("salary");
                rows.add("ADMIN".equals(role) ? new Admin(id, username, fullName, phone, salary)
                        : new Receptionist(id, username, fullName, phone, salary));
            }
            return rows;
        }
    }

    @Override
    public void addUser(String username, char[] password, String fullName, String role, String phone, double salary)
            throws SQLException, GeneralSecurityException {
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users(username, password_hash, full_name, role, phone, salary) VALUES(?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, username.trim());
            statement.setString(2, hashPassword(password));
            statement.setString(3, fullName.trim());
            statement.setString(4, role);
            statement.setString(5, emptyToNull(phone));
            statement.setDouble(6, salary);
            statement.executeUpdate();
        }
    }

    @Override
    public void updateUser(int id, String fullName, String role, String phone, double salary) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET full_name=?, role=?, phone=?, salary=? WHERE id=? AND active=1")) {
            statement.setString(1, fullName.trim());
            statement.setString(2, role);
            statement.setString(3, emptyToNull(phone));
            statement.setDouble(4, salary);
            statement.setInt(5, id);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The staff account no longer exists.");
            }
        }
    }

    @Override
    public void deactivateUser(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement target = connection.prepareStatement(
                    "SELECT role FROM users WHERE id=? AND active=1");
                 PreparedStatement countAdmins = connection.prepareStatement(
                         "SELECT count(*) FROM users WHERE role='ADMIN' AND active=1");
                 PreparedStatement deactivate = connection.prepareStatement(
                         "UPDATE users SET active=0 WHERE id=? AND active=1")) {
                target.setInt(1, id);
                String role;
                try (ResultSet result = target.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("The staff account no longer exists.");
                    }
                    role = result.getString(1);
                }
                countAdmins.execute();
                try (ResultSet result = countAdmins.getResultSet()) {
                    if ("ADMIN".equals(role) && result.next() && result.getInt(1) <= 1) {
                        throw new SQLException("At least one active administrator account must remain.");
                    }
                }
                deactivate.setInt(1, id);
                deactivate.executeUpdate();
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
    public void updatePassword(int userId, char[] password)
            throws SQLException, GeneralSecurityException {
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET password_hash=? WHERE id=? AND active=1")) {
            statement.setString(1, hashPassword(password));
            statement.setInt(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The user account no longer exists.");
            }
        }
    }

    void seedDefaults() throws SQLException, GeneralSecurityException {
        int userCount;
        try (Connection connection = DatabaseConnection.open();
             Statement count = connection.createStatement();
             ResultSet result = count.executeQuery("SELECT count(*) FROM users")) {
            result.next();
            userCount = result.getInt(1);
        }
        if (userCount == 0) {
            addUser("admin", "admin123".toCharArray(), "Gym Administrator", "ADMIN", "", 0);
            addUser("reception", "reception123".toCharArray(), "Reception Desk", "RECEPTIONIST", "", 0);
        }
    }

    private static String hashPassword(char[] password) throws GeneralSecurityException {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = passwordHash(password, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(char[] password, String stored) throws GeneralSecurityException {
        String[] parts = stored.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        return java.security.MessageDigest.isEqual(expected, passwordHash(password, salt));
    }

    private static byte[] passwordHash(char[] password, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PASSWORD_ITERATIONS, PASSWORD_KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
