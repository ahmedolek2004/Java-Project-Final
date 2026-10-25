package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import java.sql.SQLException;
import java.util.List;

public class ScheduleService {
    private final GymDataAccess repository;
    public ScheduleService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.ScheduledClass> getClasses() throws SQLException {
        return repository.scheduledClasses();
    }
    public void addClass(String title, int activityId, Integer coachId, int dayOfWeek, String start, String end, int capacity) throws SQLException {
        repository.addScheduledClass(title, activityId, coachId, dayOfWeek, start, end, capacity);
    }
    public void updateClass(int id, String title, int activityId, Integer coachId, int dayOfWeek, String start, String end, int capacity) throws SQLException {
        repository.updateScheduledClass(id, title, activityId, coachId, dayOfWeek, start, end, capacity);
    }
    public void deleteClass(int id) throws SQLException {
        repository.deleteScheduledClass(id);
    }
}