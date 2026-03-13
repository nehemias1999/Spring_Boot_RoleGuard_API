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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

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
                .id(1L)
                .username("john")
                .email("john@example.com")
                .password("$2a$10$hashed")
                .enabled(true)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userResponse = new UserResponse(1L, "john", "john@example.com", null,
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
            Role role = Role.builder().id(2L).name("ROLE_ADMIN").build();
            CreateUserRequest requestWithRole =
                    new CreateUserRequest("john", "john@example.com", "password123", null, 2L);

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(requestWithRole)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.createUser(requestWithRole);

            verify(roleRepository).findById(2L);
            assertThat(user.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when provided roleId does not exist")
        void shouldThrow_whenRoleIdNotFound() {
            CreateUserRequest requestWithRole =
                    new CreateUserRequest("john", "john@example.com", "password123", null, 99L);

            when(userRepository.existsByUsername("john")).thenReturn(false);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(userMapper.toEntity(requestWithRole)).thenReturn(user);
            when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
            when(roleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.createUser(requestWithRole))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role");
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
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            assertThat(userService.getUserById(1L).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
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

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("johnny")).thenReturn(false);
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(
                    new UserResponse(1L, "johnny", "john@example.com", null, true, 0L, null, null));

            assertThat(userService.updateUser(1L, request).username()).isEqualTo("johnny");
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("should re-hash password when new password is provided")
        void shouldHashNewPassword_whenProvided() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, "newpassword123", null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpassword123")).thenReturn("$2a$10$newHash");
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.updateUser(1L, request);

            verify(passwordEncoder).encode("newpassword123");
            assertThat(user.getPassword()).isEqualTo("$2a$10$newHash");
        }

        @Test
        @DisplayName("should disable user when enabled=false is provided")
        void shouldDisableUser_whenEnabledIsFalse() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, false);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(userResponse);

            userService.updateUser(1L, request);

            assertThat(user.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new username is taken")
        void shouldThrow_whenNewUsernameAlreadyExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest("taken", null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("username");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenUserNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, new UpdateUserRequest(null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
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
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user does not exist")
        void shouldThrow_whenNotFound() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository, never()).deleteById(any());
        }
    }
}
