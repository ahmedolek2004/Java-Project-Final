package com.gym.system.dto;

import java.time.LocalDate;

public record SubscriptionRow(
    int id, 
    int memberId, 
    LocalDate startDate,
    LocalDate endDate, 
    String status
) { }