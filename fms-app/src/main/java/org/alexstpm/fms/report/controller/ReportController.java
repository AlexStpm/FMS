package org.alexstpm.fms.report.controller;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.report.dto.BudgetExecutionReportResponse;
import org.alexstpm.fms.report.dto.CashFlowReportResponse;
import org.alexstpm.fms.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/cash-flow")
    public CashFlowReportResponse getCashFlow(
            @RequestParam Long departmentId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return reportService.getCashFlow(
                departmentId, startDate, endDate);
    }

    @GetMapping("/budget-execution")
    public BudgetExecutionReportResponse getExecutionReport(
            @RequestParam Long departmentId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return reportService.getExecutionReport(departmentId, startDate, endDate);
    }
}