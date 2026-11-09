package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static br.edu.imepac.clinicalapi.administrativo.integration.AutenticacaoTestUtils.integracaoPacientes;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API de integração do paciente com o contexto completo (Spring + JPA + Flyway em H2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PacienteIntegracaoApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;

    private Long pacienteId;

    @BeforeEach
    void preparar() {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        Paciente p = new Paciente();
        p.setNome("Maria Aparecida Souza");
        p.setCpf("12345678909");
        p.setEmail("maria@email.com");
        p.setTelefone("34999998888");
        p.setDataNascimento(LocalDate.of(1988, 4, 12));
        pacienteId = pacienteRepository.save(p).getId();
    }

    @Test
    void resumo_deveVirComCpfMascarado() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/{id}", pacienteId)
                        .with(integracaoPacientes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pacienteId))
                .andExpect(jsonPath("$.nome").value("Maria Aparecida Souza"))
                .andExpect(jsonPath("$.cpfMascarado").value("***.456.789-**"))
                .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    void resumo_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/{id}", 999999)
                        .with(integracaoPacientes()))
                .andExpect(status().isNotFound());
    }

    @Test
    void situacao_existenteEInexistente_sempre200() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/{id}/situacao", pacienteId)
                        .with(integracaoPacientes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.ativo").value(true));

        mockMvc.perform(get("/integracao/pacientes/{id}/situacao", 999999)
                        .with(integracaoPacientes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void contato_deveDevolverEmailETelefone() throws Exception {
        mockMvc.perform(get("/integracao/pacientes/{id}/contato", pacienteId)
                        .with(integracaoPacientes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.telefone").value("34999998888"));
    }
}
