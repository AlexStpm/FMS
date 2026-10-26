package org.alexstpm.fms.department.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alexstpm.fms.user.model.User;

@Entity
@Table(name = "department_memberships", uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "department_id"}))
@NoArgsConstructor
@Getter
@Setter
public class DepartmentMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
