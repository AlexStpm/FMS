package org.alexstpm.fms.transaction.repository;

import jakarta.persistence.LockModeType;
import org.alexstpm.fms.report.dto.CashFlowCategoryItem;
import org.alexstpm.fms.transaction.model.Transaction;
import org.alexstpm.fms.transaction.model.TransactionStatus;
import org.alexstpm.fms.transaction.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByReversalOf_Id(Long transactionId);

    boolean existsByCategory_Id(Long categoryId);

    boolean existsByCounterparty_Id(Long counterpartyId);

    @Query("""
            select case when count(t) > 0 then true else false end
            from Transaction t
            where t.accountFrom.id = :accountId
               or t.accountTo.id = :accountId
            """)
    boolean existsByAccountId(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select t from Transaction t
            left join t.accountFrom fromAccount
            left join fromAccount.department fromDepartment
            left join t.accountTo toAccount
            left join toAccount.department toDepartment
            where (fromAccount is not null and fromDepartment.id in :departmentIds)
              or (toAccount is not null and toDepartment.id in :departmentIds)
            """)
    Page<Transaction> findVisibleToDepartments(
            @Param("departmentIds") Set<Long> departmentIds,
            Pageable pageable);

    @Query("""
            select sum(t.amount)
            from Transaction t
            where t.type = "EXPENSE"
              and t.status = "CONFIRMED"
              and t.accountFrom.department.id = :departmentId
              and t.category.id = :categoryId
              and t.date between :startDate and :endDate
            """)
    BigDecimal sumExpenses(
            @Param("departmentId") Long departmentId,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select sum(
                case
                    when accountTo.id = :accountId then t.amount
                    else -t.amount
                end
            )
            from Transaction t
            left join t.accountFrom accountFrom
            left join t.accountTo accountTo
            where (t.status = "CONFIRMED" or t.status = "REVERSED")
              and (accountFrom.id = :accountId or accountTo.id = :accountId)
            """)
    BigDecimal calculateAccountBalance(
            @Param("accountId") Long accountId
    );

    @Query("""
        select t from Transaction t
        left join t.accountFrom fromAccount
        left join fromAccount.department fromDepartment
        left join t.accountTo toAccount
        left join toAccount.department toDepartment
        where t.id = :transactionId
          and (
                (fromAccount is not null
                    and fromDepartment.id in :departmentIds)
                or
                (toAccount is not null
                    and toDepartment.id in :departmentIds)
          )
        """)
    Optional<Transaction> findVisibleByIdToDepartments(
            @Param("transactionId") Long transactionId,
            @Param("departmentIds") Set<Long> departmentIds
    );

    @Query("""
            select new org.alexstpm.fms.report.dto.CashFlowCategoryItem(
                c.id,
                c.name,
                c.type,
                t.currency,
                sum(t.amount)
            )
                from Transaction t
                join t.category c
                left join t.accountFrom fromAccount
                left join fromAccount.department fromDepartment
                left join t.accountTo toAccount
                left join toAccount.department toDepartment
                where t.status = "CONFIRMED"
              and t.date between :startDate and :endDate
              and (
                    (t.type = "EXPENSE"
                        and fromDepartment.id = :departmentId)
                    or
                    (t.type = "INCOME"
                        and toDepartment.id = :departmentId)
              )
            group by c.id, c.name, c.type, t.currency
            order by c.type, c.name, t.currency
            """)
    List<CashFlowCategoryItem> getCashFlowByDepartmentAndPeriod(
            @Param("departmentId") Long departmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
