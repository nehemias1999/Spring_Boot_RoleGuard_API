package com.nsalazar.roleguard.permission.infrastructure.adapter.in;

import com.nsalazar.roleguard.permission.application.dto.CreatePermissionRequest;
import com.nsalazar.roleguard.permission.application.dto.PermissionResponse;
import com.nsalazar.roleguard.permission.application.dto.UpdatePermissionRequest;
import com.nsalazar.roleguard.permission.domain.port.in.IPermissionUseCase;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST adapter for the Permission slice.
 * Depends only on {@link IPermissionUseCase} — the concrete service is never referenced here.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final IPermissionUseCase permissionUseCase;

    /**
     * Returns a paginated list of all permissions.
     *
     * @param page zero-based page index (default 0)
     * @param size number of items per page, max 100 (default 20)
     * @param sort field name to sort by (default "id")
     * @return 200 with paginated permission list
     */
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @GetMapping
    public ResponseEntity<Page<PermissionResponse>> getAllPermissions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.debug("GET /api/v1/permissions — page={}, size={}, sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<PermissionResponse> result = permissionUseCase.getAllPermissions(pageable);
        log.debug("GET /api/v1/permissions — returning {} permission(s)", result.getNumberOfElements());
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a single permission by id.
     *
     * @param id permission identifier
     * @return 200 with permission, or 404 if not found
     */
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponse> getPermissionById(@PathVariable UUID id) {
        log.debug("GET /api/v1/permissions/{} — fetching permission", id);
        PermissionResponse permission = permissionUseCase.getPermissionById(id);
        log.debug("GET /api/v1/permissions/{} — found name='{}'", id, permission.name());
        return ResponseEntity.ok(permission);
    }

    /**
     * Returns a single permission by name.
     *
     * @param name permission name (e.g. "READ_USERS")
     * @return 200 with permission, or 404 if not found
     */
    @PreAuthorize("hasAuthority('PERMISSION_READ')")
    @GetMapping("/name/{name}")
    public ResponseEntity<PermissionResponse> getPermissionByName(@PathVariable String name) {
        log.debug("GET /api/v1/permissions/name/{} — fetching permission", name);
        PermissionResponse permission = permissionUseCase.getPermissionByName(name);
        log.debug("GET /api/v1/permissions/name/{} — found id={}", name, permission.id());
        return ResponseEntity.ok(permission);
    }

    /**
     * Creates a new permission. Requires ADMIN authority.
     *
     * @param request validated creation payload
     * @return 201 with created permission, or 400/409
     */
    @PreAuthorize("hasAuthority('PERMISSION_CREATE')")
    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        log.debug("POST /api/v1/permissions — creating permission name='{}'", request.name());
        PermissionResponse created = permissionUseCase.createPermission(request);
        log.debug("POST /api/v1/permissions — permission created with id={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Partially updates an existing permission. Requires ADMIN authority.
     *
     * @param id      target permission identifier
     * @param request fields to update (all optional)
     * @return 200 with updated permission, or 404/409
     */
    @PreAuthorize("hasAuthority('PERMISSION_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        log.debug("PUT /api/v1/permissions/{} — updating permission", id);
        PermissionResponse updated = permissionUseCase.updatePermission(id, request);
        log.debug("PUT /api/v1/permissions/{} — permission updated to name='{}'", id, updated.name());
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a permission by id. Requires ADMIN authority.
     *
     * @param id target permission identifier
     * @return 204 No Content, or 404 if not found
     */
    @PreAuthorize("hasAuthority('PERMISSION_DELETE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        log.debug("DELETE /api/v1/permissions/{} — deleting permission", id);
        permissionUseCase.deletePermission(id);
        log.debug("DELETE /api/v1/permissions/{} — permission deleted", id);
        return ResponseEntity.noContent().build();
    }
}
