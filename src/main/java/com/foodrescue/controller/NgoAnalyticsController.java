package com.foodrescue.controller;

import com.foodrescue.entity.*;
import com.foodrescue.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ngo")
@RequiredArgsConstructor
public class NgoAnalyticsController {

    private final DonationRequestRepository requestRepository;

    @GetMapping("/analytics/stats")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<DonationRequest> requests = requestRepository.findByNgoOrderByRequestedAtDesc(user);

        long receivedCount = requests.size();
        long acceptedCount = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.ACCEPTED).count();
        long rejectedCount = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.REJECTED).count();
        long collectedCount = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.COLLECTED).count();
        long receivedQuantityCount = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.RECEIVED).count();
        long deliveredCount = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.DELIVERED).count();

        double totalRescuedQty = requests.stream()
                .filter(r -> r.getStatus() == DonationRequestStatus.DELIVERED || r.getStatus() == DonationRequestStatus.RECEIVED)
                .mapToDouble(r -> {
                    if (r.getReceivedQuantityKg() != null) return r.getReceivedQuantityKg().doubleValue();
                    if (r.getDonation() != null && r.getDonation().getQuantityKg() != null) return r.getDonation().getQuantityKg().doubleValue();
                    return 0.0;
                }).sum();

        // Avg acceptance response time in minutes (requested_at to accepted_at)
        double avgResponseMinutes = requests.stream()
                .filter(r -> r.getRequestedAt() != null && r.getAcceptedAt() != null)
                .mapToLong(r -> java.time.Duration.between(r.getRequestedAt(), r.getAcceptedAt()).toMinutes())
                .average()
                .orElse(0.0);

        // Avg pickup completion time (accepted_at to collected_at)
        double avgPickupMinutes = requests.stream()
                .filter(r -> r.getAcceptedAt() != null && r.getCollectedAt() != null)
                .mapToLong(r -> java.time.Duration.between(r.getAcceptedAt(), r.getCollectedAt()).toMinutes())
                .average()
                .orElse(0.0);

        double successRate = receivedCount > 0 ? (double) (collectedCount + receivedQuantityCount + deliveredCount) / receivedCount * 100.0 : 0.0;

        // Categories distribution
        Map<String, Double> categoryDist = new HashMap<>();
        for (DonationRequest r : requests) {
            if (r.getDonation() != null) {
                String cat = r.getDonation().getFoodCategory() != null ? r.getDonation().getFoodCategory() : "Other";
                double qty = r.getDonation().getQuantityKg() != null ? r.getDonation().getQuantityKg().doubleValue() : 0.0;
                categoryDist.put(cat, categoryDist.getOrDefault(cat, 0.0) + qty);
            }
        }

        // Trends: last 6 active requests
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        List<DonationRequest> recent = requests.stream().limit(6).collect(Collectors.toList());
        Collections.reverse(recent);
        for (DonationRequest r : recent) {
            labels.add(r.getRequestedAt().format(fmt));
            double qty = 0.0;
            if (r.getReceivedQuantityKg() != null) {
                qty = r.getReceivedQuantityKg().doubleValue();
            } else if (r.getDonation() != null && r.getDonation().getQuantityKg() != null) {
                qty = r.getDonation().getQuantityKg().doubleValue();
            }
            values.add(qty);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("requestsReceived", receivedCount);
        response.put("requestsAccepted", acceptedCount);
        response.put("requestsRejected", rejectedCount);
        response.put("foodCollected", collectedCount);
        response.put("foodReceived", receivedQuantityCount);
        response.put("foodDelivered", deliveredCount);
        response.put("totalQuantityRescuedKg", Math.round(totalRescuedQty * 10.0) / 10.0);
        response.put("avgResponseTimeMinutes", Math.round(avgResponseMinutes * 10.0) / 10.0);
        response.put("avgPickupTimeMinutes", Math.round(avgPickupMinutes * 10.0) / 10.0);
        response.put("successRate", Math.round(successRate * 10.0) / 10.0);

        Map<String, Object> trendMap = new HashMap<>();
        trendMap.put("labels", labels);
        trendMap.put("values", values);
        response.put("trends", trendMap);

        response.put("categoryDistribution", categoryDist);

        return ResponseEntity.ok(response);
    }
}
