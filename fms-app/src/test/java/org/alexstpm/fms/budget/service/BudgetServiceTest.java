package org.alexstpm.fms.budget.service;

import org.alexstpm.fms.budget.dto.BudgetExecutionResponse;
import org.alexstpm.fms.budget.model.Budget;
import org.alexstpm.fms.budget.repository.BudgetRepository;
import org.alexstpm.fms.category.model.Category;
import org.alexstpm.fms.department.model.Department;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.department.repository.DepartmentRepository;
import org.alexstpm.fms.audit.repository.AuditRepository;
import org.alexstpm.fms.category.repository.CategoryRepository;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.alexstpm.fms.user.model.Role;
import org.alexstpm.fms.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DepartmentMembershipRepository membershipRepository;
    @Mock
    private AuditRepository auditRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private AccessService accessService;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void calculatesSpentAndRemainingAmounts() {
        // given
        Budget budget = budget();
        User manager = User.builder().id(7L).email("manager@example.test")
                .role(Role.FINANCE_MANAGER).enabled(true).build();

        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));
        when(accessService.currentUser()).thenReturn(manager);
        when(transactionRepository.sumExpenses(
                3L, 9L, budget.getStartOfPeriod(), budget.getEndOfPeriod()))
                .thenReturn(new BigDecimal("35.25"));

        // when
        BudgetExecutionResponse result = budgetService.getExecution(5L);

        // then
        assertEquals(new BigDecimal("100.00"), result.budgetAmount());
        assertEquals(new BigDecimal("35.25"), result.spentAmount());
        assertEquals(new BigDecimal("64.75"), result.remainingAmount());
    }

    @Test
    void treatsNoExpensesAsZero() {
        // given
        Budget budget = budget();
        User manager = User.builder().id(7L).email("manager@example.test")
                .role(Role.FINANCE_MANAGER).enabled(true).build();

        when(budgetRepository.findById(5L)).thenReturn(Optional.of(budget));
        when(accessService.currentUser()).thenReturn(manager);
        when(transactionRepository.sumExpenses(
                3L, 9L, budget.getStartOfPeriod(), budget.getEndOfPeriod()))
                .thenReturn(null);

        // when
        BudgetExecutionResponse result = budgetService.getExecution(5L);

        // then
        assertEquals(BigDecimal.ZERO, result.spentAmount());
        assertEquals(new BigDecimal("100.00"), result.remainingAmount());
    }

    private static Budget budget() {
        Department department = new Department();
        department.setId(3L);
        Category category = new Category();
        category.setId(9L);

        Budget budget = new Budget();
        budget.setId(5L);
        budget.setDepartment(department);
        budget.setCategory(category);
        budget.setAmount(new BigDecimal("100.00"));
        budget.setStartOfPeriod(LocalDate.of(2026, 10, 1));
        budget.setEndOfPeriod(LocalDate.of(2026, 10, 31));
        return budget;
    }
}
