package br.edu.imepac.clinicalapi.agendamento.integration;

import br.edu.imepac.clinicalapi.agendamento.client.AdministrativoClient;
import br.edu.imepac.clinicalapi.agendamento.client.dto.MedicoResumo;
import br.edu.imepac.clinicalapi.agendamento.client.dto.PacienteResumo;
import br.edu.imepac.clinicalapi.agendamento.repositories.ConsultaRepository;
import br.edu.imepac.clinicalapi.common.exception.ExternalServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contexto completo (JPA + Flyway em H2); o ms-administrativo é simulado via mock do client. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgendamentoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ConsultaRepository repository;

    @MockBean
    private AdministrativoClient administrativoClient;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        when(administrativoClient.buscarMedico(anyLong()))
                .thenAnswer(inv -> Optional.of(new MedicoResumo(inv.getArgument(0), "Maria", "CRM-1")));
        when(administrativoClient.buscarPaciente(anyLong()))
                .thenAnswer(inv -> Optional.of(new PacienteResumo(inv.getArgument(0), "João")));
    }

    private String json(long medicoId, long pacienteId, LocalDateTime data) {
        return """
                {"medicoId":%d,"pacienteId":%d,"dataAgendamento":"%s","sintomas":"febre"}
                """.formatted(medicoId, pacienteId, data);
    }

    private long agendar(long medicoId, LocalDateTime data) throws Exception {
        MvcResult result = mockMvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON)
                        .content(json(medicoId, 2, data)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void fluxoCompleto_agendarRemarcarRealizar() throws Exception {
        LocalDateTime data = LocalDateTime.now().plusDays(5).withNano(0);
        long id = agendar(1, data);

        mockMvc.perform(get("/consultas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sintomas").value("febre"))
                .andExpect(jsonPath("$.medicoId").value(1));

        mockMvc.perform(put("/consultas/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataAgendamento\":\"" + data.plusDays(1) + "\",\"observacoes\":\"retorno\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REMARCADO"));

        mockMvc.perform(patch("/consultas/{id}/realizar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADO"));

        mockMvc.perform(patch("/consultas/{id}/cancelar", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void listar_comFiltros() throws Exception {
        LocalDateTime data = LocalDateTime.now().plusDays(5).withNano(0);
        agendar(1, data);
        long outro = agendar(3, data);
        mockMvc.perform(patch("/consultas/{id}/cancelar", outro)).andExpect(status().isOk());

        mockMvc.perform(get("/consultas")).andExpect(jsonPath("$.content", hasSize(2)));
        mockMvc.perform(get("/consultas").param("medicoId", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mockMvc.perform(get("/consultas").param("status", "CANCELADO"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].medicoId").value(3));
        mockMvc.perform(get("/consultas").param("status", "INVALIDO")).andExpect(status().isBadRequest());
    }

    @Test
    void agendar_mesmoHorarioMesmoMedico_retorna409_masLiberaSeCancelada() throws Exception {
        LocalDateTime data = LocalDateTime.now().plusDays(5).withNano(0);
        long id = agendar(1, data);

        mockMvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON).content(json(1, 4, data)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/consultas/{id}/cancelar", id)).andExpect(status().isOk());
        agendar(1, data);
    }

    @Test
    void agendar_dataNoPassado_retorna400() throws Exception {
        mockMvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON)
                        .content(json(1, 2, LocalDateTime.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("dataAgendamento"));
    }

    @Test
    void agendar_medicoInexistente_retorna422() throws Exception {
        when(administrativoClient.buscarMedico(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON)
                        .content(json(99, 2, LocalDateTime.now().plusDays(2))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Médico não encontrado: 99"));
    }

    @Test
    void agendar_administrativoForaDoAr_retorna503() throws Exception {
        when(administrativoClient.buscarMedico(anyLong()))
                .thenThrow(new ExternalServiceException("ms-administrativo indisponível", new RuntimeException()));

        mockMvc.perform(post("/consultas").contentType(MediaType.APPLICATION_JSON)
                        .content(json(1, 2, LocalDateTime.now().plusDays(2))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void buscar_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/consultas/12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/consultas/12345"));
    }
}
