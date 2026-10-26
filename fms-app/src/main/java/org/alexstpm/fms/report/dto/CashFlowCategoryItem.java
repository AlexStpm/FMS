package org.alexstpm.fms.report.dto;

import org.alexstpm.fms.category.model.CategoryType;

import java.math.BigDecimal;

public record CashFlowCategoryItem(
        Long categoryId,
        String categoryName,
        CategoryType categoryType,
        String currency,
        BigDecimal amount
) {
}
