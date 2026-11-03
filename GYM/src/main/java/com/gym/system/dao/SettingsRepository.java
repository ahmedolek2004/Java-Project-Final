package com.gym.system.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** SQL access for application settings. */
final class SettingsRepository implements SettingsDAO {

    @Override
    public String language() throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT setting_value FROM app_settings WHERE setting_key='language'");
                ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getString(1) : "en";
        }
    }

    @Override
    public void setLanguage(String language) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO app_settings(setting_key, setting_value) VALUES('language', ?)
                        ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value
                        """)) {
            statement.setString(1, language);
            statement.executeUpdate();
        }
    }
}
