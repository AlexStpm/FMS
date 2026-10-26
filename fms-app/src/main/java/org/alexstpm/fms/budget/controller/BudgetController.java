package org.alexstpm.fms.budget.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.budget.dto.BudgetExecutionResponse;
import org.alexstpm.fms.budget.dto.BudgetResponse;
import org.alexstpm.fms.budget.dto.CreateBudgetRequest;
import org.alexstpm.fms.budget.service.BudgetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/budgets")
@RestController
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService budgetService;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BudgetResponse get(@PathVariable Long id) {
        return budgetService.getBudget(id);
    }

    @GetMapping("/{id}/execution")
    public BudgetExecutionResponse getExecution(@PathVariable Long id){
        return budgetService.getExecution(id);
    }

    @GetMapping
    public Page<BudgetResponse> list(
            @PageableDefault(size = 20, sort = "startOfPeriod", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return budgetService.listBudgets(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetExecutionResponse create(@Valid @RequestBody CreateBudgetRequest request) {
        return budgetService.createBudget(request);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BudgetExecutionResponse update(@PathVariable Long id,
                                          @Valid @RequestBody CreateBudgetRequest request) {
        return budgetService.updateBudget(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        budgetService.deleteBudget(id);
    }
}
