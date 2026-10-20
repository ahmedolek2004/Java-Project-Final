package com.gym.system.dao;

import com.gym.system.model.User;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    Optional<User> authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException;
    List<User> users() throws SQLException;
    void addUser(String username, char[] password, String fullName, String role, String phone, double salary)
            throws SQLException, GeneralSecurityException;
    void updateUser(int id, String fullName, String role, String phone, double salary) throws SQLException;
    void deactivateUser(int id) throws SQLException;
    void updatePassword(int userId, char[] password) throws SQLException, GeneralSecurityException;
}
