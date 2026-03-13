package com.nsalazar.roleguard.user.domain.port.in;

import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    UserResponse getUserById(Long id);

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
    UserResponse updateUser(Long id, UpdateUserRequest request);

    /**
     * Deletes a user by id.
     *
     * @param id target user identifier
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    void deleteUser(Long id);
}
