package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Activity;
import java.sql.SQLException;
import java.util.List;

public class ActivityService {
    private final GymDataAccess repository;
    public ActivityService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.ActivityRow> getAllActivities() throws SQLException {
        return repository.activities();
    }
    public void addActivity(Activity activity) throws SQLException { repository.addActivity(activity); }
    public void updateActivity(int id, Activity activity) throws SQLException { repository.updateActivity(id, activity); }
    public void deleteActivity(int id) throws SQLException { repository.deleteActivity(id); }
}