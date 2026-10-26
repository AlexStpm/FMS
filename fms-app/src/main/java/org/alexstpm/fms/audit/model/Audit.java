package org.alexstpm.fms.audit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alexstpm.fms.user.model.User;

import java.time.Instant;

@Entity
@Table(name = "audits")
@NoArgsConstructor
@Getter
@Setter
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private Action action;

    @Column(name = "entity_type", nullable = false, length = 20)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "details", nullable = false)
    private String details;


}
