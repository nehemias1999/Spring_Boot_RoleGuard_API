package com.nsalazar.roleguard.permission.application.mapper;

import com.nsalazar.roleguard.permission.application.dto.CreatePermissionRequest;
import com.nsalazar.roleguard.permission.application.dto.PermissionResponse;
import com.nsalazar.roleguard.permission.domain.model.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

/**
 * MapStruct mapper for the Permission domain.
 */
@Mapper(componentModel = "spring")
public interface IPermissionMapper {

    /**
     * Maps a {@link Permission} entity to its response DTO.
     */
    PermissionResponse toResponse(Permission permission);

    /**
     * Maps a list of {@link Permission} entities to their response DTOs.
     */
    List<PermissionResponse> toResponseList(List<Permission> permissions);

    /**
     * Maps a set of {@link Permission} entities to a list of response DTOs.
     * Used by {@link com.nsalazar.roleguard.role.application.mapper.IRoleMapper} to map
     * the Role's permissions collection.
     */
    List<PermissionResponse> setToResponseList(Set<Permission> permissions);

    /**
     * Maps a {@link CreatePermissionRequest} to a new {@link Permission} entity.
     * Fields managed by Hibernate (id, version, createdAt, updatedAt, roles) are ignored.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    Permission toEntity(CreatePermissionRequest request);
}
