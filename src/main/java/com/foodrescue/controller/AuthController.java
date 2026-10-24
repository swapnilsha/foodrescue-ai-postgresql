package com.foodrescue.controller;

import com.foodrescue.dto.AuthRequest;
import com.foodrescue.dto.AuthResponse;
import com.foodrescue.dto.ProfileUpdateRequest;
import com.foodrescue.dto.RegisterRequest;
import com.foodrescue.entity.Location;
import com.foodrescue.entity.User;
import com.foodrescue.repository.LocationRepository;
import com.foodrescue.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and profile management")
public class AuthController {

    private final AuthService authService;
    private final LocationRepository locationRepository;

    @PostMapping("/register")
    @Operation(summary = "Register a new user (Admin, Donor, or NGO)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and return a JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Returns the full profile of the currently authenticated user.
     * Includes organizationName, legalRegistrationId, and address so the
     * profile page can populate form fields with real data.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's full profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) userDetails;

        // Look up the user's address from the locations table
        String address = locationRepository.findByUser(user)
                .map(Location::getAddress)
                .orElse("");

        return ResponseEntity.ok(Map.of(
                "id",                  user.getId(),
                "email",               user.getEmail(),
                "name",                user.getName(),
                "role",                user.getRole(),
                "verified",            user.isVerified(),
                "phone",               user.getPhone() != null ? user.getPhone() : "",
                "organizationName",    user.getOrganizationName() != null ? user.getOrganizationName() : "",
                "legalRegistrationId", user.getLegalRegistrationId() != null ? user.getLegalRegistrationId() : "",
                "address",             address
        ));
    }

    /**
     * Updates the authenticated user's own profile.
     * Only the fields supplied in the request body are updated.
     * Returns the updated profile data.
     */
    @PutMapping("/profile")
    @Operation(summary = "Update the authenticated user's own profile")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProfileUpdateRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) userDetails;
        Map<String, Object> updated = authService.updateProfile(user, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Changes the authenticated user's password.
     * Requires the current password to be provided for verification.
     */
    @PutMapping("/password")
    @Operation(summary = "Change the authenticated user's password")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Both currentPassword and newPassword are required."));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "New password must be at least 6 characters."));
        }

        try {
            User user = (User) userDetails;
            authService.changePassword(user, currentPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(400)
                    .body(Map.of("message", "Current password is incorrect."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout (client-side token removal)")
    public ResponseEntity<Map<String, String>> logout() {
        // JWT is stateless — actual invalidation is done client-side by clearing localStorage
        return ResponseEntity.ok(Map.of("message", "Logged out successfully. Please clear your local token."));
    }
}