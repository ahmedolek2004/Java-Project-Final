package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.ScheduledClass;

import java.sql.SQLException;
import java.util.List;

public class ScheduleService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public ScheduleService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<ScheduledClass> getClasses() throws SQLException {
        access.requireSignedIn();
        return repository.scheduledClasses();
    }

    public void addClass(String title, int activityId, Integer coachId, int dayOfWeek, String start, String end,
            int capacity) throws SQLException {
        access.requireSignedIn();
        repository.addScheduledClass(title, activityId, coachId, dayOfWeek, start, end, capacity);
    }

    public void updateClass(int id, String title, int activityId, Integer coachId, int dayOfWeek, String start,
            String end, int capacity) throws SQLException {
        access.requireSignedIn();
        repository.updateScheduledClass(id, title, activityId, coachId, dayOfWeek, start, end, capacity);
    }

    public void deleteClass(int id) throws SQLException {
        access.requireSignedIn();
        repository.deleteScheduledClass(id);
    }
}
