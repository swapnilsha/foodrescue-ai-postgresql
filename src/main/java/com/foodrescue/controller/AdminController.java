package com.foodrescue.controller;

import com.foodrescue.dto.DonationResponse;
import com.foodrescue.entity.User;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

  
    @GetMapping("/stats")
    @Operation(summary = "Get admin dashboard statistics")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/active-missions")
    @Operation(summary = "Get all active rescue missions")
    public ResponseEntity<List<Map<String, Object>>> getActiveMissions() {
        return ResponseEntity.ok(adminService.getActiveMissions());
    }

    @GetMapping("/donations")
    @Operation(summary = "Get all donations (paginated)")
    public ResponseEntity<List<DonationResponse>> getAllDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getAllDonations(page, size));
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(users.getContent());
    }

    @GetMapping("/users/pending-verification")
    @Operation(summary = "Get NGOs pending verification")
    public ResponseEntity<List<User>> getPendingVerification() {
        return ResponseEntity.ok(userRepository.findByVerifiedFalseAndRoleNot("ROLE_ADMIN"));
    }

    @PatchMapping("/users/{id}/verify")
    @Operation(summary = "Verify or unverify an NGO/Donor")
    public ResponseEntity<Map<String, String>> verifyUser(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean verified = Boolean.TRUE.equals(body.get("verified"));
        adminService.verifyUser(id, verified);
        return ResponseEntity.ok(Map.of("message", "User verification status updated."));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete or reject a user")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
    }

    @GetMapping("/analytics/stats")
    @Operation(summary = "Get platform analytics stats")
    public ResponseEntity<Map<String, Object>> getAnalyticsStats() {
        return ResponseEntity.ok(adminService.getAnalyticsStats());
    }

    @GetMapping("/settings")
    @Operation(summary = "Get system settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(adminService.getSystemSettings());
    }

    @PostMapping("/settings")
    @Operation(summary = "Update system settings")
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> settings) {
        adminService.updateSystemSettings(settings);
        return ResponseEntity.ok(Map.of("message", "System settings updated successfully."));
    }
}
