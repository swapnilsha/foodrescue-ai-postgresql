package com.foodrescue.service;

import com.foodrescue.dto.AuthRequest;
import com.foodrescue.dto.AuthResponse;
import com.foodrescue.dto.ProfileUpdateRequest;
import com.foodrescue.dto.RegisterRequest;
import com.foodrescue.entity.User;

import java.util.Map;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);

    /** Update the authenticated user's own profile fields. */
    Map<String, Object> updateProfile(User currentUser, ProfileUpdateRequest request);

    /** Change the authenticated user's password after verifying the current one. */
    void changePassword(User currentUser, String currentPassword, String newPassword);
}