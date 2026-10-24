package com.foodrescue.controller;

import com.foodrescue.dto.NgoSummaryResponse;
import com.foodrescue.entity.User;
import com.foodrescue.repository.LocationRepository;
import com.foodrescue.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides NGO discovery endpoints for donors.
 * Only adds the missing /api/ngo/available endpoint;
 * all existing /api/ngo/** rules in SecurityConfig remain unchanged.
 */
@RestController
@RequestMapping("/api/ngo")
@RequiredArgsConstructor
@Tag(name = "NGO Discovery", description = "Endpoints for discovering available NGOs")
@SecurityRequirement(name = "Bearer Authentication")
public class NgoController {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    /**
     * Returns all NGO users (verified or not) so donors can pick who to request.
     * Optional: filter further by verified=true in a future iteration.
     *
     * GET /api/ngo/available
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('DONOR', 'NGO', 'ADMIN')")
    @Operation(summary = "Get list of available NGOs for donor to request")
    public ResponseEntity<List<NgoSummaryResponse>> getAvailableNgos() {
        List<User> ngos = userRepository.findByRole("ROLE_NGO");

        List<NgoSummaryResponse> response = ngos.stream()
                .map(ngo -> {
                    // Try to find linked location for this NGO
                    var locationOpt = locationRepository.findByUser(ngo);
                    String address = locationOpt.map(l -> l.getAddress()).orElse(null);
                    Double lat = locationOpt.map(l -> l.getLatitude()).orElse(null);
                    Double lng = locationOpt.map(l -> l.getLongitude()).orElse(null);

                    return NgoSummaryResponse.builder()
                            .id(ngo.getId())
                            .name(ngo.getName())
                            .email(ngo.getEmail())
                            .phone(ngo.getPhone())
                            .verified(ngo.isVerified())
                            .address(address)
                            .latitude(lat)
                            .longitude(lng)
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
