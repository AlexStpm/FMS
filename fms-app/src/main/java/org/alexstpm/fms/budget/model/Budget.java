package org.alexstpm.fms.budget.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alexstpm.fms.category.model.Category;
import org.alexstpm.fms.department.model.Department;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"department_id", "category_id", "start_of_period", "end_of_period"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "start_of_period", nullable = false)
    private LocalDate startOfPeriod;

    @Column(name = "end_of_period", nullable = false)
    private LocalDate endOfPeriod;

}
