package com.nsalazar.roleguard.role.application.mapper;

import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for the Role domain.
 */
@Mapper(componentModel = "spring")
public interface IRoleMapper {

    /**
     * Maps a {@link Role} entity to its response DTO.
     */
    RoleResponse toResponse(Role role);

    /**
     * Maps a list of {@link Role} entities to their response DTOs.
     */
    List<RoleResponse> toResponseList(List<Role> roles);

    /**
     * Maps a {@link CreateRoleRequest} to a new {@link Role} entity.
     * Fields managed by Hibernate (id, version, createdAt, updatedAt) are intentionally ignored.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Role toEntity(CreateRoleRequest request);
}
