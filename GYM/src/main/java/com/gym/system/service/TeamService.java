package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.model.User;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;

public class TeamService {
    private final GymDataAccess repository;
    public TeamService(GymDataAccess repository) { this.repository = repository; }

    public List<User> getUsers() throws SQLException {
        return repository.users();
    }
    public void addUser(String username, char[] password, String fullName, String role, String phone, double salary) throws SQLException, GeneralSecurityException {
        repository.addUser(username, password, fullName, role, phone, salary);
    }
    public void updateUser(int id, String fullName, String role, String phone, double salary) throws SQLException {
        repository.updateUser(id, fullName, role, phone, salary);
    }
    public void updatePassword(int id, char[] newPassword) throws SQLException, GeneralSecurityException {
        repository.updatePassword(id, newPassword);
    }
    public void deactivateUser(int id) throws SQLException {
        repository.deactivateUser(id);
    }
}