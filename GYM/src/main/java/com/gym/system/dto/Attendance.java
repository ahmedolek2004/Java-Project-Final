package com.gym.system.dto;

import java.time.LocalDateTime;

public record Attendance(int id, int memberId, LocalDateTime checkInTime) { }