package com.foodrescue.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String message;
    private String role;   // e.g. ROLE_DONOR, ROLE_NGO, ROLE_ADMIN
    private String email;
    private String name;
    private Long id;
}