package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Coach;
import java.sql.SQLException;
import java.util.List;

public class CoachService {
    private final GymDataAccess repository;
    public CoachService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.CoachRow> getAllCoaches() throws SQLException {
        return repository.coaches();
    }
    public void addCoach(Coach coach) throws SQLException { repository.addCoach(coach); }
    public void updateCoach(int id, Coach coach) throws SQLException { repository.updateCoach(id, coach); }
    public void deleteCoach(int id) throws SQLException { repository.deleteCoach(id); }
}