package com.menumind.backend.dto.auth;

import com.menumind.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response Schema
 * 
 * Returned to the client upon successful registration or login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;

    private String email;

    private String fullName;

    private Role role;

    private Long expiresIn;
}
