package com.foodrescue.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one donor → NGO food rescue request.
 * This is entirely separate from FoodDonation — the donation records the food,
 * this entity records the delivery workflow between donor and NGO.
 */
@Entity
@Table(name = "donation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donation_id", nullable = false)
    @JsonIgnoreProperties({"donor", "acceptedByNgo", "location"})
    private FoodDonation donation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donor_id", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
    private User donor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ngo_id", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
    private User ngo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DonationRequestStatus status = DonationRequestStatus.PENDING;

    // ── Timestamps per workflow stage ─────────────────────────────────────────

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "assigned_to_name")
    private String assignedToName;

    @Column(name = "assigned_to_phone", length = 30)
    private String assignedToPhone;

    @Column(name = "pickup_started_at")
    private LocalDateTime pickupStartedAt;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "received_quantity_kg", precision = 5, scale = 2)
    private BigDecimal receivedQuantityKg;

    @Column(name = "delivery_notes", length = 1000)
    private String deliveryNotes;

    // ── Optimistic locking ────────────────────────────────────────────────────

    @Version
    private Long version;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.requestedAt == null) this.requestedAt = now;
        if (this.status == null) this.status = DonationRequestStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
