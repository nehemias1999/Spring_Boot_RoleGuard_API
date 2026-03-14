package com.nsalazar.roleguard.shared.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Security helper bean used in {@code @PreAuthorize} SpEL expressions.
 * <p>
 * Referenced as {@code @userSecurity} in expressions, e.g.:
 * <pre>
 *   @PreAuthorize("hasAuthority('USER_UPDATE') or @userSecurity.isSelf(#id, authentication)")
 * </pre>
 * </p>
 */
@Component("userSecurity")
public class UserSecurityService {

    /**
     * Returns {@code true} when the authenticated principal's UUID matches {@code userId}.
     * Requires the principal to be a {@link UserPrincipal}; returns {@code false} for any
     * other principal type (e.g. anonymous or test stubs without an id).
     *
     * @param userId         the target user id from the request
     * @param authentication the current authentication token
     * @return whether the caller is acting on their own account
     */
    public boolean isSelf(UUID userId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getId().equals(userId);
    }
}
