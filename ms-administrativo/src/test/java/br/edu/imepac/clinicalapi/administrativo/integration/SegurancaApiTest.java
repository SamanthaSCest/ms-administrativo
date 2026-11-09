package br.edu.imepac.clinicalapi.administrativo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/medicos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void comTokenRetorna200() throws Exception {
        mockMvc.perform(get("/medicos").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void integracaoSemScopeRetorna403() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/1").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void integracaoComScopePassaDaSeguranca() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/999999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_integracao:pacientes.ler"))))
                .andExpect(status().isNotFound());
    }
}
