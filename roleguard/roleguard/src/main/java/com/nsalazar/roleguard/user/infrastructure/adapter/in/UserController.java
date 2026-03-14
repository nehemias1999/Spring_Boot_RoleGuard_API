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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST adapter for the User slice.
 * Depends only on {@link IUserUseCase} — the concrete service is never referenced here.
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
    @PreAuthorize("hasAuthority('USER_READ')")
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
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
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
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        log.debug("GET /api/v1/users/email/{} — fetching user", email);
        UserResponse user = userUseCase.getUserByEmail(email);
        log.debug("GET /api/v1/users/email/{} — found id={}", email, user.id());
        return ResponseEntity.ok(user);
    }

    /**
     * Creates a new user. Requires {@code USER_CREATE} authority.
     * Intended for admin-initiated user creation with optional role assignment.
     *
     * @param request validated creation payload
     * @return 201 with created user, or 400/409
     */
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.debug("POST /api/v1/users — creating user username='{}'", request.username());
        UserResponse created = userUseCase.createUser(request);
        log.debug("POST /api/v1/users — user created id={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Partially updates an existing user.
     * Allowed when the caller has {@code USER_UPDATE} authority <em>or</em> is updating their own account.
     * The {@code enabled} field may only be changed by a caller with {@code USER_UPDATE} authority.
     *
     * @param id             target user identifier
     * @param request        fields to update (all optional)
     * @param authentication current authentication (injected by Spring Security)
     * @return 200 with updated user, or 403/404/409
     */
    @PreAuthorize("hasAuthority('USER_UPDATE') or @userSecurity.isSelf(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {
        if (request.enabled() != null) {
            boolean hasUpdateAuthority = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("USER_UPDATE"));
            if (!hasUpdateAuthority) {
                throw new AccessDeniedException("Changing 'enabled' requires USER_UPDATE authority");
            }
        }
        log.debug("PUT /api/v1/users/{} — updating user", id);
        UserResponse updated = userUseCase.updateUser(id, request);
        log.debug("PUT /api/v1/users/{} — user updated", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Assigns a role to a user. Requires ADMIN authority.
     *
     * @param id     target user identifier
     * @param roleId role to assign
     * @return 200 with updated user, or 404 if user or role not found
     */
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @PutMapping("/{id}/roles/{roleId}")
    public ResponseEntity<UserResponse> assignRole(
            @PathVariable UUID id,
            @PathVariable UUID roleId) {
        log.debug("PUT /api/v1/users/{}/roles/{} — assigning role", id, roleId);
        UserResponse updated = userUseCase.assignRole(id, roleId);
        log.debug("PUT /api/v1/users/{}/roles/{} — role assigned", id, roleId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a user by id. Requires ADMIN authority.
     *
     * @param id target user identifier
     * @return 204 No Content, or 404 if not found
     */
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/users/{} — deleting user", id);
        userUseCase.deleteUser(id);
        log.debug("DELETE /api/v1/users/{} — user deleted", id);
        return ResponseEntity.noContent().build();
    }
}
