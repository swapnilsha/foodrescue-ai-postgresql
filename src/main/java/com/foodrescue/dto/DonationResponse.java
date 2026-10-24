package com.foodrescue.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DonationResponse {
    private Long id;
    private String title;
    private String foodCategory;
    private BigDecimal quantityKg;
    private String status;
    private LocalDateTime expiryTime;
    private LocalDateTime createdAt;
    private String imagePath;
    private String donorName;
    private String donorEmail;
    private String locationAddress;
    private Double locationLatitude;
    private Double locationLongitude;
    private String acceptedByNgoName;
}
