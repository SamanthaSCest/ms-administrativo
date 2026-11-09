package br.edu.imepac.clinicalapi.administrativo.integration;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class AutenticacaoTestUtils {

    private AutenticacaoTestUtils() {
    }

    /** Usuário autenticado comum, para os endpoints de administração. */
    public static RequestPostProcessor usuarioAutenticado() {
        return jwt();
    }

    /** Sistema externo com o scope de leitura de pacientes. */
    public static RequestPostProcessor integracaoPacientes() {
        return jwt().authorities(
                new SimpleGrantedAuthority("SCOPE_integracao:pacientes.ler"));
    }
}
