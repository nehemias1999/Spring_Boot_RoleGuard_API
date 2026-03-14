package com.nsalazar.roleguard.role.application.mapper;

import com.nsalazar.roleguard.permission.application.mapper.IPermissionMapper;
import com.nsalazar.roleguard.role.application.dto.CreateRoleRequest;
import com.nsalazar.roleguard.role.application.dto.RoleResponse;
import com.nsalazar.roleguard.role.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for the Role domain.
 * Uses {@link IPermissionMapper} to convert the Role's permissions set to a list of DTOs.
 */
@Mapper(componentModel = "spring", uses = {IPermissionMapper.class})
public interface IRoleMapper {

    /**
     * Maps a {@link Role} entity to its response DTO, including its permissions.
     */
    @Mapping(target = "permissions", source = "permissions")
    RoleResponse toResponse(Role role);

    /**
     * Maps a list of {@link Role} entities to their response DTOs.
     */
    List<RoleResponse> toResponseList(List<Role> roles);

    /**
     * Maps a {@link CreateRoleRequest} to a new {@link Role} entity.
     * Fields managed by Hibernate (id, version, createdAt, updatedAt, permissions) are ignored.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    Role toEntity(CreateRoleRequest request);
}
