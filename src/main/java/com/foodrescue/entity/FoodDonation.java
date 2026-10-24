package com.foodrescue.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDonation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "donor_id", nullable = false)
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "username"})
    private User donor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "accepted_by_ngo_id")
    @JsonIgnoreProperties({"password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "username"})
    private User acceptedByNgo;

    @Column(nullable = false)
    private String title;

    @Column(name = "food_category", nullable = false)
    private String foodCategory;

    @Column(name = "quantity_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal quantityKg;

    @Column(nullable = false)
    private String status; // AVAILABLE, REQUESTED, ACCEPTED, COLLECTED, EXPIRED

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "image_path")
    private String imagePath;

    // Optional fields from upload form
    @Column(name = "urgency", length = 10)
    private String urgency; // HIGH or LOW

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "AVAILABLE";
        }
    }
}