package org.alexstpm.fms.budget.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.audit.model.Action;
import org.alexstpm.fms.audit.model.Audit;
import org.alexstpm.fms.audit.repository.AuditRepository;
import org.alexstpm.fms.budget.dto.BudgetExecutionResponse;
import org.alexstpm.fms.budget.dto.BudgetResponse;
import org.alexstpm.fms.budget.dto.CreateBudgetRequest;
import org.alexstpm.fms.budget.model.Budget;
import org.alexstpm.fms.budget.repository.BudgetRepository;
import org.alexstpm.fms.category.model.Category;
import org.alexstpm.fms.category.model.CategoryType;
import org.alexstpm.fms.category.repository.CategoryRepository;
import org.alexstpm.fms.department.model.Department;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.department.repository.DepartmentRepository;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.alexstpm.fms.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final AuditRepository auditRepository;
    private final BudgetRepository budgetRepository;
    private final AccessService accessService;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER')")
    public BudgetExecutionResponse createBudget(CreateBudgetRequest request) {
        if (request.startOfPeriod().isAfter(request.endOfPeriod())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start of period is after end of period");
        }
        Category category = findCategory(request.categoryId());
        Department department = findDepartment(request.departmentId());
        User author = accessService.currentUser();
        if (!category.getType().equals(CategoryType.EXPENSE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category type must be expense");
        }
        accessService.requireDepartmentRead(author,request.departmentId());
        if (budgetRepository
                .existsByDepartment_IdAndCategory_IdAndStartOfPeriodLessThanEqualAndEndOfPeriodGreaterThanEqual(
                        request.departmentId(),
                        request.categoryId(),
                        request.endOfPeriod(),
                        request.startOfPeriod())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Budget with requested parameters already exists");
        }
        BigDecimal spentAmount = transactionRepository.sumExpenses(
                request.departmentId(),
                request.categoryId(),
                request.startOfPeriod(),
                request.endOfPeriod());
        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }
        BigDecimal remainingAmount = request.amount().subtract(spentAmount);
        Budget newBudget = new Budget();
        newBudget.setDepartment(department);
        newBudget.setCategory(category);
        newBudget.setAmount(request.amount());
        newBudget.setStartOfPeriod(request.startOfPeriod());
        newBudget.setEndOfPeriod(request.endOfPeriod());
        Budget saved = budgetRepository.save(newBudget);
        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.BUDGET_CREATED);
        audit.setEntityType("BUDGET");
        audit.setEntityId(saved.getId());
        audit.setOccurredAt(Instant.now());
        audit.setDetails("Budget created");
        auditRepository.save(audit);
        return toResponse(saved, spentAmount, remainingAmount);
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(Long id) {
        Budget budget = findBudget(id);
        accessService.requireDepartmentRead(
                accessService.currentUser(), budget.getDepartment().getId());
        return toSummaryResponse(budget);
    }

    @Transactional(readOnly = true)
    public BudgetExecutionResponse getExecution(Long budgetId){
        Budget budget =findBudget(budgetId);
        User author = accessService.currentUser();
        accessService.requireDepartmentRead(author,budget.getDepartment().getId());
        BigDecimal spentAmount = transactionRepository.sumExpenses(
                budget.getDepartment().getId(),
                budget.getCategory().getId(),
                budget.getStartOfPeriod(),
                budget.getEndOfPeriod());
        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }
        BigDecimal remainingAmount = budget.getAmount().subtract(spentAmount);
        return toResponse(budget,spentAmount, remainingAmount);
    }

    @Transactional(readOnly = true)
    public Page<BudgetResponse> listBudgets(Pageable pageable) {
        User user = accessService.currentUser();
        if (accessService.isGlobalReader(user)) {
            return budgetRepository.findAll(pageable).map(this::toSummaryResponse);
        }
        Set<Long> departmentIds = membershipRepository.findAllByUser_Id(user.getId()).stream()
                .map(membership -> membership.getDepartment().getId())
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return budgetRepository.findAllByDepartment_IdIn(departmentIds, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER')")
    public BudgetExecutionResponse updateBudget(Long id, CreateBudgetRequest request) {
        Budget budget = findBudget(id);
        User author = accessService.currentUser();
        accessService.requireDepartmentRead(author, budget.getDepartment().getId());

        if (request.startOfPeriod().isAfter(request.endOfPeriod())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start of period is after end of period");
        }
        Category category = findCategory(request.categoryId());
        Department department = findDepartment(request.departmentId());
        if (!category.getType().equals(CategoryType.EXPENSE)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category type must be expense");
        }
        accessService.requireDepartmentRead(author, request.departmentId());

        if (budgetRepository
                .existsByDepartment_IdAndCategory_IdAndStartOfPeriodLessThanEqualAndEndOfPeriodGreaterThanEqualAndIdNot(
                        request.departmentId(),
                        request.categoryId(),
                        request.endOfPeriod(),
                        request.startOfPeriod(),
                        id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Budget with requested parameters already exists");
        }

        budget.setDepartment(department);
        budget.setCategory(category);
        budget.setAmount(request.amount());
        budget.setStartOfPeriod(request.startOfPeriod());
        budget.setEndOfPeriod(request.endOfPeriod());
        Budget saved = budgetRepository.save(budget);

        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.BUDGET_UPDATED);
        audit.setEntityType("BUDGET");
        audit.setEntityId(saved.getId());
        audit.setOccurredAt(Instant.now());
        audit.setDetails("Budget updated");
        auditRepository.save(audit);

        BigDecimal spentAmount = transactionRepository.sumExpenses(
                saved.getDepartment().getId(),
                saved.getCategory().getId(),
                saved.getStartOfPeriod(),
                saved.getEndOfPeriod());
        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }
        BigDecimal remainingAmount = saved.getAmount().subtract(spentAmount);
        return toResponse(saved, spentAmount, remainingAmount);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER')")
    public void deleteBudget(Long id) {
        Budget budget = findBudget(id);
        User author = accessService.currentUser();
        accessService.requireDepartmentRead(author, budget.getDepartment().getId());

        Audit audit = new Audit();
        audit.setActor(author);
        audit.setAction(Action.BUDGET_DELETED);
        audit.setEntityType("BUDGET");
        audit.setEntityId(budget.getId());
        audit.setOccurredAt(Instant.now());
        audit.setDetails("Budget deleted");
        auditRepository.save(audit);

        budgetRepository.delete(budget);
    }

    private Category findCategory(Long id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category with id = " + id + " not found"));
    }

    private Department findDepartment(Long id) {
        if (id == null) {
            return null;
        }
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department with id = " + id + " not found"));
    }

    private Budget findBudget(Long id) {
        if (id == null) {
            return null;
        }
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Budget with id = " + id + " not found"));
    }

    private BudgetExecutionResponse toResponse(Budget budget, BigDecimal expanses, BigDecimal remain) {
        return new BudgetExecutionResponse(
                budget.getId(),
                budget.getDepartment().getId(),
                budget.getCategory().getId(),
                budget.getAmount(),
                expanses,
                remain,
                budget.getStartOfPeriod(),
                budget.getEndOfPeriod()
        );
    }

    private BudgetResponse toSummaryResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getDepartment().getId(),
                budget.getCategory().getId(),
                budget.getAmount(),
                budget.getStartOfPeriod(),
                budget.getEndOfPeriod()
        );
    }
}
