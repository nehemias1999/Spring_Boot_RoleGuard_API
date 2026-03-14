package com.nsalazar.roleguard.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates Spring Data JPA Auditing.
 * <p>
 * Kept in a dedicated {@code @Configuration} class (rather than on the main
 * {@code @SpringBootApplication} class) so that {@code @WebMvcTest} slices do not
 * accidentally try to initialise JPA infrastructure ({@code jpaAuditingHandler} →
 * {@code jpaMappingContext}) when only the web layer is under test.
 * </p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaAuditingConfig {
}
