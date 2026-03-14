package com.nsalazar.roleguard.permission.domain.port.in;

import com.nsalazar.roleguard.permission.application.dto.CreatePermissionRequest;
import com.nsalazar.roleguard.permission.application.dto.PermissionResponse;
import com.nsalazar.roleguard.permission.application.dto.UpdatePermissionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Input port defining all use cases available for the Permission domain.
 * Controllers depend on this interface, never on the concrete service.
 */
public interface IPermissionUseCase {

    /**
     * Creates a new permission. Validates uniqueness of the name before persisting.
     *
     * @param request creation payload
     * @return the persisted permission as a response DTO
     */
    PermissionResponse createPermission(CreatePermissionRequest request);

    /**
     * Retrieves a single permission by its primary key.
     *
     * @param id permission identifier
     * @return permission response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    PermissionResponse getPermissionById(UUID id);

    /**
     * Retrieves a single permission by its unique name.
     *
     * @param name permission name to look up (e.g. "READ_USERS")
     * @return permission response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    PermissionResponse getPermissionByName(String name);

    /**
     * Returns a paginated list of all permissions.
     *
     * @param pageable page number, size and sort criteria
     * @return page of permission response DTOs
     */
    Page<PermissionResponse> getAllPermissions(Pageable pageable);

    /**
     * Partially updates a permission. Only non-null fields in the request are applied.
     * Name uniqueness is re-validated when the name changes.
     *
     * @param id      target permission identifier
     * @param request fields to update
     * @return updated permission response DTO
     */
    PermissionResponse updatePermission(UUID id, UpdatePermissionRequest request);

    /**
     * Deletes a permission by id.
     *
     * @param id target permission identifier
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    void deletePermission(UUID id);
}
