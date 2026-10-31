package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.ActivityRow;
import com.gym.system.model.Activity;

import java.sql.SQLException;
import java.util.List;

public class ActivityService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public ActivityService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<ActivityRow> getAllActivities() throws SQLException {
        access.requireSignedIn();
        return repository.activities();
    }

    public void addActivity(Activity activity) throws SQLException {
        access.requireSignedIn();
        repository.addActivity(activity);
    }

    public void updateActivity(int id, Activity activity) throws SQLException {
        access.requireSignedIn();
        repository.updateActivity(id, activity);
    }

    public void deleteActivity(int id) throws SQLException {
        access.requireSignedIn();
        repository.deleteActivity(id);
    }
}
