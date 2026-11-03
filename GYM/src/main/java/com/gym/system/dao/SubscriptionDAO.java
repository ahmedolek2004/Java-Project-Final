package com.gym.system.dao;

import com.gym.system.model.Subscription;

import java.sql.SQLException;
import java.util.List;

public interface SubscriptionDAO {
    void addSubscription(Subscription subscription) throws SQLException;
    void addSubscription(Subscription subscription, boolean paidNow) throws SQLException;
    void changeSubscriptionClass(int subscriptionId, Integer classId) throws SQLException;
    List<GymRepository.SubscriptionRow> subscriptions() throws SQLException;
    void setSubscriptionStatus(int subscriptionId, String status) throws SQLException;
    GymRepository.SubscriptionInvoice subscriptionInvoice(int subscriptionId) throws SQLException;
}
