package com.foodrescue.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Lightweight NGO summary shown to donors when selecting which NGO to request.
 */
@Data
@Builder
public class NgoSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private boolean verified;

    // Location details (from the NGO's linked location row)
    private String address;
    private Double latitude;
    private Double longitude;
}
