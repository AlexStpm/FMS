package org.alexstpm.fms.report.dto;

import org.alexstpm.fms.budget.dto.BudgetExecutionResponse;

import java.time.LocalDate;
import java.util.List;

public record BudgetExecutionReportResponse(
        Long departmentId,
        LocalDate startDate,
        LocalDate endDate,
        List<BudgetExecutionResponse> budgets
) {
}