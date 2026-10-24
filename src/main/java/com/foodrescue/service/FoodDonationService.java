package com.foodrescue.service;

import com.foodrescue.dto.DonationResponse;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.dto.FoodDonationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FoodDonationService {
    FoodDonation createDonation(FoodDonationRequest request);
    FoodDonation createDonationWithImage(FoodDonationRequest request, MultipartFile image);
    List<FoodDonation> getAllAvailableFood();
    List<DonationResponse> getDonorDonations();
    List<DonationResponse> getNgoDonations();
    FoodDonation getDonationById(Long id);
    DonationResponse updateDonationStatus(Long id, String status);
    void deleteDonation(Long id);
}