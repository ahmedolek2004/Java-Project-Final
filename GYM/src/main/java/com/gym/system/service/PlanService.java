package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import java.sql.SQLException;
import java.util.List;

public class PlanService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public PlanService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<GymRepository.PlanRow> getAllPlans() throws SQLException {
        access.requireSignedIn();
        return repository.plans();
    }

    public void addPlan(String name, String planType, int durationOrSessions, double price) throws SQLException {
        access.requireAdmin();
        validate(name, planType, durationOrSessions, price);
        repository.addPlan(name, planType, durationOrSessions, price);
    }

    public void updatePlan(int id, String name, String planType, int durationOrSessions, double price)
            throws SQLException {
        access.requireAdmin();
        validate(name, planType, durationOrSessions, price);
        repository.updatePlan(id, name, planType, durationOrSessions, price);
    }

    public void deletePlan(int id) throws SQLException {
        access.requireAdmin();
        repository.deletePlan(id);
    }

    private static void validate(String name, String planType, int durationOrSessions, double price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plan name is required.");
        }
        if (!"TIME_BASED".equals(planType) && !"SESSION_BASED".equals(planType)) {
            throw new IllegalArgumentException("Unknown plan type.");
        }
        if (durationOrSessions <= 0) {
            throw new IllegalArgumentException("Duration or sessions must be positive.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
    }
}
