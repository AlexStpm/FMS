package org.alexstpm.fms.report.service;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.budget.dto.BudgetExecutionResponse;
import org.alexstpm.fms.budget.model.Budget;
import org.alexstpm.fms.budget.repository.BudgetRepository;
import org.alexstpm.fms.department.repository.DepartmentRepository;
import org.alexstpm.fms.report.dto.BudgetExecutionReportResponse;
import org.alexstpm.fms.report.dto.CashFlowReportResponse;
import org.alexstpm.fms.security.service.AccessService;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.alexstpm.fms.user.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final TransactionRepository transactionRepository;
    private final DepartmentRepository departmentRepository;
    private final BudgetRepository budgetRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER','AUDITOR')")
    public CashFlowReportResponse getCashFlow(
            Long departmentId,
            LocalDate startDate,
            LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date must not be after end date");
        }
        var department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department with id = " + departmentId + " not found"));
        User user = accessService.currentUser();
        accessService.requireDepartmentRead(user, departmentId);
        var categories = transactionRepository.getCashFlowByDepartmentAndPeriod(
                departmentId, startDate, endDate);
        return new CashFlowReportResponse(
                department.getId(),
                startDate,
                endDate,
                categories);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_MANAGER','AUDITOR')")
    public BudgetExecutionReportResponse getExecutionReport(
            Long departmentId,
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date must not be after end date");
        }

        departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department with id = " + departmentId + " not found"));

        User user = accessService.currentUser();
        accessService.requireDepartmentRead(user, departmentId);
        List<BudgetExecutionResponse> budgets = budgetRepository
                .findAllByDepartment_IdAndStartOfPeriodAndEndOfPeriod(
                        departmentId, startDate, endDate)
                .stream()
                .map(this::toExecutionResponse)
                .toList();
        return new BudgetExecutionReportResponse(
                departmentId, startDate, endDate, budgets);
    }

    private BudgetExecutionResponse toExecutionResponse(Budget budget) {
        BigDecimal spentAmount = transactionRepository.sumExpenses(
                budget.getDepartment().getId(),
                budget.getCategory().getId(),
                budget.getStartOfPeriod(),
                budget.getEndOfPeriod());
        if (spentAmount == null) {
            spentAmount = BigDecimal.ZERO;
        }
        return new BudgetExecutionResponse(
                budget.getId(),
                budget.getDepartment().getId(),
                budget.getCategory().getId(),
                budget.getAmount(),
                spentAmount,
                budget.getAmount().subtract(spentAmount),
                budget.getStartOfPeriod(),
                budget.getEndOfPeriod());
    }
}