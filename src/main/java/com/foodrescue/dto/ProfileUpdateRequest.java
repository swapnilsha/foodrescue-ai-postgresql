package com.foodrescue.dto;

import lombok.Data;

/**
 * DTO for updating the authenticated user's own profile.
 * All fields are optional — only non-null values will be applied.
 */
@Data
public class ProfileUpdateRequest {
    private String name;
    private String phone;
    private String organizationName;
    private String legalRegistrationId;
    private String address;   // The user's pickup / storage location address
}
