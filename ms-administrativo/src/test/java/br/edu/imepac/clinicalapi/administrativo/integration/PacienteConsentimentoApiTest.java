package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Consentimento LGPD do paciente (RF26). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "recepcao", roles = "FUNCIONARIO")
class PacienteConsentimentoApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private ResponsavelLegalRepository responsavelRepository;

    @BeforeEach
    void limpar() {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        responsavelRepository.deleteAll();
    }

    private static String json(String consentimento) {
        return """
                {"nome":"Ana Lima","cpf":"12345678909","dataNascimento":"1990-01-01","telefone":"34999990000"%s}
                """.formatted(consentimento.isEmpty() ? "" : "," + consentimento);
    }

    private ResultActions criar(String consentimento) throws Exception {
        return mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON).content(json(consentimento)));
    }

    private JsonNode atualizar(long id, String consentimento) throws Exception {
        String body = mockMvc.perform(put("/pacientes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(consentimento)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static JsonNode ler(ResultActions r) throws Exception {
        return new ObjectMapper().readTree(r.andReturn().getResponse().getContentAsString());
    }

    @Test
    void comConsentimento_registraVersaoEData() throws Exception {
        criar("\"consentimentoLgpd\":true,\"versaoTermoLgpd\":\"1.0\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentimentoLgpd").value(true))
                .andExpect(jsonPath("$.versaoTermoLgpd").value("1.0"))
                .andExpect(jsonPath("$.dataConsentimentoLgpd").value(notNullValue()));
    }

    @Test
    void semConsentimento_camposVazios() throws Exception {
        criar("")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentimentoLgpd").value(false))
                .andExpect(jsonPath("$.versaoTermoLgpd").value(nullValue()))
                .andExpect(jsonPath("$.dataConsentimentoLgpd").value(nullValue()));
    }

    @Test
    void consentimentoSemVersao_retorna400() throws Exception {
        criar("\"consentimentoLgpd\":true")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("versaoTermoLgpdInformada"));
        criar("\"consentimentoLgpd\":true,\"versaoTermoLgpd\":\"  \"").andExpect(status().isBadRequest());
    }

    @Test
    void edicao_mesmaVersaoMantemData_versaoNovaRenova_retirarLimpa() throws Exception {
        JsonNode criado = ler(criar("\"consentimentoLgpd\":true,\"versaoTermoLgpd\":\"1.0\"").andExpect(status().isCreated()));
        long id = criado.get("id").asLong();
        String dataOriginal = criado.get("dataConsentimentoLgpd").asText();

        JsonNode mesmaVersao = atualizar(id, "\"consentimentoLgpd\":true,\"versaoTermoLgpd\":\"1.0\"");
        assertThat(mesmaVersao.get("dataConsentimentoLgpd").asText()).isEqualTo(dataOriginal);

        JsonNode versaoNova = atualizar(id, "\"consentimentoLgpd\":true,\"versaoTermoLgpd\":\"2.0\"");
        assertThat(versaoNova.get("versaoTermoLgpd").asText()).isEqualTo("2.0");
        assertThat(versaoNova.get("dataConsentimentoLgpd").asText()).isGreaterThanOrEqualTo(dataOriginal);

        JsonNode retirado = atualizar(id, "\"consentimentoLgpd\":false");
        assertThat(retirado.get("consentimentoLgpd").asBoolean()).isFalse();
        assertThat(retirado.get("versaoTermoLgpd").isNull()).isTrue();
        assertThat(retirado.get("dataConsentimentoLgpd").isNull()).isTrue();
    }
}
