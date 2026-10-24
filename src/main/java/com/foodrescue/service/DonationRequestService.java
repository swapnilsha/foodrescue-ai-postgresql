package com.foodrescue.service;

import com.foodrescue.dto.DonationRequestDto;
import com.foodrescue.dto.DonationRequestResponse;

import java.math.BigDecimal;
import java.util.List;

public interface DonationRequestService {

    /** Donor creates a request for a donation to be handled by a specific NGO. */
    DonationRequestResponse createRequest(DonationRequestDto dto);

    /** Returns all requests created by the currently authenticated donor. */
    List<DonationRequestResponse> getDonorRequests();

    /** Returns all requests received by the currently authenticated NGO. */
    List<DonationRequestResponse> getNgoRequests();

    /** Returns a single request by ID (validates ownership). */
    DonationRequestResponse getRequestById(Long requestId);

    /** NGO accepts a PENDING request (PENDING → ACCEPTED). */
    DonationRequestResponse acceptRequest(Long requestId);

    /** NGO rejects a PENDING request (PENDING → REJECTED). */
    DonationRequestResponse rejectRequest(Long requestId, String reason);

    /** NGO assigns a delivery person (ACCEPTED → ASSIGNED). */
    DonationRequestResponse assignDelivery(Long requestId, String assignedToName, String assignedToPhone);

    /** Delivery person/NGO starts pickup (ASSIGNED → PICKUP_IN_PROGRESS). */
    DonationRequestResponse startPickup(Long requestId);

    /** Marks food as collected from donor (PICKUP_IN_PROGRESS → COLLECTED). */
    DonationRequestResponse collectFood(Long requestId);

    /** NGO confirms food received (COLLECTED → RECEIVED). */
    DonationRequestResponse receiveFood(Long requestId, BigDecimal receivedQuantityKg, String notes);

    /** NGO marks food as delivered/distributed (RECEIVED → DELIVERED). */
    DonationRequestResponse deliverFood(Long requestId, String notes);

    /** Cancel a request (donor or admin). */
    DonationRequestResponse cancelRequest(Long requestId);
}
