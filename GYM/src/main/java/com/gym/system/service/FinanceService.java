package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.ActivityDemand;
import com.gym.system.dao.GymRepository.Payment;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Financial use cases. Payment history and expenses are administrator-only. */
public class FinanceService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public FinanceService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<Payment> getPayments() throws SQLException {
        access.requireAdmin();
        return repository.payments();
    }

    public List<ActivityDemand> getActivityDemand() throws SQLException {
        access.requireSignedIn();
        return repository.activityDemand();
    }

    public void addExpense(String description, double amount, LocalDate date) throws SQLException {
        access.requireAdmin();
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("An expense description is required.");
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("The expense amount must be greater than zero.");
        }
        if (date == null) {
            throw new IllegalArgumentException("An expense date is required.");
        }
        repository.addExpense(description, amount, date);
    }
}
