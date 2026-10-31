package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.CoachRow;
import com.gym.system.model.Coach;

import java.sql.SQLException;
import java.util.List;

public class CoachService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public CoachService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<CoachRow> getAllCoaches() throws SQLException {
        access.requireSignedIn();
        return repository.coaches();
    }

    public void addCoach(Coach coach) throws SQLException {
        access.requireSignedIn();
        repository.addCoach(coach);
    }

    public void updateCoach(int id, Coach coach) throws SQLException {
        access.requireSignedIn();
        repository.updateCoach(id, coach);
    }

    public void deleteCoach(int id) throws SQLException {
        access.requireSignedIn();
        repository.deleteCoach(id);
    }
}
