package com.foodrescue.controller;

import com.foodrescue.dto.DonationRequestDto;
import com.foodrescue.dto.DonationRequestResponse;
import com.foodrescue.service.DonationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Donation Requests", description = "Workflow: Donor → NGO request lifecycle")
@SecurityRequirement(name = "Bearer Authentication")
public class DonationRequestController {

    private final DonationRequestService requestService;

    /** Donor creates a new donation request for a specific NGO. */
    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    @Operation(summary = "Create a new donation request (DONOR only)")
    public ResponseEntity<DonationRequestResponse> createRequest(
            @RequestBody DonationRequestDto dto) {
        return new ResponseEntity<>(requestService.createRequest(dto), HttpStatus.CREATED);
    }

    /** Get all requests created by the logged-in donor. */
    @GetMapping("/donor")
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN')")
    @Operation(summary = "Get logged-in donor's requests")
    public ResponseEntity<List<DonationRequestResponse>> getDonorRequests() {
        return ResponseEntity.ok(requestService.getDonorRequests());
    }

    /** Get all requests received by the logged-in NGO. */
    @GetMapping("/ngo")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Get logged-in NGO's incoming requests")
    public ResponseEntity<List<DonationRequestResponse>> getNgoRequests() {
        return ResponseEntity.ok(requestService.getNgoRequests());
    }

    /** Get details for a single request (donor, NGO, or admin access only). */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get request details by ID")
    public ResponseEntity<DonationRequestResponse> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    /** NGO accepts a PENDING request → ACCEPTED. */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Accept a pending request (NGO only)")
    public ResponseEntity<DonationRequestResponse> acceptRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.acceptRequest(id));
    }

    /** NGO rejects a PENDING request → REJECTED. */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Reject a pending request (NGO only)")
    public ResponseEntity<DonationRequestResponse> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(requestService.rejectRequest(id, reason));
    }

    /** NGO assigns a delivery person → ASSIGNED. */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Assign delivery personnel (NGO only)")
    public ResponseEntity<DonationRequestResponse> assignDelivery(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.getOrDefault("assignedToName", "NGO Team");
        String phone = body.getOrDefault("assignedToPhone", "");
        return ResponseEntity.ok(requestService.assignDelivery(id, name, phone));
    }

    /** Start pickup → PICKUP_IN_PROGRESS. */
    @PatchMapping("/{id}/pickup-start")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Mark pickup as started")
    public ResponseEntity<DonationRequestResponse> startPickup(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.startPickup(id));
    }

    /** Mark food collected → COLLECTED. */
    @PatchMapping("/{id}/collect")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Mark food as collected from donor")
    public ResponseEntity<DonationRequestResponse> collectFood(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.collectFood(id));
    }

    /** Confirm food received → RECEIVED. */
    @PatchMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Confirm food received at NGO")
    public ResponseEntity<DonationRequestResponse> receiveFood(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        BigDecimal qty = null;
        String notes = null;
        if (body != null) {
            Object qtyVal = body.get("receivedQuantityKg");
            if (qtyVal != null) qty = new BigDecimal(qtyVal.toString());
            notes = (String) body.get("notes");
        }
        return ResponseEntity.ok(requestService.receiveFood(id, qty, notes));
    }

    /** Mark food as delivered → DELIVERED. */
    @PatchMapping("/{id}/deliver")
    @PreAuthorize("hasAnyRole('NGO', 'ADMIN')")
    @Operation(summary = "Mark food as delivered/distributed")
    public ResponseEntity<DonationRequestResponse> deliverFood(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(requestService.deliverFood(id, notes));
    }

    /** Cancel a request (donor or admin). */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a request")
    public ResponseEntity<DonationRequestResponse> cancelRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.cancelRequest(id));
    }
}
