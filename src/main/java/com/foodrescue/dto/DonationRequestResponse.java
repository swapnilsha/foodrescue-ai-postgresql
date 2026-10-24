package com.foodrescue.dto;

import com.foodrescue.entity.DonationRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Full read-only response returned for a donation request.
 */
@Data
@Builder
public class DonationRequestResponse {

    private Long id;

    // Donation info
    private Long donationId;
    private String donationTitle;
    private String donationFoodCategory;
    private BigDecimal donationQuantityKg;
    private String donationImagePath;
    private LocalDateTime donationExpiryTime;

    // Donor info
    private Long donorId;
    private String donorName;
    private String donorEmail;

    // Location info
    private String pickupAddress;
    private Double locationLatitude;
    private Double locationLongitude;

    // NGO info
    private Long ngoId;
    private String ngoName;
    private String ngoEmail;
    private String ngoPhone;

    // Workflow status
    private DonationRequestStatus status;

    // Timestamps
    private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime assignedAt;
    private String assignedToName;
    private String assignedToPhone;
    private LocalDateTime pickupStartedAt;
    private LocalDateTime collectedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime deliveredAt;
    private BigDecimal receivedQuantityKg;
    private String deliveryNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
