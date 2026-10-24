package com.foodrescue.service.impl;

import com.foodrescue.dto.AuthRequest;
import com.foodrescue.dto.AuthResponse;
import com.foodrescue.dto.ProfileUpdateRequest;
import com.foodrescue.dto.RegisterRequest;
import com.foodrescue.entity.Location;
import com.foodrescue.entity.User;
import com.foodrescue.repository.LocationRepository;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.AuthService;
import com.foodrescue.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final com.foodrescue.service.AuditLogService auditLogService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use.");
        }

        // Normalize role — accept "DONOR", "NGO", "ADMIN" or already-prefixed values
        String role = normalizeRole(request.getRole());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .phone(request.getPhone())
                .verified(false)
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.log(savedUser.getEmail(), "REGISTER", "INFO", "Registered user: " + savedUser.getName() + " with role: " + savedUser.getRole());
        String jwtToken = jwtUtils.generateToken(savedUser);

        return AuthResponse.builder()
                .id(savedUser.getId())
                .token(jwtToken)
                .role(savedUser.getRole())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .message("User registered successfully")
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        String jwtToken = jwtUtils.generateToken(user);
        auditLogService.log(user.getEmail(), "LOGIN", "INFO", "User logged in: " + user.getName());

        return AuthResponse.builder()
                .id(user.getId())
                .token(jwtToken)
                .role(user.getRole())
                .email(user.getEmail())
                .name(user.getName())
                .message("Login successful")
                .build();
    }

    /**
     * Updates the authenticated user's own profile.
     * Only non-null/non-blank values in the request are applied.
     * The address field creates or updates the user's linked Location record.
     */
    @Override
    @Transactional
    public Map<String, Object> updateProfile(User currentUser, ProfileUpdateRequest request) {
        // Update basic user fields
        if (request.getName() != null && !request.getName().isBlank()) {
            currentUser.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            currentUser.setPhone(request.getPhone().trim());
        }
        if (request.getOrganizationName() != null) {
            currentUser.setOrganizationName(request.getOrganizationName().trim());
        }
        if (request.getLegalRegistrationId() != null) {
            currentUser.setLegalRegistrationId(request.getLegalRegistrationId().trim());
        }

        userRepository.save(currentUser);

        // Update or create the linked location (address) record
        String address = request.getAddress();
        if (address != null) {
            Location location = locationRepository.findByUser(currentUser)
                    .orElse(Location.builder()
                            .user(currentUser)
                            .latitude(0.0)
                            .longitude(0.0)
                            .build());
            location.setAddress(address.trim());
            locationRepository.save(location);
        }

        auditLogService.log(currentUser.getEmail(), "PROFILE_UPDATE", "INFO",
                "User updated their profile: " + currentUser.getName());

        // Return the refreshed profile data so the frontend can update immediately
        return buildProfileMap(currentUser, address);
    }

    /**
     * Changes the user's password after verifying the current one.
     * Throws BadCredentialsException if the current password is wrong.
     */
    @Override
    @Transactional
    public void changePassword(User currentUser, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, currentUser.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }
        currentUser.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(currentUser);
        auditLogService.log(currentUser.getEmail(), "PASSWORD_CHANGE", "INFO",
                "User changed their password.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String normalizeRole(String role) {
        if (role == null) throw new IllegalArgumentException("Role is required.");
        String upper = role.trim().toUpperCase();
        if (upper.startsWith("ROLE_")) return upper;
        return "ROLE_" + upper;
    }

    /**
     * Builds the profile map returned to the frontend after a successful update.
     * The address parameter is the new address string (may be null if not updated).
     */
    private Map<String, Object> buildProfileMap(User user, String newAddress) {
        Map<String, Object> map = new HashMap<>();
        map.put("id",                  user.getId());
        map.put("name",                user.getName());
        map.put("email",               user.getEmail());
        map.put("role",                user.getRole());
        map.put("phone",               user.getPhone() != null ? user.getPhone() : "");
        map.put("organizationName",    user.getOrganizationName() != null ? user.getOrganizationName() : "");
        map.put("legalRegistrationId", user.getLegalRegistrationId() != null ? user.getLegalRegistrationId() : "");
        map.put("verified",            user.isVerified());

        // Include address: prefer newly submitted value, then fall back to DB
        if (newAddress != null) {
            map.put("address", newAddress.trim());
        } else {
            String dbAddress = locationRepository.findByUser(user)
                    .map(Location::getAddress).orElse("");
            map.put("address", dbAddress);
        }
        return map;
    }
}