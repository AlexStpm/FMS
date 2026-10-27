package org.alexstpm.fms.category.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.budget.repository.BudgetRepository;
import org.alexstpm.fms.category.dto.CategoryResponse;
import org.alexstpm.fms.category.dto.SaveCategoryRequest;
import org.alexstpm.fms.category.model.Category;
import org.alexstpm.fms.category.repository.CategoryRepository;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Page<CategoryResponse> getCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public CategoryResponse getById(Long id) {
        return toResponse(findCategory(id));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse createCategory(SaveCategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse updateCategory(Long id, SaveCategoryRequest request) {
        Category category = findCategory(id);
        boolean typeIsChanging = category.getType() != request.type();
        if (typeIsChanging) {
            boolean hasChildren = categoryRepository.existsByParent_Id(id);
            boolean usedByTransactions = transactionRepository.existsByCategory_Id(id);
            boolean usedByBudgets = budgetRepository.existsByCategory_Id(id);
            if (hasChildren || usedByTransactions || usedByBudgets) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Category type cannot be changed while it is in use");
            }
        }
        apply(category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        boolean hasChildren = categoryRepository.existsByParent_Id(id);
        boolean usedByTransactions = transactionRepository.existsByCategory_Id(id);
        boolean usedByBudgets = budgetRepository.existsByCategory_Id(id);
        if (hasChildren || usedByTransactions || usedByBudgets) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Category is used by other records and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private void apply(Category category, SaveCategoryRequest request) {
        Category parent = null;
        if (request.parentId() != null) {
            parent = findCategory(request.parentId());
            if (parent.getType() != request.type()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Parent category must have the same type");
            }
            Category ancestor = parent;
            while (ancestor != null) {
                if (ancestor.getId().equals(category.getId())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Category hierarchy cannot contain a cycle");
                }
                ancestor = ancestor.getParent();
            }
        }
        category.setName(request.name().trim());
        category.setType(request.type());
        category.setParent(parent);
    }

    private Category findCategory(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Category with id = " + id + " not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getParent() == null ? null : category.getParent().getId());
    }
}