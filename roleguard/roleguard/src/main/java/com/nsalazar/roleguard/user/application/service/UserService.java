package com.nsalazar.roleguard.user.application.service;

import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.application.mapper.IUserMapper;
import com.nsalazar.roleguard.user.domain.model.User;
import com.nsalazar.roleguard.user.domain.port.in.IUserUseCase;
import com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Application service implementing all User use cases.
 * Orchestrates domain rules, persistence, role lookup, and password hashing.
 * Never exposes JPA entities outside this layer — all outputs are DTOs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements IUserUseCase {

    private static final Set<String> ROLE_NAMES = Set.of("ADMIN", "MODERATOR", "USER", "SUPPORT");

    private final IUserRepositoryPort userRepository;
    private final IRoleRepositoryPort roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IUserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.debug("Creating user with username='{}', email='{}'", request.username(), request.email());

        validateUsernameUniqueness(request.username());
        validateEmailUniqueness(request.email());

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled() != null ? request.enabled() : Boolean.TRUE);

        if (request.roleId() != null) {
            Role role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.roleId()));
            user.setRole(role);
        } else {
            roleRepository.findByName("USER").ifPresent(user::setRole);
        }

        User saved = userRepository.save(user);
        log.info("User created — id={}, username='{}'", saved.getId(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.debug("Fetching user id={}", id);
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.debug("Fetching user by username='{}'", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email='{}'", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users — page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.debug("Updating user id={}", id);

        User user = findUserOrThrow(id);

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            validateUsernameUniqueness(request.username());
            user.setUsername(request.username());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            validateEmailUniqueness(request.email());
            user.setEmail(request.email());
        }

        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
            log.debug("User id={} enabled set to {}", id, request.enabled());
        }

        User updated = userRepository.save(user);
        log.info("User updated — id={}", updated.getId());
        return userMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public UserResponse assignRole(UUID userId, UUID roleId) {
        log.debug("Assigning role id={} to user id={}", roleId, userId);
        User targetUser = findUserOrThrow(userId);
        Role newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String callerUsername = auth.getName();
        String callerRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(ROLE_NAMES::contains)
                .findFirst()
                .orElse(null);

        if (targetUser.getUsername().equals(callerUsername)) {
            throw new AccessDeniedException("Cannot change your own role");
        }

        if ("MODERATOR".equals(callerRole)) {
            validateModeratorRoleAssignment(targetUser, newRole);
        }

        targetUser.setRole(newRole);
        User updated = userRepository.save(targetUser);
        log.info("Role '{}' assigned to user id={} by '{}'", newRole.getName(), userId, callerUsername);
        return userMapper.toResponse(updated);
    }

    private void validateModeratorRoleAssignment(User targetUser, Role newRole) {
        String currentRoleName = targetUser.getRole() != null ? targetUser.getRole().getName() : null;
        String newRoleName = newRole.getName();

        boolean allowed =
                ("USER".equals(currentRoleName) && ("MODERATOR".equals(newRoleName) || "SUPPORT".equals(newRoleName)))
                || ("SUPPORT".equals(currentRoleName) && "USER".equals(newRoleName));

        if (!allowed) {
            throw new AccessDeniedException(
                    "MODERATOR cannot assign role '" + newRoleName
                    + "' to user with role '" + currentRoleName + "'");
        }
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.debug("Deleting user id={}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        userRepository.deleteById(id);
        log.info("User deleted — id={}", id);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private void validateUsernameUniqueness(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User", "username", username);
        }
    }

    private void validateEmailUniqueness(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }
    }
}
