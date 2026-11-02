package br.com.clinica.ms_administrativo.comum;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaConfigurationTest {

    private final AuditoriaConfiguration configuration = new AuditoriaConfiguration();

    @AfterEach
    void limparContexto() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void deveUsarPrincipalDaRequisicaoComoAuditor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal((Principal) () -> "kennedy");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("kennedy", configuration.auditorAware().getCurrentAuditor().orElseThrow());
    }

    @Test
    void deveDeixarAuditorVazioQuandoNaoHaPrincipalAutenticado() {
        assertTrue(configuration.auditorAware().getCurrentAuditor().isEmpty());
    }
}
