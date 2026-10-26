package org.alexstpm.fms.audit.dto;

import org.alexstpm.fms.audit.model.Action;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorEmail,
        Action action,
        String entityType,
        Long entityId,
        Instant occurredAt,
        String details
) {
}
