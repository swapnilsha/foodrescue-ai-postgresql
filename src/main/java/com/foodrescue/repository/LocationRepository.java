package com.foodrescue.repository;

import com.foodrescue.entity.Location;
import com.foodrescue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    /** Find the location associated with this user (their registered pickup address). */
    Optional<Location> findByUser(User user);
}