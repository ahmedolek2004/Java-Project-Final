package com.gym.system.dao;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Payment, expense and dashboard operations. */
public interface FinanceDAO {

    List<GymRepository.Payment> payments() throws SQLException;

    void confirmOfflineSubscriptionPayment(int subscriptionId) throws SQLException;

    void addExpense(String description, double amount, LocalDate date) throws SQLException;

    GymRepository.Dashboard dashboard() throws SQLException;
}
