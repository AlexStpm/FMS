package org.alexstpm.fms.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBudgetRequest(
        @NotNull Long departmentId,
        @NotNull Long categoryId,

        @NotNull
        @DecimalMin("0.0001")
        @Digits(integer = 15, fraction = 4)
        BigDecimal amount,

        @NotNull LocalDate startOfPeriod,
        @NotNull LocalDate endOfPeriod
) {}
