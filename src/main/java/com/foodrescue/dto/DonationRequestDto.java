package com.foodrescue.dto;

import lombok.Data;

/**
 * Request body for creating a new donation request (donor → NGO).
 */
@Data
public class DonationRequestDto {

    /** ID of the food donation to request. */
    private Long donationId;

    /** ID of the NGO user to send the request to. */
    private Long ngoId;
}
