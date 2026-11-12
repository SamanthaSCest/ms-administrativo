package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Convênios e salas (cadastros de apoio do diagrama). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class CadastrosApoioApiTest {

    // CNPJ válido (dígitos verificadores corretos)
    private static final String CNPJ = "11222333000181";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ConvenioRepository convenioRepository;
    @Autowired
    private SalaRepository salaRepository;

    @BeforeEach
    void limpar() {
        convenioRepository.deleteAll();
        salaRepository.deleteAll();
    }

    private String convenioJson(String ans, String cnpj) {
        return """
                {"nome":"Unimed","registroAns":"%s","cnpj":"%s","telefone":"3433330000","email":"contato@unimed.com"}
                """.formatted(ans, cnpj);
    }

    @Test
    void convenio_crudEUnicidade() throws Exception {
        MvcResult result = mockMvc.perform(post("/convenios").contentType(MediaType.APPLICATION_JSON)
                        .content(convenioJson("123456", CNPJ)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registroAns").value("123456"))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Mesmo registro ANS -> 409
        mockMvc.perform(post("/convenios").contentType(MediaType.APPLICATION_JSON)
                        .content(convenioJson("123456", "11444777000161")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/convenios/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(convenioJson("654321", CNPJ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registroAns").value("654321"));

        mockMvc.perform(delete("/convenios/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/convenios/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void convenio_cnpjInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/convenios").contentType(MediaType.APPLICATION_JSON)
                        .content(convenioJson("123456", "11222333000100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cnpj"));
    }

    @Test
    void sala_crudEUnicidade() throws Exception {
        String body = "{\"numero\":\"101\",\"tipo\":\"CONSULTORIO\",\"andar\":\"1º\"}";
        MvcResult result = mockMvc.perform(post("/salas").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CONSULTORIO"))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/salas").contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/salas")).andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(delete("/salas/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    void sala_tipoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"S1\",\"tipo\":\"COZINHA\"}"))
                .andExpect(status().isBadRequest());
        // EXAME deixou de existir: tipos válidos são CONSULTORIO, CIRURGIA e PROCEDIMENTO
        mockMvc.perform(post("/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"S1\",\"tipo\":\"EXAME\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sala_cirurgiaComNumeroAndarEObservacoes() throws Exception {
        mockMvc.perform(post("/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"numero":"C2","tipo":"CIRURGIA","andar":"2º","observacoes":"Possui foco cirúrgico duplo"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("C2"))
                .andExpect(jsonPath("$.tipo").value("CIRURGIA"))
                .andExpect(jsonPath("$.andar").value("2º"))
                .andExpect(jsonPath("$.observacoes").value("Possui foco cirúrgico duplo"));
    }

    @Test
    @WithMockUser(username = "recepcao", roles = "FUNCIONARIO")
    void funcionario_consultaMasNaoAltera() throws Exception {
        mockMvc.perform(get("/salas")).andExpect(status().isOk());
        mockMvc.perform(post("/salas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numero\":\"S1\",\"tipo\":\"PROCEDIMENTO\"}"))
                .andExpect(status().isForbidden());
    }
}
