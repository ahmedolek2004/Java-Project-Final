package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.model.User;

import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;

/** Staff-account use cases. Everything here is administrator-only. */
public class TeamService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public TeamService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<User> getUsers() throws SQLException {
        access.requireAdmin();
        return repository.users();
    }

    public void addUser(String username, char[] password, String fullName, String role, String phone, double salary)
            throws SQLException, GeneralSecurityException {
        access.requireAdmin();
        repository.addUser(username, password, fullName, role, phone, salary);
    }

    public void updateUser(int id, String fullName, String role, String phone, double salary) throws SQLException {
        access.requireAdmin();
        repository.updateUser(id, fullName, role, phone, salary);
    }

    public void updatePassword(int id, char[] newPassword) throws SQLException, GeneralSecurityException {
        access.requireAdmin();
        repository.updatePassword(id, newPassword);
    }

    public void deactivateUser(int id) throws SQLException {
        access.requireAdmin();
        repository.deactivateUser(id);
    }
}
