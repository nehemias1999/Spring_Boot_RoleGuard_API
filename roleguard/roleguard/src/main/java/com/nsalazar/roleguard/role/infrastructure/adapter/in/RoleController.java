package com.nsalazar.roleguard.role.infrastructure.adapter.in;

import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import com.nsalazar.roleguard.role.domain.port.in.IRoleUseCase;
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
 * REST adapter for the Role slice.
 * Depends only on {@link IRoleUseCase} — the concrete service is never referenced here.
 * All endpoints are currently unsecured; role-based access will be applied
 * after JWT authentication is implemented.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final IRoleUseCase roleUseCase;

    /**
     * Returns a paginated list of all roles.
     *
     * @param page zero-based page index (default 0)
     * @param size number of items per page, max 100 (default 20)
     * @param sort field name to sort by (default "id")
     * @return 200 with paginated role list
     */
    @GetMapping
    public ResponseEntity<Page<RoleResponse>> getAllRoles(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.debug("GET /api/v1/roles — page={}, size={}, sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<RoleResponse> result = roleUseCase.getAllRoles(pageable);
        log.debug("GET /api/v1/roles — returning {} role(s), totalElements={}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a single role by id.
     *
     * @param id role identifier
     * @return 200 with role, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        log.debug("GET /api/v1/roles/{} — fetching role", id);
        RoleResponse role = roleUseCase.getRoleById(id);
        log.debug("GET /api/v1/roles/{} — found name='{}'", id, role.name());
        return ResponseEntity.ok(role);
    }

    /**
     * Returns a single role by name.
     *
     * @param name role name (e.g. "ADMIN")
     * @return 200 with role, or 404 if not found
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<RoleResponse> getRoleByName(@PathVariable String name) {
        log.debug("GET /api/v1/roles/name/{} — fetching role", name);
        RoleResponse role = roleUseCase.getRoleByName(name);
        log.debug("GET /api/v1/roles/name/{} — found id={}", name, role.id());
        return ResponseEntity.ok(role);
    }

    /**
     * Creates a new role.
     *
     * @param request validated creation payload
     * @return 201 with created role, or 400/409
     */
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.debug("POST /api/v1/roles — creating role name='{}'", request.name());
        RoleResponse created = roleUseCase.createRole(request);
        log.debug("POST /api/v1/roles — role created with id={}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Partially updates an existing role.
     *
     * @param id      target role identifier
     * @param request fields to update (all optional)
     * @return 200 with updated role, or 404/409
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        log.debug("PUT /api/v1/roles/{} — updating role", id);
        RoleResponse updated = roleUseCase.updateRole(id, request);
        log.debug("PUT /api/v1/roles/{} — role updated to name='{}'", id, updated.name());
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a role by id.
     * Returns 409 if users are still assigned to this role.
     *
     * @param id target role identifier
     * @return 204 No Content, or 404/409
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        log.debug("DELETE /api/v1/roles/{} — deleting role", id);
        roleUseCase.deleteRole(id);
        log.debug("DELETE /api/v1/roles/{} — role deleted", id);
        return ResponseEntity.noContent().build();
    }
}
