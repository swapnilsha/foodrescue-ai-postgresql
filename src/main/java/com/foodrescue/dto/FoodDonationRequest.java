package com.foodrescue.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodDonationRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Food category is required")
    private String foodCategory;

    @DecimalMin(value = "0.1", message = "Quantity must be greater than 0")
    private BigDecimal quantityKg;

    @Min(value = 1, message = "Food must be valid for at least 1 hour")
    private Integer hoursUntilExpiry;

    // Optional — if provided, looked up directly; otherwise pickupAddress is used
    private Long locationId;

    // Optional — donor's pickup address (used to auto-create a location if locationId is absent)
    private String pickupAddress;

    // Optional — pickup urgency level ("HIGH" or "LOW"), used for prioritization
    private String urgency;

    // Optional — special notes, allergen info, or handling instructions
    private String notes;
}