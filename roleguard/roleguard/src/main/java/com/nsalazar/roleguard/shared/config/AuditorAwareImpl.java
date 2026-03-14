package com.nsalazar.roleguard.shared.config;

import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides the current authenticated username to Spring Data JPA Auditing.
 * <p>
 * Used to populate {@code @CreatedBy} and {@code @LastModifiedBy} fields on entities.
 * Falls back to {@code "system"} for operations executed outside a security context
 * (e.g. database migrations, scheduled tasks).
 * </p>
 */
@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public @NonNull Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.of("system");
        }
        return Optional.of(auth.getName());
    }
}
