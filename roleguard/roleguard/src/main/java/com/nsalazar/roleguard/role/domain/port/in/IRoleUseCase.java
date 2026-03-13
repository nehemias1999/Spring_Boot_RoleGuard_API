package com.nsalazar.roleguard.role.domain.port.in;

import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.application.dto.UpdateRoleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Input port defining all use cases available for the Role domain.
 * Controllers depend on this interface, never on the concrete service.
 */
public interface IRoleUseCase {

    /**
     * Creates a new role. Validates uniqueness of the name before persisting.
     *
     * @param request creation payload
     * @return the persisted role as a response DTO
     */
    RoleResponse createRole(CreateRoleRequest request);

    /**
     * Retrieves a single role by its primary key.
     *
     * @param id role identifier
     * @return role response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    RoleResponse getRoleById(Long id);

    /**
     * Retrieves a single role by its unique name.
     *
     * @param name role name to look up (e.g. "ADMIN")
     * @return role response DTO
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    RoleResponse getRoleByName(String name);

    /**
     * Returns a paginated list of all roles.
     *
     * @param pageable page number, size and sort criteria
     * @return page of role response DTOs
     */
    Page<RoleResponse> getAllRoles(Pageable pageable);

    /**
     * Partially updates a role. Only non-null fields in the request are applied.
     * Name uniqueness is re-validated when the name changes.
     *
     * @param id      target role identifier
     * @param request fields to update
     * @return updated role response DTO
     */
    RoleResponse updateRole(Long id, UpdateRoleRequest request);

    /**
     * Deletes a role by id.
     * Throws a data integrity error if any users are still assigned to this role.
     *
     * @param id target role identifier
     * @throws com.nsalazar.roleguard.shared.exception.ResourceNotFoundException if not found
     */
    void deleteRole(Long id);
}
