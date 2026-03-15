package com.nsalazar.roleguard.user.application.service;

import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.shared.exception.DuplicateResourceException;
import com.nsalazar.roleguard.shared.exception.ResourceNotFoundException;
import com.nsalazar.roleguard.user.application.dto.CreateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UpdateUserRequest;
import com.nsalazar.roleguard.user.application.dto.UserResponse;
import com.nsalazar.roleguard.user.application.mapper.IUserMapper;
import com.nsalazar.roleguard.user.domain.model.User;
import com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    private static final UUID USER_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ROLE_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID UNKNOWN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Mock private IUserRepositoryPort userRepository;
    @Mock private IRoleRepositoryPort roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IUserMapper userMapper;

    @InjectMocks private UserService userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username("john")
                .email("john@example.com")
                .password("$2a$10$hashed")
                .enabled(true)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userResponse = new UserResponse(USER_ID, "john", "john@example.com", null,
                true, 0L, user.getCreatedAt(), user.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createUser()")
    class CreateUser {

        private final CreateUserRequest request =
                new CreateUserRequest("john", "john@example.com", "password123", null, null);

        @Test
        @DisplayName("should persist and return UserResponse when input is valid")
        void shouldCreateUser_whenValidRequest() {
            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.username()).isEqualTo("john");
            assertThat(result.enabled()).isTrue();
            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should default enabled to true when not provided")
        void shouldDefaultEnabledToTrue_whenNotProvided() {
            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.createUser(request);

            assertThat(user.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should create user with enabled=false when explicitly provided")
        void shouldCreateDisabledUser_whenEnabledIsFalse() {
            CreateUserRequest disabledRequest =
                    new CreateUserRequest("john", "john@example.com", "password123", false, null);

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(disabledRequest)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findByName("USER")).thenReturn(Optional.empty());
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.createUser(disabledRequest);

            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when username already exists")
        void shouldThrow_whenUsernameExists() {
            when(userRepository.existsByUsername("john")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void shouldThrow_whenEmailExists() {
            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("email");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should assign role when valid roleId is provided")
        void shouldAssignRole_whenRoleIdProvided() {
            Role role = Role.builder().id(ROLE_ID).name("ROLE_ADMIN").build();
            CreateUserRequest requestWithRole =
                    new CreateUserRequest("john", "john@example.com", "password123", null, ROLE_ID);

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(requestWithRole)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.createUser(requestWithRole);

            verify(roleRepository).findById(ROLE_ID);
            assertThat(user.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when provided roleId does not exist")
        void shouldThrow_whenRoleIdNotFound() {
            CreateUserRequest requestWithRole =
                    new CreateUserRequest("john", "john@example.com", "password123", null, UNKNOWN_ID);

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(requestWithRole)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(requestWithRole))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");
        }

        @Test
        @DisplayName("should auto-assign USER role when no roleId is provided and USER role exists")
        void shouldAutoAssignUserRole_whenUserRoleExists() {
            Role userRole = Role.builder().id(ROLE_ID).name("USER").build();

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.createUser(request);

            assertThat(user.getRole()).isEqualTo(userRole);
        }
    }

    // -------------------------------------------------------------------------
    // getUserById
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("should return UserResponse when user exists")
        void shouldReturnUser_whenExists() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            assertThat(userService.getUserById(USER_ID).id()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenNotFound() {
            when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }
    }

    // -------------------------------------------------------------------------
    // getUserByEmail
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserByEmail()")
    class GetUserByEmail {

        @Test
        @DisplayName("should return UserResponse when email exists")
        void shouldReturnUser_whenEmailExists() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            UserResponse result = userService.getUserByEmail("john@example.com");

            assertThat(result.email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when email not found")
        void shouldThrow_whenEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByEmail("unknown@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("email");
        }
    }

    // -------------------------------------------------------------------------
    // getAllUsers
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("should return paginated user list")
        void shouldReturnPaginatedUsers() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            Page<UserResponse> result = userService.getAllUsers(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).username()).isEqualTo("john");
        }
    }

    // -------------------------------------------------------------------------
    // updateUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("should update only the fields provided in the request")
        void shouldUpdateFields_whenProvided() {
            UpdateUserRequest request = new UpdateUserRequest("johnny", null, null, null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("johnny")).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(
                    new UserResponse(USER_ID, "johnny", "john@example.com", null, true, 0L, null, null));

            assertThat(userService.updateUser(USER_ID, request).username()).isEqualTo("johnny");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("should re-hash password when new password is provided")
        void shouldHashNewPassword_whenProvided() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, "newpassword123", null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$10$newHash");
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.updateUser(USER_ID, request);

            verify(passwordEncoder).encode("newpassword123");
            assertThat(user.getPassword()).isEqualTo("$2a$10$newHash");
        }

        @Test
        @DisplayName("should disable user when enabled=false is provided")
        void shouldDisableUser_whenEnabledIsFalse() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, false);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.updateUser(USER_ID, request);

            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new username is taken")
        void shouldThrow_whenNewUsernameAlreadyExists() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(USER_ID, new UpdateUserRequest("taken", null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(UNKNOWN_ID, new UpdateUserRequest(null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getUserByUsername
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getUserByUsername()")
    class GetUserByUsername {

        @Test
        @DisplayName("should return UserResponse when username exists")
        void shouldReturnUser_whenUsernameExists() {
            when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            assertThat(userService.getUserByUsername("john").username()).isEqualTo("john");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when username not found")
        void shouldThrow_whenUsernameNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserByUsername("ghost"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("username");
        }
    }

    // -------------------------------------------------------------------------
    // assignRole
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("assignRole()")
    class AssignRole {

        private static final UUID ADMIN_ROLE_ID     = UUID.fromString("33333333-3333-3333-3333-333333333333");
        private static final UUID MODERATOR_ROLE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
        private static final UUID SUPPORT_ROLE_ID   = UUID.fromString("55555555-5555-5555-5555-555555555555");

        private final Role userRole      = Role.builder().id(ROLE_ID).name("USER").build();
        private final Role adminRole     = Role.builder().id(ADMIN_ROLE_ID).name("ADMIN").build();
        private final Role moderatorRole = Role.builder().id(MODERATOR_ROLE_ID).name("MODERATOR").build();
        private final Role supportRole   = Role.builder().id(SUPPORT_ROLE_ID).name("SUPPORT").build();

        @AfterEach
        void clearSecurityContext() {
            SecurityContextHolder.clearContext();
        }

        private void loginAs(String username, String roleName) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            username, null, List.of(new SimpleGrantedAuthority(roleName))));
        }

        @Test
        @DisplayName("ADMIN should assign any role to another user")
        void adminShouldAssignRole_whenTargetIsNotSelf() {
            loginAs("admin", "ADMIN");
            UserResponse updatedResponse = new UserResponse(USER_ID, "john", "john@example.com", "MODERATOR",
                    true, 1L, user.getCreatedAt(), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(MODERATOR_ROLE_ID)).thenReturn(Optional.of(moderatorRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(updatedResponse);

            UserResponse result = userService.assignRole(USER_ID, MODERATOR_ROLE_ID);

            assertThat(result.roleName()).isEqualTo("MODERATOR");
            assertThat(user.getRole()).isEqualTo(moderatorRole);
        }

        @Test
        @DisplayName("ADMIN should not change own role")
        void adminShouldThrow_whenChangingOwnRole() {
            loginAs("john", "ADMIN");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(MODERATOR_ROLE_ID)).thenReturn(Optional.of(moderatorRole));

            assertThatThrownBy(() -> userService.assignRole(USER_ID, MODERATOR_ROLE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own role");
        }

        @Test
        @DisplayName("MODERATOR should promote USER to MODERATOR")
        void moderatorShouldAssignRole_userToModerator() {
            loginAs("mod1", "MODERATOR");
            user.setRole(userRole);
            UserResponse updatedResponse = new UserResponse(USER_ID, "john", "john@example.com", "MODERATOR",
                    true, 1L, user.getCreatedAt(), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(MODERATOR_ROLE_ID)).thenReturn(Optional.of(moderatorRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(updatedResponse);

            UserResponse result = userService.assignRole(USER_ID, MODERATOR_ROLE_ID);

            assertThat(result.roleName()).isEqualTo("MODERATOR");
        }

        @Test
        @DisplayName("MODERATOR should promote USER to SUPPORT")
        void moderatorShouldAssignRole_userToSupport() {
            loginAs("mod1", "MODERATOR");
            user.setRole(userRole);
            UserResponse updatedResponse = new UserResponse(USER_ID, "john", "john@example.com", "SUPPORT",
                    true, 1L, user.getCreatedAt(), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(SUPPORT_ROLE_ID)).thenReturn(Optional.of(supportRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(updatedResponse);

            UserResponse result = userService.assignRole(USER_ID, SUPPORT_ROLE_ID);

            assertThat(result.roleName()).isEqualTo("SUPPORT");
        }

        @Test
        @DisplayName("MODERATOR should demote SUPPORT to USER")
        void moderatorShouldAssignRole_supportToUser() {
            loginAs("mod1", "MODERATOR");
            user.setRole(supportRole);
            UserResponse updatedResponse = new UserResponse(USER_ID, "john", "john@example.com", "USER",
                    true, 1L, user.getCreatedAt(), null);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(userRole));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(updatedResponse);

            UserResponse result = userService.assignRole(USER_ID, ROLE_ID);

            assertThat(result.roleName()).isEqualTo("USER");
        }

        @Test
        @DisplayName("MODERATOR should not assign ADMIN role to a USER")
        void moderatorShouldThrow_whenAssigningAdminToUser() {
            loginAs("mod1", "MODERATOR");
            user.setRole(userRole);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(ADMIN_ROLE_ID)).thenReturn(Optional.of(adminRole));

            assertThatThrownBy(() -> userService.assignRole(USER_ID, ADMIN_ROLE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("MODERATOR cannot assign");
        }

        @Test
        @DisplayName("MODERATOR should not change role of another MODERATOR")
        void moderatorShouldThrow_whenTargetIsAlsoModerator() {
            loginAs("mod1", "MODERATOR");
            user.setRole(moderatorRole);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(userRole));

            assertThatThrownBy(() -> userService.assignRole(USER_ID, ROLE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("MODERATOR cannot assign");
        }

        @Test
        @DisplayName("MODERATOR should not change own role")
        void moderatorShouldThrow_whenChangingOwnRole() {
            loginAs("john", "MODERATOR");
            user.setRole(moderatorRole);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(userRole));

            assertThatThrownBy(() -> userService.assignRole(USER_ID, ROLE_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own role");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignRole(UNKNOWN_ID, ROLE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when role does not exist")
        void shouldThrow_whenRoleNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(roleRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignRole(USER_ID, UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");
        }
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("should delete user when it exists")
        void shouldDelete_whenExists() {
            when(userRepository.existsById(USER_ID)).thenReturn(true);

            userService.deleteUser(USER_ID);

            verify(userRepository).deleteById(USER_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenNotFound() {
            when(userRepository.existsById(UNKNOWN_ID)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(UNKNOWN_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository, never()).deleteById(any());
        }
    }
}
