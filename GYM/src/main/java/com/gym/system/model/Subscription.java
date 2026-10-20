package com.gym.system.model;

import java.time.LocalDate;

public final class Subscription {
    public enum PlanType { TIME_BASED, SESSION_BASED }
    public enum Status { ACTIVE, EXPIRED, FROZEN }

    private int id;
    private int memberId;
    private Integer activityId;
    private String planName;
    private PlanType planType;
    private Integer durationMonths;
    private Integer totalSessions;
    private Integer remainingSessions;
    private LocalDate startDate;
    private LocalDate endDate;
    private double price;
    private double discountApplied;
    private Status status;

    public Subscription(int id, int memberId, Integer activityId, String planName, PlanType planType,
                        Integer durationMonths, Integer totalSessions, Integer remainingSessions,
                        LocalDate startDate, LocalDate endDate, double price, double discountApplied,
                        Status status) {
        this.id = id;
        this.memberId = memberId;
        this.activityId = activityId;
        setPlanName(planName);
        setPlanType(planType);
        setDurationMonths(durationMonths);
        setTotalSessions(totalSessions);
        setRemainingSessions(remainingSessions);
        setStartDate(startDate);
        setEndDate(endDate);
        setPrice(price);
        setDiscountApplied(discountApplied);
        setStatus(status);
    }

    public int getId() { return id; }
    public int getMemberId() { return memberId; }
    public Integer getActivityId() { return activityId; }
    public String getPlanName() { return planName; }
    public PlanType getPlanType() { return planType; }
    public Integer getDurationMonths() { return durationMonths; }
    public Integer getTotalSessions() { return totalSessions; }
    public Integer getRemainingSessions() { return remainingSessions; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getPrice() { return price; }
    public double getDiscountApplied() { return discountApplied; }
    public Status getStatus() { return status; }
    public void setId(int id) { this.id = id; }
    public void setMemberId(int memberId) { this.memberId = memberId; }
    public void setActivityId(Integer activityId) { this.activityId = activityId; }
    public void setPlanName(String planName) {
        if (planName == null || planName.isBlank()) throw new IllegalArgumentException("Plan name is required.");
        this.planName = planName.trim();
    }
    public void setPlanType(PlanType planType) {
        if (planType == null) throw new IllegalArgumentException("Plan type is required.");
        this.planType = planType;
    }
    public void setDurationMonths(Integer durationMonths) {
        if (durationMonths != null && durationMonths <= 0) throw new IllegalArgumentException("Duration must be positive.");
        this.durationMonths = durationMonths;
    }
    public void setTotalSessions(Integer totalSessions) {
        if (totalSessions != null && totalSessions <= 0) throw new IllegalArgumentException("Sessions must be positive.");
        this.totalSessions = totalSessions;
    }
    public void setRemainingSessions(Integer remainingSessions) {
        if (remainingSessions != null && remainingSessions < 0) throw new IllegalArgumentException("Remaining sessions cannot be negative.");
        this.remainingSessions = remainingSessions;
    }
    public void setStartDate(LocalDate startDate) {
        if (startDate == null) throw new IllegalArgumentException("Start date is required.");
        this.startDate = startDate;
    }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setPrice(double price) {
        if (!Double.isFinite(price) || price < 0) throw new IllegalArgumentException("Price cannot be negative.");
        this.price = price;
    }
    public void setDiscountApplied(double discountApplied) {
        if (!Double.isFinite(discountApplied) || discountApplied < 0 || discountApplied > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100.");
        }
        this.discountApplied = discountApplied;
    }
    public void setStatus(Status status) {
        if (status == null) throw new IllegalArgumentException("Status is required.");
        this.status = status;
    }
}
