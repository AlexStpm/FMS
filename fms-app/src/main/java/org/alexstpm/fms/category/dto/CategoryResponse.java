package org.alexstpm.fms.category.dto;

import org.alexstpm.fms.category.model.CategoryType;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        Long parentId
) {
}