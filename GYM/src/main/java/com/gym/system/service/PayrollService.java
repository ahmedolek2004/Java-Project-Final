package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.Payroll;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;

/** Payroll use cases. Everything here is administrator-only. */
public class PayrollService {
    private final GymDataAccess repository;
    private final AccessControl access;

    public PayrollService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<Payroll> getPayroll(YearMonth month) throws SQLException {
        access.requireAdmin();
        return repository.payroll(month);
    }

    public void paySalary(String employeeType, int employeeId, YearMonth month) throws SQLException {
        access.requireAdmin();
        repository.paySalary(employeeType, employeeId, month);
    }
}
