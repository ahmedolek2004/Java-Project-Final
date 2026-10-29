package com.gym.system.dto;

import java.time.YearMonth;
import java.util.Map;

public record Payroll(YearMonth month, Map<Integer, Double> salaries) { }