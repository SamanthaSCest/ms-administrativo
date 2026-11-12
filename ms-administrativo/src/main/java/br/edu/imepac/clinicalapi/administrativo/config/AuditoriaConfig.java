package br.edu.imepac.clinicalapi.administrativo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Auditoria das entidades (EntidadeBase.criadoPor / atualizadoPor): grava o login de quem fez a operação
 * (claim "sub" do JWT, ou "servico" para chamadas com X-Api-Key). Sem usuário autenticado: "sistema".
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditoriaConfig {

    public static final String SISTEMA = "sistema";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return Optional.of(SISTEMA);
            }
            return Optional.of(auth.getName());
        };
    }
}
