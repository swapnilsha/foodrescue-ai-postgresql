package com.foodrescue.service.impl;

import com.foodrescue.dto.DonationRequestDto;
import com.foodrescue.dto.DonationRequestResponse;
import com.foodrescue.entity.*;
import com.foodrescue.repository.DonationRequestRepository;
import com.foodrescue.repository.FoodDonationRepository;
import com.foodrescue.repository.UserRepository;
import com.foodrescue.service.DonationRequestService;
import com.foodrescue.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DonationRequestServiceImpl implements DonationRequestService {

    private final DonationRequestRepository requestRepository;
    private final FoodDonationRepository donationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.foodrescue.service.AuditLogService auditLogService;

    // ── Allowed status transitions ────────────────────────────────────────────

    private static final java.util.Map<DonationRequestStatus, Set<DonationRequestStatus>> ALLOWED_TRANSITIONS =
            java.util.Map.of(
                DonationRequestStatus.PENDING, Set.of(
                        DonationRequestStatus.ACCEPTED, DonationRequestStatus.REJECTED,
                        DonationRequestStatus.EXPIRED, DonationRequestStatus.CANCELLED),
                DonationRequestStatus.ACCEPTED, Set.of(
                        DonationRequestStatus.ASSIGNED, DonationRequestStatus.CANCELLED,
                        DonationRequestStatus.EXPIRED),
                DonationRequestStatus.ASSIGNED, Set.of(
                        DonationRequestStatus.PICKUP_IN_PROGRESS, DonationRequestStatus.CANCELLED),
                DonationRequestStatus.PICKUP_IN_PROGRESS, Set.of(DonationRequestStatus.COLLECTED),
                DonationRequestStatus.COLLECTED, Set.of(DonationRequestStatus.RECEIVED),
                DonationRequestStatus.RECEIVED, Set.of(DonationRequestStatus.DELIVERED),
                DonationRequestStatus.DELIVERED, Set.of(),
                DonationRequestStatus.REJECTED, Set.of(),
                DonationRequestStatus.EXPIRED, Set.of(),
                DonationRequestStatus.CANCELLED, Set.of()
            );

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse createRequest(DonationRequestDto dto) {
        User donor = getCurrentUser();
        requireRole(donor, "ROLE_DONOR");

        FoodDonation donation = donationRepository.findById(dto.getDonationId())
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + dto.getDonationId()));

        // Security: donor must own this donation
        if (!donation.getDonor().getId().equals(donor.getId())) {
            throw new AccessDeniedException("You can only request pickup for your own donations.");
        }

        // Check donation status — must be available or requested
        String donationStatus = donation.getStatus();
        if ("EXPIRED".equalsIgnoreCase(donationStatus) || "COLLECTED".equalsIgnoreCase(donationStatus)
                || "ACCEPTED".equalsIgnoreCase(donationStatus)) {
            throw new IllegalStateException("Donation is no longer available for requests (status: " + donationStatus + ").");
        }

        // Check food expiry
        if (donation.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("This donation has already expired.");
        }

        User ngo = userRepository.findById(dto.getNgoId())
                .orElseThrow(() -> new IllegalArgumentException("NGO not found: " + dto.getNgoId()));

        if (!ngo.getRole().equals("ROLE_NGO")) {
            throw new IllegalArgumentException("Target user is not an NGO.");
        }

        // Duplicate prevention
        if (requestRepository.existsByDonationAndNgo(donation, ngo)) {
            throw new IllegalStateException("A request has already been sent to this NGO for this donation.");
        }

        DonationRequest request = DonationRequest.builder()
                .donation(donation)
                .donor(donor)
                .ngo(ngo)
                .status(DonationRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(donor.getEmail(), "REQUEST_CREATED", "INFO", "Donation request sent to NGO " + ngo.getName() + " for: " + donation.getTitle());
        log.info("Donation request #{} created: donor={} → ngo={}", saved.getId(), donor.getEmail(), ngo.getEmail());

        // Notify NGO
        notificationService.createNotification(
                ngo,
                "New food donation request received from " + donor.getName() + " — " + donation.getTitle(),
                "REQUEST_RECEIVED",
                donation
        );

        return toResponse(saved);
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    public List<DonationRequestResponse> getDonorRequests() {
        User donor = getCurrentUser();
        return requestRepository.findByDonorOrderByRequestedAtDesc(donor)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DonationRequestResponse> getNgoRequests() {
        User ngo = getCurrentUser();
        return requestRepository.findByNgoOrderByRequestedAtDesc(ngo)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public DonationRequestResponse getRequestById(Long requestId) {
        DonationRequest request = findRequest(requestId);
        User current = getCurrentUser();
        validateOwnership(request, current);
        return toResponse(request);
    }

    // ── NGO ACCEPT ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse acceptRequest(Long requestId) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");

        DonationRequest request = findRequest(requestId);

        // NGO must be the target of this request
        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("You are not authorized to accept this request.");
        }

        try {
            validateTransition(request.getStatus(), DonationRequestStatus.ACCEPTED);
            request.setStatus(DonationRequestStatus.ACCEPTED);
            request.setAcceptedAt(LocalDateTime.now());

            // Update donation status and accepted NGO
            FoodDonation donation = request.getDonation();
            donation.setStatus("ACCEPTED");
            donation.setAcceptedByNgo(ngo);
            donationRepository.save(donation);

            // Cancel any other pending requests for the same donation
            List<DonationRequest> pendingRequests = requestRepository.findPendingByDonation(donation, DonationRequestStatus.PENDING);
            for (DonationRequest other : pendingRequests) {
                if (!other.getId().equals(request.getId())) {
                    other.setStatus(DonationRequestStatus.CANCELLED);
                    requestRepository.save(other);
                }
            }

            DonationRequest saved = requestRepository.save(request);
            auditLogService.log(ngo.getEmail(), "REQUEST_ACCEPTED", "INFO", "NGO accepted donation request #" + requestId);
            log.info("Request #{} accepted by NGO {}", requestId, ngo.getEmail());

            // Notify donor
            notificationService.createNotification(
                    request.getDonor(),
                    "Your food donation request has been accepted by " + ngo.getName() + ".",
                    "REQUEST_ACCEPTED",
                    request.getDonation()
            );

            return toResponse(saved);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Request was modified by another user. Please refresh and try again.");
        }
    }

    // ── NGO REJECT ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse rejectRequest(Long requestId, String reason) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");

        DonationRequest request = findRequest(requestId);

        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("You are not authorized to reject this request.");
        }

        validateTransition(request.getStatus(), DonationRequestStatus.REJECTED);
        request.setStatus(DonationRequestStatus.REJECTED);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectionReason(reason);
        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(ngo.getEmail(), "REQUEST_REJECTED", "WARNING", "NGO rejected request #" + requestId + " (Reason: " + reason + ")");
        log.info("Request #{} rejected by NGO {}", requestId, ngo.getEmail());

        notificationService.createNotification(
                request.getDonor(),
                "Your food donation request was rejected by " + ngo.getName()
                        + (reason != null && !reason.isBlank() ? " — Reason: " + reason : "."),
                "REQUEST_REJECTED",
                request.getDonation()
        );

        return toResponse(saved);
    }

    // ── ASSIGN DELIVERY ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse assignDelivery(Long requestId, String assignedToName, String assignedToPhone) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");

        DonationRequest request = findRequest(requestId);

        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("You are not authorized to assign delivery for this request.");
        }

        validateTransition(request.getStatus(), DonationRequestStatus.ASSIGNED);
        request.setStatus(DonationRequestStatus.ASSIGNED);
        request.setAssignedAt(LocalDateTime.now());
        request.setAssignedToName(assignedToName);
        request.setAssignedToPhone(assignedToPhone);
        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(ngo.getEmail(), "DELIVERY_ASSIGNED", "INFO", "Assigned driver " + assignedToName + " to request #" + requestId);
        log.info("Request #{} assigned to {} ({})", requestId, assignedToName, assignedToPhone);

        notificationService.createNotification(
                request.getDonor(),
                "Delivery has been assigned for your donation '" + request.getDonation().getTitle()
                        + "'. Driver: " + assignedToName,
                "DELIVERY_ASSIGNED",
                request.getDonation()
        );

        return toResponse(saved);
    }

    // ── START PICKUP ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse startPickup(Long requestId) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");
        DonationRequest request = findRequest(requestId);
        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Not authorized.");
        }
        validateTransition(request.getStatus(), DonationRequestStatus.PICKUP_IN_PROGRESS);
        request.setStatus(DonationRequestStatus.PICKUP_IN_PROGRESS);
        request.setPickupStartedAt(LocalDateTime.now());
        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(ngo.getEmail(), "PICKUP_STARTED", "INFO", "Driver started pickup route for request #" + requestId);

        notificationService.createNotification(
                request.getDonor(),
                "Pickup has started for your donation '" + request.getDonation().getTitle() + "'.",
                "PICKUP_STARTED",
                request.getDonation()
        );
        return toResponse(saved);
    }

    // ── COLLECT ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse collectFood(Long requestId) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");
        DonationRequest request = findRequest(requestId);
        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Not authorized.");
        }
        validateTransition(request.getStatus(), DonationRequestStatus.COLLECTED);
        request.setStatus(DonationRequestStatus.COLLECTED);
        request.setCollectedAt(LocalDateTime.now());

        // Update food donation status
        FoodDonation donation = request.getDonation();
        donation.setStatus("COLLECTED");
        donationRepository.save(donation);

        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(ngo.getEmail(), "FOOD_COLLECTED", "INFO", "Food collected for request #" + requestId);

        notificationService.createNotification(
                request.getDonor(),
                "Your donation '" + request.getDonation().getTitle() + "' has been collected!",
                "FOOD_COLLECTED",
                request.getDonation()
        );
        return toResponse(saved);
    }

    // ── RECEIVE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse receiveFood(Long requestId, BigDecimal receivedQuantityKg, String notes) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");
        DonationRequest request = findRequest(requestId);
        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Not authorized.");
        }
        validateTransition(request.getStatus(), DonationRequestStatus.RECEIVED);
        request.setStatus(DonationRequestStatus.RECEIVED);
        request.setReceivedAt(LocalDateTime.now());
        request.setReceivedQuantityKg(receivedQuantityKg);
        request.setDeliveryNotes(notes);
        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(ngo.getEmail(), "FOOD_RECEIVED", "INFO", "Food received for request #" + requestId + " (" + receivedQuantityKg + " kg)");

        notificationService.createNotification(
                request.getDonor(),
                "Your donation '" + request.getDonation().getTitle() + "' has been received by " + ngo.getName() + ".",
                "FOOD_RECEIVED",
                request.getDonation()
        );
        return toResponse(saved);
    }

    // ── DELIVER ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse deliverFood(Long requestId, String notes) {
        User ngo = getCurrentUser();
        requireRole(ngo, "ROLE_NGO", "ROLE_ADMIN");
        DonationRequest request = findRequest(requestId);
        if (!request.getNgo().getId().equals(ngo.getId()) && !hasRole(ngo, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Not authorized.");
        }
        validateTransition(request.getStatus(), DonationRequestStatus.DELIVERED);
        request.setStatus(DonationRequestStatus.DELIVERED);
        request.setDeliveredAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) request.setDeliveryNotes(notes);
        DonationRequest saved = requestRepository.save(request);
        auditLogService.log(ngo.getEmail(), "FOOD_DELIVERED", "INFO", "Food delivered/distributed for request #" + requestId);
        notificationService.createNotification(
                request.getDonor(),
                "Your donation '" + request.getDonation().getTitle() + "' has been successfully delivered/distributed!",
                "FOOD_DELIVERED",
                request.getDonation()
        );
        log.info("Request #{} marked DELIVERED by NGO {}", requestId, ngo.getEmail());
        return toResponse(saved);
    }

    // ── CANCEL ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DonationRequestResponse cancelRequest(Long requestId) {
        User current = getCurrentUser();
        DonationRequest request = findRequest(requestId);

        boolean isDonor = hasRole(current, "ROLE_DONOR") && request.getDonor().getId().equals(current.getId());
        boolean isAdmin = hasRole(current, "ROLE_ADMIN");
        if (!isDonor && !isAdmin) {
            throw new AccessDeniedException("You are not authorized to cancel this request.");
        }

        // Donor can only cancel PENDING requests
        if (isDonor && request.getStatus() != DonationRequestStatus.PENDING) {
            throw new IllegalStateException("Donors can only cancel requests that are in PENDING status.");
        }

        validateTransition(request.getStatus(), DonationRequestStatus.CANCELLED);
        request.setStatus(DonationRequestStatus.CANCELLED);
        DonationRequest saved = requestRepository.save(request);
        log.info("Request #{} cancelled by {}", requestId, current.getEmail());
        return toResponse(saved);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private DonationRequest findRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found: " + requestId));
    }

    private void validateTransition(DonationRequestStatus current, DonationRequestStatus next) {
        Set<DonationRequestStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                    "Invalid status transition: " + current + " → " + next
            );
        }
    }

    private void validateOwnership(DonationRequest request, User user) {
        boolean isAdmin = hasRole(user, "ROLE_ADMIN");
        boolean isDonor = request.getDonor().getId().equals(user.getId());
        boolean isNgo = request.getNgo().getId().equals(user.getId());
        if (!isAdmin && !isDonor && !isNgo) {
            throw new AccessDeniedException("You do not have permission to view this request.");
        }
    }

    private void requireRole(User user, String... roles) {
        for (String role : roles) {
            if (hasRole(user, role)) return;
        }
        throw new AccessDeniedException("Insufficient role. Required: " + String.join(" or ", roles));
    }

    private boolean hasRole(User user, String role) {
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // ── MAPPING ───────────────────────────────────────────────────────────────

    private DonationRequestResponse toResponse(DonationRequest r) {
        FoodDonation d = r.getDonation();
        User donor = r.getDonor();
        User ngo = r.getNgo();

        return DonationRequestResponse.builder()
                .id(r.getId())
                .donationId(d.getId())
                .donationTitle(d.getTitle())
                .donationFoodCategory(d.getFoodCategory())
                .donationQuantityKg(d.getQuantityKg())
                .donationImagePath(d.getImagePath())
                .donationExpiryTime(d.getExpiryTime())
                .donorId(donor.getId())
                .donorName(donor.getName())
                .donorEmail(donor.getEmail())
                .pickupAddress(d.getLocation() != null ? d.getLocation().getAddress() : null)
                .locationLatitude(d.getLocation() != null ? d.getLocation().getLatitude() : null)
                .locationLongitude(d.getLocation() != null ? d.getLocation().getLongitude() : null)
                .ngoId(ngo.getId())
                .ngoName(ngo.getName())
                .ngoEmail(ngo.getEmail())
                .ngoPhone(ngo.getPhone())
                .status(r.getStatus())
                .requestedAt(r.getRequestedAt())
                .acceptedAt(r.getAcceptedAt())
                .rejectedAt(r.getRejectedAt())
                .rejectionReason(r.getRejectionReason())
                .assignedAt(r.getAssignedAt())
                .assignedToName(r.getAssignedToName())
                .assignedToPhone(r.getAssignedToPhone())
                .pickupStartedAt(r.getPickupStartedAt())
                .collectedAt(r.getCollectedAt())
                .receivedAt(r.getReceivedAt())
                .deliveredAt(r.getDeliveredAt())
                .receivedQuantityKg(r.getReceivedQuantityKg())
                .deliveryNotes(r.getDeliveryNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
