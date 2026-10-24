package com.foodrescue.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalDonors;
    private long totalNgos;
    private long totalDonations;
    private long availableDonations;
    private long acceptedDonations;
    private long collectedDonations;
    private long expiredDonations;
    private long pendingVerifications;
    private Map<String, Long> donationsByCategory;
}
