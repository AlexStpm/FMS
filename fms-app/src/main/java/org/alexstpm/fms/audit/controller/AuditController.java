package org.alexstpm.fms.audit.controller;

import lombok.RequiredArgsConstructor;
import org.alexstpm.fms.audit.dto.AuditLogResponse;
import org.alexstpm.fms.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audits")
@RequiredArgsConstructor
public class AuditController {
    private final AuditService auditService;

    @GetMapping
    public Page<AuditLogResponse> list(
            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        return auditService.list(pageable);
    }
}
