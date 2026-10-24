package com.foodrescue.service;

import com.foodrescue.dto.DonationResponse;

import java.util.List;
import java.util.Map;

public interface AdminService {
    Map<String, Object> getStats();
    List<DonationResponse> getAllDonations(int page, int size);
    List<Map<String, Object>> getActiveMissions();
    void verifyUser(Long userId, boolean verified);
    void deleteUser(Long userId);
    Map<String, Object> getAnalyticsStats();
    Map<String, String> getSystemSettings();
    void updateSystemSettings(Map<String, String> settings);
}

