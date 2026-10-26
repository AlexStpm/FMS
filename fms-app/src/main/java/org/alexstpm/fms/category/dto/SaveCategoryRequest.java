package org.alexstpm.fms.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.alexstpm.fms.category.model.CategoryType;

public record SaveCategoryRequest(
        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        CategoryType type,

        Long parentId
) {
}