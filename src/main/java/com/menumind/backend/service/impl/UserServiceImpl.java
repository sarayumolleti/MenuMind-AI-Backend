package com.menumind.backend.service.impl;

import com.menumind.backend.dto.user.CreateUserRequest;
import com.menumind.backend.dto.user.UpdateProfileRequest;
import com.menumind.backend.dto.user.UpdateUserRequest;
import com.menumind.backend.dto.user.UserResponse;
import com.menumind.backend.entity.Role;
import com.menumind.backend.entity.User;
import com.menumind.backend.exception.BadRequestException;
import com.menumind.backend.exception.ResourceNotFoundException;
import com.menumind.backend.repository.UserRepository;
import com.menumind.backend.security.UserPrincipal;
import com.menumind.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(Role roleFilter) {
        List<User> users;
        if (roleFilter != null) {
            users = userRepository.findByRole(roleFilter);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, UserPrincipal currentUser) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with email '" + request.getEmail() + "' already exists.");
        }

        // Role authorization rule: Managers can only create Staff members
        if (currentUser.getRole() == Role.ROLE_MANAGER && request.getRole() != Role.ROLE_STAFF) {
            throw new BadRequestException("Restaurant Managers are only permitted to create Staff accounts.");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .role(request.getRole())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully: {} by {}", savedUser.getEmail(), currentUser.getEmail());

        return UserResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Managers cannot modify Admins or other Managers
        if (currentUser.getRole() == Role.ROLE_MANAGER &&
                (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_MANAGER)) {
            throw new BadRequestException("Managers do not have permission to modify this user account.");
        }

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());

        // Only Admin can change another user's role
        if (request.getRole() != null && currentUser.getRole() == Role.ROLE_ADMIN) {
            user.setRole(request.getRole());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long id, Boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);
        log.info("Updated active status for user {}: {}", user.getEmail(), isActive);

        return UserResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());

        User updatedUser = userRepository.save(user);
        return UserResponse.fromEntity(updatedUser);
    }
}
