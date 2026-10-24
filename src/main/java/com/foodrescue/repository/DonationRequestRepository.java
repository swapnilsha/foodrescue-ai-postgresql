package com.foodrescue.repository;

import com.foodrescue.entity.DonationRequest;
import com.foodrescue.entity.DonationRequestStatus;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRequestRepository extends JpaRepository<DonationRequest, Long> {

    /** All requests created by a specific donor, newest first. */
    List<DonationRequest> findByDonorOrderByRequestedAtDesc(User donor);

    /** All requests received by a specific NGO, newest first. */
    List<DonationRequest> findByNgoOrderByRequestedAtDesc(User ngo);

    /** Find request for a specific donation+NGO pair (duplicate detection). */
    Optional<DonationRequest> findByDonationAndNgo(FoodDonation donation, User ngo);

    /** Check existence for duplicate guard. */
    boolean existsByDonationAndNgo(FoodDonation donation, User ngo);

    /** Find all requests in a given status. */
    List<DonationRequest> findByStatus(DonationRequestStatus status);

    /** Count requests received by an NGO. */
    long countByNgo(User ngo);

    /** Count requests in a given status received by an NGO. */
    long countByNgoAndStatus(User ngo, DonationRequestStatus status);

    /** Count all requests in a given status (admin overview). */
    long countByStatus(DonationRequestStatus status);

    /**
     * BUG FIX: Previously used string literal 'PENDING' in JPQL for an @Enumerated(STRING) field.
     * While it works at runtime, using the enum parameter is cleaner and safer.
     * Pending requests for a specific donation (to expire them).
     */
    @Query("SELECT r FROM DonationRequest r WHERE r.donation = :donation AND r.status = :status")
    List<DonationRequest> findPendingByDonation(
            @Param("donation") FoodDonation donation,
            @Param("status") DonationRequestStatus status);

    /** Count all requests by NGO across all statuses (for NGO dashboard stats). */
    long countByNgoAndStatusIn(User ngo, List<DonationRequestStatus> statuses);
}
