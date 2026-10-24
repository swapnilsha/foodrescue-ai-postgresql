package com.foodrescue.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    /**
     * BUG FIX: LazyInitializationException
     * Location.user was LAZY with no @JsonIgnore.
     * Jackson was trying to serialize User after the Hibernate session closed,
     * causing "could not initialize proxy - no Session" on POST /api/food.
     * Ignoring the 'user' field entirely from JSON serialization fixes the 500 error.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}