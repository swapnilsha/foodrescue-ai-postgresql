package com.foodrescue.controller;

import com.foodrescue.entity.*;
import com.foodrescue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMonitoringController {

    private final UserRepository userRepository;
    private final FoodDonationRepository donationRepository;
    private final DonationRequestRepository requestRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/monitoring/live")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getLiveSystemStatus() {
        long totalUsers = userRepository.count();
        long activeDonations = donationRepository.countByStatus("AVAILABLE");
        long pendingRequests = requestRepository.countByStatus(DonationRequestStatus.PENDING);

        long activeDeliveries = requestRepository.countByStatus(DonationRequestStatus.ASSIGNED)
                + requestRepository.countByStatus(DonationRequestStatus.PICKUP_IN_PROGRESS)
                + requestRepository.countByStatus(DonationRequestStatus.COLLECTED)
                + requestRepository.countByStatus(DonationRequestStatus.RECEIVED);

        long completedDeliveries = requestRepository.countByStatus(DonationRequestStatus.DELIVERED);
        long expiredDonations = donationRepository.countByStatus("EXPIRED");
        long totalNotifications = notificationRepository.count();

        // Get server uptime in seconds
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSec = uptimeMs / 1000;

        // Recent activity: last 5 requests or audit logs
        List<AuditLog> recentLogs = auditLogRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(5)
                .collect(Collectors.toList());

        List<Map<String, Object>> recentActivity = recentLogs.stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("actor", log.getActor());
            map.put("action", log.getAction());
            map.put("severity", log.getSeverity());
            map.put("description", log.getDescription());
            map.put("createdAt", log.getCreatedAt().toString());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("totalUsers", totalUsers);
        response.put("activeDonations", activeDonations);
        response.put("pendingRequests", pendingRequests);
        response.put("activeDeliveries", activeDeliveries);
        response.put("completedDeliveries", completedDeliveries);
        response.put("expiredDonations", expiredDonations);
        response.put("notificationCount", totalNotifications);
        response.put("serverUptimeSeconds", uptimeSec);
        response.put("recentActivity", recentActivity);

        return ResponseEntity.ok(response);
    }
}
