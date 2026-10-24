package com.foodrescue.repository;

import com.foodrescue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(String role);
    long countByVerifiedFalseAndRoleNot(String role);
    List<User> findByRole(String role);
    List<User> findByVerifiedFalseAndRoleNot(String role);
}