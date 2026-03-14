package com.nsalazar.roleguard.shared.security;

import com.nsalazar.roleguard.user.domain.model.User;
import com.nsalazar.roleguard.user.infrastructure.persistence.IJpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security {@link UserDetailsService} backed by the JPA user repository.
 * <p>
 * Uses {@link IJpaUserRepository#findWithRoleByUsername} to eagerly fetch the
 * user's role in a single query, avoiding {@code LazyInitializationException}
 * when the role name is read outside of an active transaction (e.g. inside
 * {@link JwtAuthFilter}).
 * </p>
 * <p>
 * Role mapping: the raw role name stored in the database (e.g. {@code "ADMIN"}) is
 * used directly as a {@link SimpleGrantedAuthority} value — no {@code ROLE_} prefix
 * is added, matching the {@code hasAuthority("ADMIN")} checks in {@code SecurityConfig}.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final IJpaUserRepository jpaUserRepository;

    /**
     * Loads a user's security details by username.
     *
     * @param username the username to look up
     * @return fully populated {@link UserDetails} with hashed password and authorities
     * @throws UsernameNotFoundException if no user with the given username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading UserDetails for username='{}'", username);

        User user = jpaUserRepository.findWithRoleByUsername(username)
                .orElseThrow(() -> {
                    log.warn("UserDetails not found for username='{}'", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
            user.getRole().getPermissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.getName())));
        }

        log.debug("UserDetails loaded for username='{}' — role='{}', permissions={}",
                username, user.getRole() != null ? user.getRole().getName() : "none",
                authorities.stream().map(SimpleGrantedAuthority::getAuthority).toList());

        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(),
                user.isEnabled(), authorities);
    }
}
