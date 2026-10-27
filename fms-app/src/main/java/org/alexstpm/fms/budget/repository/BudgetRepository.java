package org.alexstpm.fms.budget.repository;

import org.alexstpm.fms.budget.model.Budget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    boolean existsByCategory_Id(Long categoryId);

    boolean existsByDepartment_Id(Long departmentId);

    boolean existsByDepartment_IdAndCategory_IdAndStartOfPeriodLessThanEqualAndEndOfPeriodGreaterThanEqual(
            Long departmentId,
            Long categoryId,
            LocalDate requestedEnd,
            LocalDate requestedStart
    );

    boolean existsByDepartment_IdAndCategory_IdAndStartOfPeriodLessThanEqualAndEndOfPeriodGreaterThanEqualAndIdNot(
            Long departmentId,
            Long categoryId,
            LocalDate requestedEnd,
            LocalDate requestedStart,
            Long excludedId
    );

    List<Budget> findAllByDepartment_IdAndStartOfPeriodAndEndOfPeriod(
            Long departmentId,
            LocalDate startDate,
            LocalDate endDate
    );

    Page<Budget> findAllByDepartment_IdIn(Collection<Long> departmentIds, Pageable pageable);
}
