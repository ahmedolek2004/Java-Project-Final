package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class FinanceService {
    private final GymDataAccess repository;
    public FinanceService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.Payment> getPayments() throws SQLException {
        return repository.payments();
    }
    public List<GymRepository.ActivityDemand> getActivityDemand() throws SQLException {
        return repository.activityDemand();
    }
    public void addExpense(String description, double amount, LocalDate date) throws SQLException {
        repository.addExpense(description, amount, date);
    }
}