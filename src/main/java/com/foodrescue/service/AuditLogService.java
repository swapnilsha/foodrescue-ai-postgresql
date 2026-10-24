package com.foodrescue.service;

import com.foodrescue.entity.AuditLog;
import java.util.List;

public interface AuditLogService {
    void log(String actor, String action, String severity, String description);
    List<AuditLog> getAllLogs();
}
