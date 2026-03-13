package com.nsalazar.roleguard.user.application.mapper;

import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for the User domain.
 */
@Mapper(componentModel = "spring")
public interface IUserMapper {

    /**
     * Maps a {@link User} entity to its response DTO.
     * Extracts {@code role.name} from the lazily-loaded role association.
     */
    @Mapping(target = "roleName", source = "role.name")
    UserResponse toResponse(User user);

    /**
     * Maps a list of {@link User} entities to their response DTOs.
     */
    List<UserResponse> toResponseList(List<User> users);

    /**
     * Maps a {@link CreateUserRequest} to a new {@link User} entity.
     * <ul>
     *   <li>{@code password} — ignored; hashed and set by the service.</li>
     *   <li>{@code enabled} — ignored; set by the service to apply the default.</li>
     *   <li>{@code version} — ignored; managed exclusively by Hibernate.</li>
     *   <li>{@code role} — ignored; resolved by the service from {@code roleId}.</li>
     *   <li>{@code createdAt / updatedAt} — ignored; set by Hibernate timestamps.</li>
     * </ul>
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CreateUserRequest request);
}
