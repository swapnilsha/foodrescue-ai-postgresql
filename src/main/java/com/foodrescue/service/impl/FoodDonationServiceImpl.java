package com.foodrescue.service.impl;

import com.foodrescue.dto.DonationResponse;
import com.foodrescue.dto.FoodDonationRequest;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.entity.Location;
import com.foodrescue.entity.User;
import com.foodrescue.mapper.FoodDonationMapper;
import com.foodrescue.repository.FoodDonationRepository;
import com.foodrescue.repository.LocationRepository;
import com.foodrescue.service.FoodDonationService;
import com.foodrescue.service.NotificationService;
import com.foodrescue.websocket.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FoodDonationServiceImpl implements FoodDonationService {

    private final FoodDonationRepository donationRepository;
    private final LocationRepository locationRepository;
    private final FoodDonationMapper mapper;
    private final NotificationDispatcher notificationDispatcher;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final com.foodrescue.service.AuditLogService auditLogService;

    @Override
    @Transactional
    public FoodDonation createDonation(FoodDonationRequest request) {
        return createDonationWithImage(request, null);
    }

    @Override
    @Transactional
    public FoodDonation createDonationWithImage(FoodDonationRequest request, MultipartFile image) {
        User currentDonor = getCurrentUser();

        FoodDonation donation = mapper.toEntity(request);
        donation.setDonor(currentDonor);

        Location location = resolveLocation(request, currentDonor);
        donation.setLocation(location);

        if (image != null && !image.isEmpty()) {
            String imagePath = fileStorageService.store(image);
            donation.setImagePath(imagePath);
        }

        FoodDonation saved = donationRepository.save(donation);
        auditLogService.log(currentDonor.getEmail(), "DONATION_CREATED", "INFO", "Surplus food donation uploaded: " + saved.getTitle() + " (" + saved.getQuantityKg() + " kg)");
        log.info("New food donation created — ID: {}, Donor: {}", saved.getId(), currentDonor.getEmail());

        // Broadcast to NGOs via WebSocket
        notificationDispatcher.broadcastNewDonation(saved.getTitle(), 0.0);

        return saved;
    }

    @Override
    public List<FoodDonation> getAllAvailableFood() {
        return donationRepository.findByStatus("AVAILABLE");
    }

    @Override
    public List<DonationResponse> getDonorDonations() {
        User donor = getCurrentUser();
        return donationRepository.findByDonorOrderByCreatedAtDesc(donor)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DonationResponse> getNgoDonations() {
        User ngo = getCurrentUser();
        return donationRepository.findByAcceptedByNgoOrderByCreatedAtDesc(ngo)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public FoodDonation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + id));
    }

    @Override
    @Transactional
    public DonationResponse updateDonationStatus(Long id, String newStatus) {
        FoodDonation donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + id));

        String oldStatus = donation.getStatus();
        donation.setStatus(newStatus.toUpperCase());

        User currentUser = getCurrentUser();

        // Track who accepted
        if ("ACCEPTED".equalsIgnoreCase(newStatus)) {
            donation.setAcceptedByNgo(currentUser);
            // Notify donor
            notificationService.createNotification(
                    donation.getDonor(),
                    "Your donation '" + donation.getTitle() + "' was accepted by an NGO.",
                    "DONATION_ACCEPTED",
                    donation
            );
        } else if ("COLLECTED".equalsIgnoreCase(newStatus)) {
            notificationService.createNotification(
                    donation.getDonor(),
                    "Your donation '" + donation.getTitle() + "' has been collected!",
                    "DONATION_COLLECTED",
                    donation
            );
        }

        FoodDonation saved = donationRepository.save(donation);
        log.info("Donation {} status: {} → {}", id, oldStatus, newStatus);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteDonation(Long id) {
        FoodDonation donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + id));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !donation.getDonor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only delete your own donations.");
        }

        donationRepository.delete(donation);
        log.info("Donation {} deleted by {}", id, currentUser.getEmail());
    }

    // ── Location Resolution (4-tier fallback) ────────────────────────────────

    /** Constant ID of the seeded fallback location (V3 migration). */
    private static final Long FALLBACK_LOCATION_ID = 1L;

    /**
     * Resolves a Location for the donation:
     * 1. Use explicit locationId if provided in the request.
     * 2. Reuse the donor's existing location row (if any).
     * 3. Create a new Location from the provided pickupAddress.
     * 4. Fall back to the seeded default location (id=1).
     */
    private Location resolveLocation(FoodDonationRequest request, User donor) {
        // Tier 1: explicit location ID
        if (request.getLocationId() != null) {
            return locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Location ID: " + request.getLocationId()));
        }

        // Tier 2: donor's pre-existing location
        var existing = locationRepository.findByUser(donor);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Tier 3: create from address string submitted by the form
        String address = request.getPickupAddress();
        if (address != null && !address.isBlank()) {
            Location newLoc = Location.builder()
                    .latitude(0.0)   // geocoding placeholder
                    .longitude(0.0)
                    .address(address.trim())
                    .user(donor)
                    .build();
            Location saved = locationRepository.save(newLoc);
            log.info("Auto-created location {} for donor {}", saved.getId(), donor.getEmail());
            return saved;
        }

        // Tier 4: fallback to seeded default
        log.warn("No location provided by donor {}; falling back to default location id={}", donor.getEmail(), FALLBACK_LOCATION_ID);
        return locationRepository.findById(FALLBACK_LOCATION_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Default location not found. Please run V3 migration or provide a pickup address."));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private DonationResponse toResponse(FoodDonation d) {
        return DonationResponse.builder()

                .id(d.getId())
                .title(d.getTitle())
                .foodCategory(d.getFoodCategory())
                .quantityKg(d.getQuantityKg())
                .status(d.getStatus())
                .expiryTime(d.getExpiryTime())
                .createdAt(d.getCreatedAt())
                .imagePath(d.getImagePath())
                .donorName(d.getDonor() != null ? d.getDonor().getName() : null)
                .donorEmail(d.getDonor() != null ? d.getDonor().getEmail() : null)
                .locationAddress(d.getLocation() != null ? d.getLocation().getAddress() : null)
                .locationLatitude(d.getLocation() != null ? d.getLocation().getLatitude() : null)
                .locationLongitude(d.getLocation() != null ? d.getLocation().getLongitude() : null)
                .acceptedByNgoName(d.getAcceptedByNgo() != null ? d.getAcceptedByNgo().getName() : null)
                .build();
    }
}