package org.alexstpm.fms.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetExecutionResponse(
        Long budgetId,
        Long departmentId,
        Long categoryId,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        LocalDate startOfPeriod,
        LocalDate endOfPeriod
) {
}

