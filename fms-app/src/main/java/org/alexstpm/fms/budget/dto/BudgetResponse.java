package org.alexstpm.fms.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        Long budgetId,
        Long departmentId,
        Long categoryId,
        BigDecimal amount,
        LocalDate startOfPeriod,
        LocalDate endOfPeriod
) {
}