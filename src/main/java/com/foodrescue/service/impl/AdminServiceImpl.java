package com.foodrescue.service.impl;

import com.foodrescue.dto.DonationResponse;
import com.foodrescue.entity.DonationRequestStatus;
import com.foodrescue.repository.DonationRequestRepository;
import com.foodrescue.repository.FoodDonationRepository;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final FoodDonationRepository donationRepository;
    private final DonationRequestRepository requestRepository;
    private final com.foodrescue.service.AuditLogService auditLogService;
    private final com.foodrescue.repository.LocationRepository locationRepository;
    private final com.foodrescue.repository.SystemSettingRepository systemSettingRepository;
    private final com.foodrescue.service.NotificationService notificationService;

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // ── User Stats ────────────────────────────────────────────────────────
        stats.put("totalUsers",         userRepository.count());
        stats.put("totalDonors",        userRepository.countByRole("ROLE_DONOR"));
        stats.put("totalNgos",          userRepository.countByRole("ROLE_NGO"));

        // ── Donation Stats ────────────────────────────────────────────────────
        stats.put("totalDonations",     donationRepository.count());
        stats.put("availableDonations", donationRepository.countByStatus("AVAILABLE"));
        stats.put("acceptedDonations",  donationRepository.countByStatus("ACCEPTED"));
        stats.put("collectedDonations", donationRepository.countByStatus("COLLECTED"));
        stats.put("expiredDonations",   donationRepository.countByStatus("EXPIRED"));

        // ── Donation Request Workflow Stats (BUG FIX: previously missing) ────
        stats.put("pendingRequests",          requestRepository.countByStatus(DonationRequestStatus.PENDING));
        stats.put("acceptedRequests",         requestRepository.countByStatus(DonationRequestStatus.ACCEPTED));
        stats.put("assignedRequests",         requestRepository.countByStatus(DonationRequestStatus.ASSIGNED));
        stats.put("pickupInProgressRequests", requestRepository.countByStatus(DonationRequestStatus.PICKUP_IN_PROGRESS));
        stats.put("collectedRequests",        requestRepository.countByStatus(DonationRequestStatus.COLLECTED));
        stats.put("receivedRequests",         requestRepository.countByStatus(DonationRequestStatus.RECEIVED));
        stats.put("deliveredRequests",        requestRepository.countByStatus(DonationRequestStatus.DELIVERED));
        stats.put("rejectedRequests",         requestRepository.countByStatus(DonationRequestStatus.REJECTED));
        stats.put("totalRequests",            requestRepository.count());

        // ── Verification ──────────────────────────────────────────────────────
        stats.put("pendingVerifications", userRepository.countByVerifiedFalseAndRoleNot("ROLE_ADMIN"));

        return stats;
    }

    @Override
    public List<DonationResponse> getAllDonations(int page, int size) {
        return donationRepository
                .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(d -> DonationResponse.builder()
                        .id(d.getId())
                        .title(d.getTitle())
                        .foodCategory(d.getFoodCategory())
                        .quantityKg(d.getQuantityKg())
                        .status(d.getStatus())
                        .expiryTime(d.getExpiryTime())
                        .createdAt(d.getCreatedAt())
                        .imagePath(d.getImagePath())
                        .donorName(d.getDonor() != null ? d.getDonor().getName() : null)
                        .donorEmail(d.getDonor() != null ? d.getDonor().getEmail() : null)
                        .locationAddress(d.getLocation() != null ? d.getLocation().getAddress() : null)
                        .acceptedByNgoName(d.getAcceptedByNgo() != null ? d.getAcceptedByNgo().getName() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getActiveMissions() {
        return requestRepository.findAll().stream()
                .filter(r -> r.getStatus() != com.foodrescue.entity.DonationRequestStatus.DELIVERED
                        && r.getStatus() != com.foodrescue.entity.DonationRequestStatus.REJECTED
                        && r.getStatus() != com.foodrescue.entity.DonationRequestStatus.CANCELLED)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", r.getId());
                    map.put("donorName", r.getDonor() != null ? r.getDonor().getName() : "Donor");
                    map.put("ngoName", r.getNgo() != null ? r.getNgo().getName() : "NGO Partner");
                    map.put("status", r.getStatus().toString());
                    map.put("title", r.getDonation() != null ? r.getDonation().getTitle() : "Surplus Food");
                    map.put("quantityKg", r.getDonation() != null ? r.getDonation().getQuantityKg() : 0.0);
                    map.put("assignedToName", r.getAssignedToName() != null ? r.getAssignedToName() : "");
                    map.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void verifyUser(Long userId, boolean verified) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setVerified(verified);
            userRepository.save(user);
            auditLogService.log("admin_workspace", "USER_VERIFIED", "INFO", "Set verification status of " + user.getEmail() + " (" + user.getRole() + ") to " + verified);
            if (verified) {
                notificationService.createNotification(user, "Your organization has been successfully verified! You can now participate in food rescues.", "SYSTEM", null);
            }
            log.info("User {} verification status set to {}", user.getEmail(), verified);
        });
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            userRepository.delete(user);
            auditLogService.log("admin_workspace", "USER_DELETED", "WARNING", "Deleted/Rejected user: " + user.getEmail());
            log.info("User {} deleted by admin", user.getEmail());
        });
    }

    @Override
    public Map<String, Object> getAnalyticsStats() {
        Map<String, Object> analytics = new HashMap<>();

        List<com.foodrescue.entity.FoodDonation> donations = donationRepository.findAll();
        List<com.foodrescue.entity.DonationRequest> requests = requestRepository.findAll();
        List<com.foodrescue.entity.User> users = userRepository.findAll();

        java.time.LocalDate now = java.time.LocalDate.now();
        int currentYear = now.getYear();

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        double[] rescued = new double[12];
        double[] target = new double[12];
        double targetBaseline = 100.0; // 100kg baseline target

        for (com.foodrescue.entity.DonationRequest r : requests) {
            if (r.getStatus() == com.foodrescue.entity.DonationRequestStatus.DELIVERED && r.getDeliveredAt() != null) {
                java.time.LocalDateTime dt = r.getDeliveredAt();
                if (dt.getYear() == currentYear) {
                    int monthIdx = dt.getMonthValue() - 1;
                    double qty = r.getReceivedQuantityKg() != null ? r.getReceivedQuantityKg().doubleValue() : 
                                 ((r.getDonation() != null && r.getDonation().getQuantityKg() != null) ? r.getDonation().getQuantityKg().doubleValue() : 0.0);
                    rescued[monthIdx] += qty;
                }
            }
        }

        for (int i = 0; i < 12; i++) {
            target[i] = targetBaseline * Math.pow(1.05, i);
        }

        analytics.put("months", months);
        analytics.put("rescuedQty", rescued);
        analytics.put("targetQty", target);

        Map<String, Long> densityMap = new HashMap<>();
        for (com.foodrescue.entity.User u : users) {
            if ("ROLE_NGO".equals(u.getRole())) {
                com.foodrescue.entity.Location loc = locationRepository.findByUser(u).orElse(null);
                String city = "Other";
                if (loc != null && loc.getAddress() != null) {
                    String addr = loc.getAddress().trim();
                    int commaIdx = addr.indexOf(',');
                    if (commaIdx > 0) {
                        city = addr.substring(0, commaIdx).trim();
                    } else {
                        city = addr;
                    }
                    if (city.length() > 20) {
                        city = city.substring(0, 20);
                    }
                }
                densityMap.put(city, densityMap.getOrDefault(city, 0L) + 1);
            }
        }
        analytics.put("density", densityMap);

        double totalRescuedKg = 0.0;
        for (com.foodrescue.entity.DonationRequest r : requests) {
            if (r.getStatus() == com.foodrescue.entity.DonationRequestStatus.DELIVERED) {
                totalRescuedKg += r.getReceivedQuantityKg() != null ? r.getReceivedQuantityKg().doubleValue() : 
                                  ((r.getDonation() != null && r.getDonation().getQuantityKg() != null) ? r.getDonation().getQuantityKg().doubleValue() : 0.0);
            }
        }

        double totalWastedKg = 0.0;
        for (com.foodrescue.entity.FoodDonation d : donations) {
            if ("EXPIRED".equals(d.getStatus())) {
                totalWastedKg += d.getQuantityKg() != null ? d.getQuantityKg().doubleValue() : 0.0;
            }
        }

        analytics.put("totalRescuedKg", totalRescuedKg);
        analytics.put("totalWastedKg", totalWastedKg);

        return analytics;
    }

    @Override
    public Map<String, String> getSystemSettings() {
        Map<String, String> settings = new HashMap<>();
        List<com.foodrescue.entity.SystemSetting> list = systemSettingRepository.findAll();
        for (com.foodrescue.entity.SystemSetting s : list) {
            settings.put(s.getKey(), s.getValue());
        }
        return settings;
    }

    @Override
    @Transactional
    public void updateSystemSettings(Map<String, String> settings) {
        settings.forEach((key, val) -> {
            com.foodrescue.entity.SystemSetting s = systemSettingRepository.findById(key)
                .orElse(com.foodrescue.entity.SystemSetting.builder().key(key).build());
            s.setValue(val);
            systemSettingRepository.save(s);
        });
        auditLogService.log("admin_workspace", "SETTINGS_UPDATED", "INFO", "System settings updated: " + settings.toString());
    }
}
