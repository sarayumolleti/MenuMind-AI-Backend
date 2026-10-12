package com.menumind.backend.service;

import com.menumind.backend.dto.user.CreateUserRequest;
import com.menumind.backend.dto.user.UpdateProfileRequest;
import com.menumind.backend.dto.user.UpdateUserRequest;
import com.menumind.backend.dto.user.UserResponse;
import com.menumind.backend.entity.Role;
import com.menumind.backend.security.UserPrincipal;

import java.util.List;

/**
 * User Service Contract
 */
public interface UserService {

    List<UserResponse> getAllUsers(Role roleFilter);

    UserResponse getUserById(Long id);

    UserResponse createUser(CreateUserRequest request, UserPrincipal currentUser);

    UserResponse updateUser(Long id, UpdateUserRequest request, UserPrincipal currentUser);

    UserResponse toggleUserStatus(Long id, Boolean isActive);

    UserResponse updateProfile(String email, UpdateProfileRequest request);
}
