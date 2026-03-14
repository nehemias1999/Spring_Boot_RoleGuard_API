package com.nsalazar.roleguard.user.domain.port.in;

import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Input port defining all use cases available for the User domain.
 * Controllers depend on this interface, never on the concrete service.
 */
public interface IUserUseCase {

    /**
     * Registers a new user. Validates uniqueness of username and email,
     * then hashes the password before persistence.
     *
     * @param request creation payload
     * @return the persisted user as a response DTO
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Retrieves a single user by its primary key.
     *
     * @param id user identifier
     * @return user response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    UserResponse getUserById(UUID id);

    /**
     * Retrieves a single user by username.
     *
     * @param username unique username
     * @return user response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    UserResponse getUserByUsername(String username);

    /**
     * Retrieves a single user by email address.
     *
     * @param email email address to look up
     * @return user response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    UserResponse getUserByEmail(String email);

    /**
     * Returns a paginated list of all registered users.
     *
     * @param pageable page number, size and sort criteria
     * @return page of user response DTOs
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Partially updates a user. Only non-null fields in the request are applied.
     * Uniqueness constraints are re-validated when username or email change.
     *
     * @param id      target user identifier
     * @param request fields to update
     * @return updated user response DTO
     */
    UserResponse updateUser(UUID id, UpdateUserRequest request);

    /**
     * Assigns a role to a user, replacing any previously assigned role.
     *
     * @param userId target user identifier
     * @param roleId role to assign
     * @return updated user response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if user or role not found
     */
    UserResponse assignRole(UUID userId, UUID roleId);

    /**
     * Deletes a user by id.
     *
     * @param id target user identifier
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    void deleteUser(UUID id);
}
