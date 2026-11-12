package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.entities.OutboxEvento;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.OutboxRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Eventos de domínio gravados na outbox (RF23, RN19) a cada criação, alteração, inativação, reativação e remoção. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class EventosOutboxApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;
    @Autowired
    private SalaRepository salaRepository;

    @BeforeEach
    @AfterEach
    void limpar() {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadeRepository.deleteAll();
        salaRepository.deleteAll();
        outboxRepository.deleteAll();
    }

    private long criar(String url, String body) throws Exception {
        MvcResult r = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    /** Eventos do agregado, em ordem de gravação. */
    private List<OutboxEvento> eventos(long agregadoId, String prefixo) {
        return outboxRepository.findAll().stream()
                .filter(e -> e.getAgregadoId() == agregadoId && e.getTipo().startsWith(prefixo))
                .sorted(Comparator.comparing(OutboxEvento::getCriadoEm))
                .toList();
    }

    @Test
    void paciente_cicloDeVidaCompleto_comCpfMascaradoNosDados() throws Exception {
        long id = criar("/pacientes", "{\"nome\":\"Ana\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\"}");

        mockMvc.perform(put("/pacientes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\",\"telefoneFixo\":\"3432410000\"}"))
                .andExpect(status().isOk());
        // PUT sem mudança nenhuma não publica evento
        mockMvc.perform(put("/pacientes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\",\"telefoneFixo\":\"3432410000\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/pacientes/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isOk());
        // Inativar de novo não muda nada: sem evento
        mockMvc.perform(patch("/pacientes/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/pacientes/{id}/status", id).contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/pacientes/{id}", id)).andExpect(status().isNoContent());

        List<OutboxEvento> eventos = eventos(id, "paciente.");
        assertThat(eventos).extracting(OutboxEvento::getTipo).containsExactly(
                "paciente.criado", "paciente.atualizado", "paciente.inativado", "paciente.reativado", "paciente.removido");

        JsonNode criado = eventos.get(0).getPayload();
        assertThat(criado.path("origem").asText()).isEqualTo("ms-administrativo");
        assertThat(criado.path("dados").path("cpfMascarado").asText()).isEqualTo("***.456.789-**");
        // LGPD: o CPF completo nunca vai no evento
        assertThat(criado.toString()).doesNotContain("12345678909");

        JsonNode atualizado = eventos.get(1).getPayload();
        assertThat(atualizado.path("camposAlterados")).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("nome", "telefoneFixo");
        assertThat(atualizado.path("dados").path("nome").asText()).isEqualTo("Ana Souza");

        assertThat(eventos.get(2).getPayload().path("dados").path("ativo").asBoolean()).isFalse();
    }

    @Test
    void medicoCriado_levaEspecialidades_eFemininoParaEspecialidadeESala() throws Exception {
        long esp = criar("/especialidades", "{\"nome\":\"Cardiologia\"}");
        long sala = criar("/salas", "{\"numero\":\"101\",\"tipo\":\"CONSULTORIO\"}");
        long medico = criar("/medicos", """
                {"nome":"Dr. House","cpf":"12345678901","dataNascimento":"1980-01-01","crm":"1001","ufCrm":"MG",
                 "especialidadeIds":[%d],"senha":"senhaForte1"}
                """.formatted(esp));

        assertThat(eventos(esp, "especialidade.")).extracting(OutboxEvento::getTipo).containsExactly("especialidade.criada");
        assertThat(eventos(sala, "sala.")).extracting(OutboxEvento::getTipo).containsExactly("sala.criada");

        JsonNode dados = eventos(medico, "medico.").getFirst().getPayload().path("dados");
        assertThat(dados.path("crm").asText()).isEqualTo("1001");
        assertThat(dados.path("especialidades").get(0).path("nome").asText()).isEqualTo("Cardiologia");
        assertThat(dados.has("cpf")).isFalse();
    }

    @Test
    void erroDeValidacao_naoGravaEvento() throws Exception {
        mockMvc.perform(post("/especialidades").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest());
        criar("/especialidades", "{\"nome\":\"Cardiologia\"}");
        // Duplicada: 409 e a transação é desfeita, sem evento extra
        mockMvc.perform(post("/especialidades").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"cardiologia\"}"))
                .andExpect(status().isConflict());

        assertThat(outboxRepository.findAll()).extracting(OutboxEvento::getTipo).containsExactly("especialidade.criada");
    }
}
