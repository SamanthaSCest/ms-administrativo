package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sobe o contexto completo (Spring + JPA + Flyway em H2) e exercita a API via HTTP simulado. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdministrativoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;
    @Autowired
    private PacienteRepository pacienteRepository;

    @BeforeEach
    void limpar() {
        medicoRepository.deleteAll();
        especialidadeRepository.deleteAll();
        pacienteRepository.deleteAll();
    }

    private long criarEspecialidade(String nome) throws Exception {
        MvcResult result = mockMvc.perform(post("/especialidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"descricao\":\"d\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private String medicoJson(long especialidadeId, String cpf, String crm) {
        return """
                {"nome":"Maria Souza","cpf":"%s","email":"maria@clinica.com","telefone":"18999999999",
                 "dataNascimento":"1985-04-20","crm":"%s","especialidadeId":%d}
                """.formatted(cpf, crm, especialidadeId);
    }

    @Test
    void especialidade_crudCompleto() throws Exception {
        long id = criarEspecialidade("Cardiologia");

        mockMvc.perform(get("/especialidades/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cardiologia"))
                .andExpect(jsonPath("$.dataCriacao").exists());

        mockMvc.perform(put("/especialidades/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cardio\",\"descricao\":\"nova\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Cardio"));

        mockMvc.perform(get("/especialidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(delete("/especialidades/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/especialidades/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/especialidades/" + id));
    }

    @Test
    void especialidade_nomeDuplicado_retorna409() throws Exception {
        criarEspecialidade("Cardiologia");

        mockMvc.perform(post("/especialidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"cardiologia\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void especialidade_nomeEmBranco_retorna400ComDetalhes() throws Exception {
        mockMvc.perform(post("/especialidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("nome"));
    }

    @Test
    void medico_criaEBuscaComEspecialidade() throws Exception {
        long espId = criarEspecialidade("Cardiologia");

        MvcResult result = mockMvc.perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "12345678901", "CRM-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.especialidade.nome").value("Cardiologia"))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/medicos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.crm").value("CRM-1"))
                .andExpect(jsonPath("$.especialidade.id").value(espId));
    }

    @Test
    void medico_cpfInvalido_retorna400() throws Exception {
        long espId = criarEspecialidade("Cardiologia");

        mockMvc.perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "123", "CRM-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("cpf"));
    }

    @Test
    void medico_especialidadeInexistente_retorna422() throws Exception {
        mockMvc.perform(post("/medicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(9999, "12345678901", "CRM-1")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void medico_crmDuplicado_retorna409() throws Exception {
        long espId = criarEspecialidade("Cardiologia");
        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                .content(medicoJson(espId, "12345678901", "CRM-1"))).andExpect(status().isCreated());

        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "10987654321", "CRM-1")))
                .andExpect(status().isConflict());
    }

    @Test
    void especialidade_comMedico_naoPodeSerRemovida() throws Exception {
        long espId = criarEspecialidade("Cardiologia");
        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                .content(medicoJson(espId, "12345678901", "CRM-1"))).andExpect(status().isCreated());

        mockMvc.perform(delete("/especialidades/{id}", espId)).andExpect(status().isConflict());
    }

    @Test
    void paciente_crudBasico() throws Exception {
        String body = "{\"nome\":\"João Pereira\",\"cpf\":\"98765432100\",\"dataNascimento\":\"1990-01-15\"}";

        MvcResult result = mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(id).isPositive();

        mockMvc.perform(get("/pacientes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("98765432100"));

        mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/pacientes/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void jsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idNaoNumerico_retorna400() throws Exception {
        mockMvc.perform(get("/pacientes/abc")).andExpect(status().isBadRequest());
    }
}
