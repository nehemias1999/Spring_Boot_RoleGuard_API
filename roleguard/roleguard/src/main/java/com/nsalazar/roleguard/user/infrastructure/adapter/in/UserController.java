package com.nsalazar.roleguard.user.infrastructure.adapter.in;

import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.domain.port.in.IUserUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST adapter for the User slice.
 * Depends only on {@link IUserUseCase} — the concrete service is never referenced here.
 * All endpoints are currently unsecured; role-based access will be applied
 * after JWT authentication is implemented.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserUseCase userUseCase;

    /**
     * Returns a paginated list of all users.
     *
     * @param page zero-based page index (default 0)
     * @param size number of items per page, max 100 (default 20)
     * @param sort field name to sort by (default "id")
     * @return 200 with paginated user list
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.debug("GET /api/v1/users — page={}, size={}, sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<UserResponse> result = userUseCase.getAllUsers(pageable);
        log.debug("GET /api/v1/users — returning {} user(s), totalElements={}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a single user by id.
     *
     * @param id user identifier
     * @return 200 with user, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.debug("GET /api/v1/users/{} — fetching user", id);
        UserResponse user = userUseCase.getUserById(id);
        log.debug("GET /api/v1/users/{} — found username='{}'", id, user.username());
        return ResponseEntity.ok(user);
    }

    /**
     * Returns a single user by email address.
     *
     * @param email email address to look up
     * @return 200 with user, or 404 if not found
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        log.debug("GET /api/v1/users/email/{} — fetching user", email);
        UserResponse user = userUseCase.getUserByEmail(email);
        log.debug("GET /api/v1/users/email/{} — found id={}", email, user.id());
        return ResponseEntity.ok(user);
    }

    /**
     * Creates a new user.
     *
     * @param request validated creation payload
     * @return 201 with created user, or 400/409
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.debug("POST /api/v1/users — creating user username='{}'", request.username());
        UserResponse created = userUseCase.createUser(request);
        log.debug("POST /api/v1/users — user created with id={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Partially updates an existing user.
     *
     * @param id      target user identifier
     * @param request fields to update (all optional)
     * @return 200 with updated user, or 404/409
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.debug("PUT /api/v1/users/{} — updating user", id);
        UserResponse updated = userUseCase.updateUser(id, request);
        log.debug("PUT /api/v1/users/{} — user updated", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a user by id.
     *
     * @param id target user identifier
     * @return 204 No Content, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.debug("DELETE /api/v1/users/{} — deleting user", id);
        userUseCase.deleteUser(id);
        log.debug("DELETE /api/v1/users/{} — user deleted", id);
        return ResponseEntity.noContent().build();
    }
}
