package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository;
import com.gym.system.model.Subscription;
import java.sql.SQLException;
import java.util.List;

public class SubscriptionService {
    private final GymDataAccess repository;
    public SubscriptionService(GymDataAccess repository) { this.repository = repository; }

    public List<GymRepository.SubscriptionRow> getAllSubscriptions() throws SQLException {
        return repository.subscriptions();
    }
    public void addSubscription(Subscription subscription) throws SQLException {
        repository.addSubscription(subscription);
    }
    public void setSubscriptionStatus(int id, String status) throws SQLException {
        repository.setSubscriptionStatus(id, status);
    }
    public void confirmPayment(int id) throws SQLException {
        repository.confirmOfflineSubscriptionPayment(id);
    }
}