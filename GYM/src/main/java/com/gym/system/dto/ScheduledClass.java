package com.gym.system.dto;

public record ScheduledClass(
    int id, 
    String title, 
    int activityId,
    Integer coachId, 
    int dayOfWeek,
    String startTime, 
    String endTime, 
    int capacity
) { }