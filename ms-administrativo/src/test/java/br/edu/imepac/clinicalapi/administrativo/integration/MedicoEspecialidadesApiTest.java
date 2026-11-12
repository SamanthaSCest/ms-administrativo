package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Médico com várias especialidades e CRM único por UF. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class MedicoEspecialidadesApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;

    @BeforeEach
    void limpar() {
        medicoRepository.deleteAll();
        especialidadeRepository.deleteAll();
    }

    private long especialidade(String nome) throws Exception {
        MvcResult r = mockMvc.perform(post("/especialidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private String medico(String cpf, String crm, String uf, String especialidadeIds) {
        return """
                {"nome":"Maria Souza","cpf":"%s","dataNascimento":"1985-04-20","crm":"%s","ufCrm":"%s",
                 "especialidadeIds":%s,"senha":"senhaForte1"}
                """.formatted(cpf, crm, uf, especialidadeIds);
    }

    @Test
    void medico_comDuasEspecialidades() throws Exception {
        long cardio = especialidade("Cardiologia");
        long clinica = especialidade("Clínica Geral");

        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(medico("12345678901", "1234", "MG", "[" + cardio + "," + clinica + "]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ufCrm").value("MG"))
                .andExpect(jsonPath("$.especialidades", hasSize(2)))
                .andExpect(jsonPath("$.especialidades[0].nome").value("Cardiologia"));
    }

    @Test
    void mesmoCrmEmUfDiferente_permitido_mesmaUf_conflito() throws Exception {
        long cardio = especialidade("Cardiologia");
        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                .content(medico("12345678901", "1234", "MG", "[" + cardio + "]"))).andExpect(status().isCreated());

        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                .content(medico("10987654321", "1234", "SP", "[" + cardio + "]"))).andExpect(status().isCreated());

        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(medico("11111111122", "1234", "MG", "[" + cardio + "]")))
                .andExpect(status().isConflict());
    }

    @Test
    void semEspecialidades_retorna400() throws Exception {
        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(medico("12345678901", "1234", "MG", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("especialidadeIds"));
    }
}
