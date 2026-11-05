package br.edu.imepac.clinicalapi.administrativo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditoriaConfiguration {

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                HttpServletRequest request = attributes.getRequest();
                if (request.getUserPrincipal() != null) {
                    return Optional.of(request.getUserPrincipal().getName());
                }
            }
            return Optional.empty();
        };
    }
}
