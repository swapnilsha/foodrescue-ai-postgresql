package com.foodrescue.entity;

/**
 * Represents all possible states in the donation request lifecycle.
 * Transitions are validated in DonationRequestServiceImpl.
 */
public enum DonationRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    ASSIGNED,
    PICKUP_IN_PROGRESS,
    COLLECTED,
    RECEIVED,
    DELIVERED,
    EXPIRED,
    CANCELLED
}
