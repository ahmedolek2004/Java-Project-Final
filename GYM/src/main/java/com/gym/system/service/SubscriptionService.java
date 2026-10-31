package com.gym.system.service;

import com.gym.system.dao.GymDataAccess;
import com.gym.system.dao.GymRepository.SubscriptionRow;
import com.gym.system.model.Subscription;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public class SubscriptionService {
    private static final Set<String> TOGGLE_STATUSES = Set.of("ACTIVE", "FROZEN");

    private final GymDataAccess repository;
    private final AccessControl access;

    public SubscriptionService(GymDataAccess repository, AccessControl access) {
        this.repository = repository;
        this.access = access;
    }

    public List<SubscriptionRow> getAllSubscriptions() throws SQLException {
        access.requireSignedIn();
        return repository.subscriptions();
    }

    public void addSubscription(Subscription subscription) throws SQLException {
        access.requireSignedIn();
        repository.addSubscription(subscription);
    }

    /** Freezes or reactivates a subscription; other statuses are managed by the system itself. */
    public void setSubscriptionStatus(int id, String status) throws SQLException {
        access.requireSignedIn();
        if (!TOGGLE_STATUSES.contains(status)) {
            throw new IllegalArgumentException("A subscription can only be set to ACTIVE or FROZEN.");
        }
        repository.setSubscriptionStatus(id, status);
    }

    public void confirmPayment(int id) throws SQLException {
        access.requireSignedIn();
        repository.confirmOfflineSubscriptionPayment(id);
    }
}
