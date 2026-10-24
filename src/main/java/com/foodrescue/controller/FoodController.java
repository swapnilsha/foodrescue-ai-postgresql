package com.foodrescue.controller;

import com.foodrescue.dto.DonationResponse;
import com.foodrescue.dto.FoodDonationRequest;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.service.FoodDonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
@Tag(name = "Food Management", description = "Endpoints for managing food donations")
@SecurityRequirement(name = "Bearer Authentication")
public class FoodController {

    private final FoodDonationService foodDonationService;

    // --- B-17 FIX: Multipart upload endpoint ---
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Upload food donation with optional image")
    public ResponseEntity<FoodDonation> uploadDonation(
            @RequestParam("title") String title,
            @RequestParam("foodCategory") String foodCategory,
            @RequestParam("quantityKg") java.math.BigDecimal quantityKg,
            @RequestParam("hoursUntilExpiry") Integer hoursUntilExpiry,
            @RequestParam("locationId") Long locationId,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        FoodDonationRequest request = new FoodDonationRequest();
        request.setTitle(title);
        request.setFoodCategory(foodCategory);
        request.setQuantityKg(quantityKg);
        request.setHoursUntilExpiry(hoursUntilExpiry);
        request.setLocationId(locationId);

        FoodDonation created = foodDonationService.createDonationWithImage(request, image);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // --- Original JSON endpoint ---
    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Create food donation (JSON)")
    public ResponseEntity<FoodDonation> createDonation(@Valid @RequestBody FoodDonationRequest request) {
        return new ResponseEntity<>(foodDonationService.createDonation(request), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN', 'NGO')")
    @Operation(summary = "Get all available food")
    public ResponseEntity<List<FoodDonation>> getAllAvailableFood() {
        return ResponseEntity.ok(foodDonationService.getAllAvailableFood());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN', 'NGO')")
    @Operation(summary = "Get a food donation by ID")
    public ResponseEntity<FoodDonation> getDonationById(@PathVariable Long id) {
        return ResponseEntity.ok(foodDonationService.getDonationById(id));
    }

    // --- B-15 FIX: Donor's own donations ---
    @GetMapping("/donor")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    @Operation(summary = "Get logged-in donor's donations")
    public ResponseEntity<List<DonationResponse>> getDonorDonations() {
        return ResponseEntity.ok(foodDonationService.getDonorDonations());
    }

    // --- B-15 FIX: NGO's accepted donations ---
    @GetMapping("/ngo")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Get donations accepted by logged-in NGO")
    public ResponseEntity<List<DonationResponse>> getNgoDonations() {
        return ResponseEntity.ok(foodDonationService.getNgoDonations());
    }

    // --- B-14 FIX: Status update endpoint (NGO accept/reject) ---
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN', 'DONOR')")
    @Operation(summary = "Update donation status (ACCEPTED, COLLECTED, EXPIRED, etc.)")
    public ResponseEntity<DonationResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(foodDonationService.updateDonationStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    @Operation(summary = "Delete a donation")
    public ResponseEntity<Void> deleteDonation(@PathVariable Long id) {
        foodDonationService.deleteDonation(id);
        return ResponseEntity.noContent().build();
    }
}