package com.gym.system.dao;

import java.sql.SQLException;

/** Application settings storage. */
public interface SettingsDAO {

    String language() throws SQLException;

    void setLanguage(String language) throws SQLException;
}
