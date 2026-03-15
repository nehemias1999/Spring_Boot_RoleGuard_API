package com.nsalazar.roleguard.shared.config;

import com.nsalazar.roleguard.permission.domain.model.Permission;
import com.nsalazar.roleguard.permission.domain.port.out.IPermissionRepositoryPort;
import com.nsalazar.roleguard.role.domain.model.Role;
import com.nsalazar.roleguard.role.domain.port.out.IRoleRepositoryPort;
import com.nsalazar.roleguard.user.domain.model.User;
import com.nsalazar.roleguard.user.domain.port.out.IUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Seeds the database with default permissions, roles, and the built-in ADMIN user on first startup.
 * All operations are idempotent — if the data already exists it is left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final List<String> DEFAULT_ROLES = List.of("USER", "ADMIN", "MODERATOR", "SUPPORT");
    private static final List<String> DEFAULT_PERMISSIONS = List.of(
            "USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE", "USER_ASSIGN",
            "ROLE_READ", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE", "ROLE_ASSIGN",
            "PERMISSION_READ", "PERMISSION_CREATE", "PERMISSION_UPDATE", "PERMISSION_DELETE", "PERMISSION_ASSIGN"
    );
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN",     Set.of("USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE", "USER_ASSIGN",
                                "ROLE_READ", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE", "ROLE_ASSIGN",
                                "PERMISSION_READ", "PERMISSION_CREATE", "PERMISSION_UPDATE", "PERMISSION_DELETE", "PERMISSION_ASSIGN"),
            "MODERATOR", Set.of("USER_READ", "USER_CREATE", "USER_UPDATE", "USER_DELETE", "USER_ASSIGN",
                                "ROLE_READ", "ROLE_ASSIGN",
                                "PERMISSION_READ"),
            "SUPPORT",   Set.of("USER_READ", "ROLE_READ", "PERMISSION_READ"),
            "USER",      Set.of("USER_READ", "ROLE_READ", "PERMISSION_READ")
    );
    private static final String ADMIN_USERNAME = "ADMIN";
    private static final String ADMIN_PASSWORD = "ADMIN";

    private final IRoleRepositoryPort roleRepository;
    private final IPermissionRepositoryPort permissionRepository;
    private final IUserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedRolePermissions();
        seedAdminUser();
    }

    private void seedPermissions() {
        for (String name : DEFAULT_PERMISSIONS) {
            if (!permissionRepository.existsByName(name)) {
                permissionRepository.save(Permission.builder().name(name).build());
                log.info("DataInitializer — permission '{}' created", name);
            } else {
                log.debug("DataInitializer — permission '{}' already exists, skipping", name);
            }
        }
    }

    private void seedRoles() {
        for (String name : DEFAULT_ROLES) {
            if (!roleRepository.existsByName(name)) {
                roleRepository.save(Role.builder().name(name).build());
                log.info("DataInitializer — role '{}' created", name);
            } else {
                log.debug("DataInitializer — role '{}' already exists, skipping", name);
            }
        }
    }

    private void seedRolePermissions() {
        ROLE_PERMISSIONS.forEach((roleName, permissionNames) -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role '" + roleName + "' not found after seeding"));

            permissionNames.forEach(permName -> {
                Permission permission = permissionRepository.findByName(permName)
                        .orElseThrow(() -> new IllegalStateException("Permission '" + permName + "' not found after seeding"));

                if (role.getPermissions().add(permission)) {
                    log.info("DataInitializer — permission '{}' assigned to role '{}'", permName, roleName);
                } else {
                    log.debug("DataInitializer — permission '{}' already assigned to role '{}', skipping", permName, roleName);
                }
            });

            roleRepository.save(role);
        });
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            log.debug("DataInitializer — user '{}' already exists, skipping", ADMIN_USERNAME);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found after seeding"));

        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .email("admin@roleguard.internal")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .enabled(true)
                .role(adminRole)
                .build();

        userRepository.save(admin);
        log.info("DataInitializer — default ADMIN user created");
    }
}
