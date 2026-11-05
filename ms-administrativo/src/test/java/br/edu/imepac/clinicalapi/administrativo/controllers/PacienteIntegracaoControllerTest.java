package br.edu.imepac.clinicalapi.administrativo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PacienteIntegracaoControllerTest {

    private static final UUID PACIENTE_ID = UUID.fromString("8f14e45f-ceea-4a7b-9b1e-0c7d2f1a9e11");
    private static final UUID ID_INEXISTENTE = UUID.fromString("2f14e45f-ceea-4a7b-9b1e-0c7d2f1a9e22");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararBanco() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS paciente (
                    id UUID PRIMARY KEY,
                    nome VARCHAR(150) NOT NULL,
                    cpf CHAR(11) NOT NULL,
                    data_nascimento DATE NOT NULL,
                    sexo VARCHAR(15) NOT NULL,
                    email VARCHAR(150),
                    telefone_celular VARCHAR(11),
                    ativo BOOLEAN NOT NULL
                )
                """);
        jdbcTemplate.update("DELETE FROM paciente");
        jdbcTemplate.update(
                """
                        INSERT INTO paciente (id, nome, cpf, data_nascimento, sexo, email, telefone_celular, ativo)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                PACIENTE_ID,
                "Maria Aparecida Souza",
                "12345678901",
                LocalDate.of(1988, 4, 12),
                "FEMININO",
                "maria.souza@email.com",
                "34999998888",
                true
        );
    }

    @Test
    void deveConsultarResumoPersistidoComCpfMascarado() throws Exception {
        mockMvc.perform(get("/api/v1/integracao/pacientes/{id}", PACIENTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PACIENTE_ID.toString()))
                .andExpect(jsonPath("$.nome").value("Maria Aparecida Souza"))
                .andExpect(jsonPath("$.cpfMascarado").value("***.456.789-**"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRetornarSituacaoRealDoPaciente() throws Exception {
        jdbcTemplate.update("UPDATE paciente SET ativo = false WHERE id = ?", PACIENTE_ID);

        mockMvc.perform(get("/api/v1/integracao/pacientes/{id}/situacao", PACIENTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornarSituacaoFalsaParaIdInexistenteSemErro() throws Exception {
        mockMvc.perform(get("/api/v1/integracao/pacientes/{id}/situacao", ID_INEXISTENTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existe").value(false))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRetornarContatoPersistido() throws Exception {
        mockMvc.perform(get("/api/v1/integracao/pacientes/{id}/contato", PACIENTE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Aparecida Souza"))
                .andExpect(jsonPath("$.email").value("maria.souza@email.com"))
                .andExpect(jsonPath("$.telefoneCelular").value("34999998888"));
    }

    @Test
    void deveRetornar404ParaResumoOuContatoInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/integracao/pacientes/{id}", ID_INEXISTENTE))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/integracao/pacientes/{id}/contato", ID_INEXISTENTE))
                .andExpect(status().isNotFound());
    }
}
