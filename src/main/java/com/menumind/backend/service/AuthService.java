package com.menumind.backend.service;

import com.menumind.backend.dto.auth.AuthResponse;
import com.menumind.backend.dto.auth.LoginRequest;
import com.menumind.backend.dto.auth.RegisterRequest;
import com.menumind.backend.dto.auth.UserProfileDTO;

/**
 * Authentication Service Contract
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileDTO getCurrentUserProfile(String email);
}
