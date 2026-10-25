package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

public class PayrollService {
    private final GymDataAccess repository;
    public PayrollService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.Payroll> getPayroll(YearMonth month) throws SQLException {
        return repository.payroll(month);
    }
    public void paySalary(String employeeType, int employeeId, YearMonth month) throws SQLException {
        repository.paySalary(employeeType, employeeId, month);
    }
}