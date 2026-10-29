package com.gym.system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Payment(int id, int memberId, BigDecimal amount, LocalDate date) { }