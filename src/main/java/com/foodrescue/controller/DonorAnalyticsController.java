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
@RequestMapping("/api/donor")
@RequiredArgsConstructor
public class DonorAnalyticsController {

    private final DonationRequestRepository requestRepository;
    private final FoodDonationRepository donationRepository;
    private final UserRepository userRepository;

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAnalytics(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<FoodDonation> donations = donationRepository.findByDonorOrderByCreatedAtDesc(user);
        List<DonationRequest> requests = requestRepository.findByDonorOrderByRequestedAtDesc(user);

        long totalDonations = donations.size();
        double totalQty = donations.stream().mapToDouble(d -> d.getQuantityKg() != null ? d.getQuantityKg().doubleValue() : 0.0).sum();
        long deliveredCount = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.DELIVERED).count();
        long pendingRequests = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.PENDING).count();
        long acceptedRequests = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.ACCEPTED).count();
        long rejectedRequests = requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.REJECTED).count();
        long expiredDonations = donations.stream().filter(d -> "EXPIRED".equalsIgnoreCase(d.getStatus())).count();

        // Calculate trends (recent 6 donations for display)
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        List<FoodDonation> recent = donations.stream().limit(6).collect(Collectors.toList());
        Collections.reverse(recent);
        for (FoodDonation d : recent) {
            labels.add(d.getCreatedAt().format(fmt));
            values.add(d.getQuantityKg() != null ? d.getQuantityKg().doubleValue() : 0.0);
        }

        // Category distribution
        Map<String, Double> categoryDist = new HashMap<>();
        for (FoodDonation d : donations) {
            String category = d.getFoodCategory() != null ? d.getFoodCategory() : "Other";
            categoryDist.put(category, categoryDist.getOrDefault(category, 0.0) + (d.getQuantityKg() != null ? d.getQuantityKg().doubleValue() : 0.0));
        }

        double successRate = totalDonations > 0 ? (double) requests.stream().filter(r -> r.getStatus() == DonationRequestStatus.DELIVERED).count() / totalDonations * 100.0 : 0.0;
        
        // Avg pickup time in minutes
        double avgPickupMinutes = requests.stream()
                .filter(r -> r.getRequestedAt() != null && r.getPickupStartedAt() != null)
                .mapToLong(r -> java.time.Duration.between(r.getRequestedAt(), r.getPickupStartedAt()).toMinutes())
                .average()
                .orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("totalDonations", totalDonations);
        response.put("totalQuantityKg", Math.round(totalQty * 10.0) / 10.0);
        response.put("deliveredDonations", deliveredCount);
        response.put("pendingRequests", pendingRequests);
        response.put("acceptedRequests", acceptedRequests);
        response.put("rejectedRequests", rejectedRequests);
        response.put("expiredDonations", expiredDonations);
        response.put("mealsSaved", Math.round(totalQty * 2.0)); 
        response.put("co2ReductionKg", Math.round(totalQty * 1.5 * 10.0) / 10.0); 
        response.put("successRate", Math.round(successRate * 10.0) / 10.0);
        response.put("avgPickupTimeMinutes", Math.round(avgPickupMinutes * 10.0) / 10.0);

        Map<String, Object> trendMap = new HashMap<>();
        trendMap.put("labels", labels);
        trendMap.put("values", values);
        response.put("trends", trendMap);

        response.put("categoryDistribution", categoryDist);

        // Group requests by NGO partner name to show partner distribution
        Map<String, Long> partnerDist = requests.stream()
                .filter(r -> r.getNgo() != null)
                .collect(Collectors.groupingBy(r -> r.getNgo().getName(), Collectors.counting()));
        response.put("partnerDistribution", partnerDist);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/impact")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getImpactLeaderboard(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        List<User> donors = userRepository.findAll().stream()
                .filter(u -> "ROLE_DONOR".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        for (User d : donors) {
            List<DonationRequest> requests = requestRepository.findByDonorOrderByRequestedAtDesc(d);
            double totalWeight = requests.stream()
                    .filter(r -> r.getStatus() == DonationRequestStatus.DELIVERED)
                    .mapToDouble(r -> {
                        if (r.getReceivedQuantityKg() != null) return r.getReceivedQuantityKg().doubleValue();
                        if (r.getDonation() != null && r.getDonation().getQuantityKg() != null) return r.getDonation().getQuantityKg().doubleValue();
                        return 0.0;
                    }).sum();

            Map<String, Object> map = new HashMap<>();
            map.put("name", d.getName());
            map.put("mealsSaved", Math.round(totalWeight * 2.0));
            map.put("co2Offset", Math.round(totalWeight * 1.5 * 10.0) / 10.0);
            map.put("socialRoi", Math.round(totalWeight * 5.0));
            map.put("isCurrent", d.getId().equals(currentUser.getId()));
            map.put("email", d.getEmail());
            leaderboard.add(map);
        }

        leaderboard.sort((m1, m2) -> Long.compare(((Number) m2.get("mealsSaved")).longValue(), ((Number) m1.get("mealsSaved")).longValue()));

        int rank = 1;
        int currentRank = -1;
        for (int i = 0; i < leaderboard.size(); i++) {
            Map<String, Object> m = leaderboard.get(i);
            m.put("rank", rank++);
            if ((boolean) m.get("isCurrent")) {
                currentRank = i + 1;
            }
        }

        List<DonationRequest> currentRequests = requestRepository.findByDonorOrderByRequestedAtDesc(currentUser);
        double currentWeight = currentRequests.stream()
                .filter(r -> r.getStatus() == DonationRequestStatus.DELIVERED)
                .mapToDouble(r -> {
                    if (r.getReceivedQuantityKg() != null) return r.getReceivedQuantityKg().doubleValue();
                    if (r.getDonation() != null && r.getDonation().getQuantityKg() != null) return r.getDonation().getQuantityKg().doubleValue();
                    return 0.0;
                }).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("leaderboard", leaderboard);
        response.put("currentRank", currentRank > 0 ? getOrdinal(currentRank) : "N/A");
        response.put("currentMealsSaved", Math.round(currentWeight * 2.0));
        response.put("currentCo2Offset", Math.round(currentWeight * 1.5 * 10.0) / 10.0);
        response.put("currentName", currentUser.getName());
        response.put("currentPoints", Math.round(currentWeight * 10.0));

        return ResponseEntity.ok(response);
    }

    private String getOrdinal(int value) {
        int hundredRemainder = value % 100;
        int tenRemainder = value % 10;
        if (hundredRemainder - tenRemainder == 10) {
            return value + "th";
        }
        switch (tenRemainder) {
            case 1:
                return value + "st";
            case 2:
                return value + "nd";
            case 3:
                return value + "rd";
            default:
                return value + "th";
        }
    }
}
