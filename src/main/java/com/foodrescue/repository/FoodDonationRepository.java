package com.foodrescue.repository;

import com.foodrescue.entity.FoodDonation;
import com.foodrescue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodDonationRepository extends JpaRepository<FoodDonation, Long> {

    // Haversine formula — find available food within a radius (km)
    @Query(value = """
        SELECT fd.*,\s
        ( 6371 * acos( cos( radians(:ngoLat) ) \s
        * cos( radians( l.latitude ) )\s
        * cos( radians( l.longitude ) - radians(:ngoLng) )\s
        + sin( radians(:ngoLat) )\s
        * sin( radians( l.latitude ) ) ) ) AS distance\s
        FROM food_donations fd\s
        JOIN locations l ON fd.location_id = l.id\s
        WHERE fd.status = 'AVAILABLE'\s
        HAVING distance < :radiusKm\s
        ORDER BY distance ASC
        """, nativeQuery = true)
    List<FoodDonation> findNearbyAvailableFood(
            @Param("ngoLat") double ngoLat,
            @Param("ngoLng") double ngoLng,
            @Param("radiusKm") double radiusKm);

    List<FoodDonation> findByStatus(String status);

    List<FoodDonation> findByDonorOrderByCreatedAtDesc(User donor);

    List<FoodDonation> findByAcceptedByNgoOrderByCreatedAtDesc(User ngo);

    List<FoodDonation> findByDonorAndStatus(User donor, String status);

    long countByStatus(String status);

    long countByDonor(User donor);

    long countByDonorAndStatus(User donor, String status);

    long countByAcceptedByNgo(User ngo);
}