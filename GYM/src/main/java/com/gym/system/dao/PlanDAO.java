package com.gym.system.dao;

import java.sql.SQLException;
import java.util.List;

public interface PlanDAO {
    List<GymRepository.PlanRow> plans() throws SQLException;
    void addPlan(String name, String planType, int durationOrSessions, double price) throws SQLException;
    void updatePlan(int id, String name, String planType, int durationOrSessions, double price) throws SQLException;
    void deletePlan(int id) throws SQLException;
}
