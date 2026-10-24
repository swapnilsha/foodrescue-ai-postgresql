package com.foodrescue.service.impl;

import com.foodrescue.entity.AuditLog;
import com.foodrescue.repository.AuditLogRepository;
import com.foodrescue.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(String actor, String action, String severity, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(action)
                    .severity(severity)
                    .description(description)
                    .build();
            auditLogRepository.save(auditLog);
            log.info("[AUDIT] Actor: {}, Action: {}, Severity: {}, Desc: {}", actor, action, severity, description);
        } catch (Exception e) {
            log.error("Failed to write audit log: {}", e.getMessage());
        }
    }

    @Override
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
