package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Custos (consulta/procedimento, vigência) e disponibilidade (semanal, exceções, agenda do dia). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class CustoDisponibilidadeApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private ProcedimentoRepository procedimentoRepository;
    @Autowired
    private InsumoRepository insumoRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;
    @Autowired
    private SalaRepository salaRepository;

    private long cardio;
    private long dermato;
    private long medico;
    private long sala;

    @BeforeEach
    void preparar() throws Exception {
        limpar();
        cardio = id(criar("/especialidades", "{\"nome\":\"Cardiologia\"}"));
        dermato = id(criar("/especialidades", "{\"nome\":\"Dermatologia\"}"));
        medico = criarMedico("11145678920", "1001");
        sala = id(criar("/salas", "{\"numero\":\"101\",\"tipo\":\"CONSULTORIO\"}"));
    }

    // Custos e disponibilidades somem junto com o médico (ON DELETE CASCADE)
    @AfterEach
    void limpar() {
        medicoRepository.deleteAll();
        procedimentoRepository.deleteAll();
        insumoRepository.deleteAll();
        salaRepository.deleteAll();
        especialidadeRepository.deleteAll();
    }

    private long id(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private MvcResult criar(String url, String body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
    }

    private long criarMedico(String cpf, String crm) throws Exception {
        return id(criar("/medicos", """
                {"nome":"Dr. %s","cpf":"%s","dataNascimento":"1980-01-01","crm":"%s","ufCrm":"MG",
                 "especialidadeIds":[%d],"senha":"senhaForte1"}
                """.formatted(crm, cpf, crm, cardio)));
    }

    private String custoConsulta(long especialidadeId, String inicio, String fim, String valor) {
        return """
                {"tipo":"CONSULTA","medicoId":%d,"especialidadeId":%d,"valor":%s,
                 "vigenciaInicio":"%s","vigenciaFim":%s}
                """.formatted(medico, especialidadeId, valor, inicio, fim == null ? "null" : "\"" + fim + "\"");
    }

    private String diaria(long medicoId, String dia, String ini, String fim, Long salaId) {
        return """
                {"tipo":"DIARIA","medicoId":%d,"salaId":%s,"diaSemana":"%s","horaInicial":"%s","horaFinal":"%s"}
                """.formatted(medicoId, salaId == null ? "null" : salaId.toString(), dia, ini, fim);
    }

    // ---------- Custos ----------

    @Test
    void custoConsulta_vigenciasEValidacoes() throws Exception {
        mockMvc.perform(post("/custos-medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(custoConsulta(cardio, "2024-01-01", "2024-12-31", "200.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("CONSULTA"))
                .andExpect(jsonPath("$.especialidade.descricao").value("Cardiologia"))
                .andExpect(jsonPath("$.vigente").value(false));

        // Vigência sobreposta para o mesmo item -> 409
        mockMvc.perform(post("/custos-medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(custoConsulta(cardio, "2024-06-01", null, "250.00")))
                .andExpect(status().isConflict());
        // Período seguinte, em aberto -> ok
        mockMvc.perform(post("/custos-medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(custoConsulta(cardio, "2025-01-01", null, "250.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vigente").value(true));

        // Especialidade que o médico não atende -> 422
        mockMvc.perform(post("/custos-medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(custoConsulta(dermato, "2025-01-01", null, "100.00")))
                .andExpect(status().isUnprocessableEntity());
        // Fim antes do início -> 422
        mockMvc.perform(post("/custos-medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(custoConsulta(cardio, "2030-02-01", "2030-01-01", "100.00")))
                .andExpect(status().isUnprocessableEntity());

        // Vigente em cada data
        mockMvc.perform(get("/custos-medicos/vigente").param("medicoId", String.valueOf(medico))
                        .param("especialidadeId", String.valueOf(cardio)).param("data", "2024-03-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(200.00));
        mockMvc.perform(get("/custos-medicos/vigente").param("medicoId", String.valueOf(medico))
                        .param("especialidadeId", String.valueOf(cardio)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(250.00));
        mockMvc.perform(get("/custos-medicos/vigente").param("medicoId", String.valueOf(medico))
                        .param("especialidadeId", String.valueOf(cardio)).param("data", "2023-01-01"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/custos-medicos").param("medicoId", String.valueOf(medico)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void custoProcedimento_eRemocaoDoMedicoApagaOsCustos() throws Exception {
        long procedimento = id(criar("/procedimentos", """
                {"especialidadeId":%d,"codigoTuss":"20101012","nome":"Eletrocardiograma","duracaoMinutos":20,
                 "valorBase":80.00}
                """.formatted(cardio)));
        long custo = id(criar("/custos-medicos", """
                {"tipo":"PROCEDIMENTO","medicoId":%d,"procedimentoId":%d,"valor":60.00,"vigenciaInicio":"2024-01-01"}
                """.formatted(medico, procedimento)));

        mockMvc.perform(get("/custos-medicos/{id}", custo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procedimento.descricao").value("20101012 - Eletrocardiograma"));

        // Procedimento com custo não pode ser removido
        mockMvc.perform(delete("/procedimentos/{id}", procedimento)).andExpect(status().isConflict());

        mockMvc.perform(delete("/medicos/{id}", medico)).andExpect(status().isNoContent());
        mockMvc.perform(get("/custos-medicos/{id}", custo)).andExpect(status().isNotFound());
    }

    // ---------- Disponibilidade ----------

    @Test
    void disponibilidade_conflitosEValidacoes() throws Exception {
        mockMvc.perform(post("/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                        .content(diaria(medico, "SEGUNDA", "08:00", "12:00", sala)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.horaInicial").value("08:00"))
                .andExpect(jsonPath("$.sala.descricao").value("101"));

        // Mesmo médico, horário sobreposto -> 409
        mockMvc.perform(post("/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                        .content(diaria(medico, "SEGUNDA", "11:00", "13:00", null)))
                .andExpect(status().isConflict());
        // Encostar no limite (12:00) não é sobreposição
        mockMvc.perform(post("/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                        .content(diaria(medico, "SEGUNDA", "12:00", "13:00", null)))
                .andExpect(status().isCreated());
        // Outro médico na mesma sala e horário -> 409
        long outro = criarMedico("22245678982", "1002");
        mockMvc.perform(post("/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                        .content(diaria(outro, "SEGUNDA", "10:00", "11:00", sala)))
                .andExpect(status().isConflict());

        // Hora final antes da inicial -> 422 ; DIARIA sem dia -> 422
        mockMvc.perform(post("/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                        .content(diaria(medico, "TERCA", "12:00", "08:00", null)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(post("/disponibilidades").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"DIARIA\",\"medicoId\":" + medico + ",\"horaInicial\":\"08:00\",\"horaFinal\":\"09:00\"}"))
                .andExpect(status().isUnprocessableEntity());

        // Sala com disponibilidade não pode ser removida
        mockMvc.perform(delete("/salas/{id}", sala)).andExpect(status().isConflict());
    }

    @Test
    void agendaDoDia_semanaisMenosBloqueiosMaisExtras() throws Exception {
        LocalDate segunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        criar("/disponibilidades", diaria(medico, "SEGUNDA", "08:00", "12:00", null));
        criar("/disponibilidades", diaria(medico, "SEGUNDA", "14:00", "18:00", null));
        criar("/disponibilidades", diaria(medico, "TERCA", "08:00", "12:00", null));
        // Bloqueio de manhã e horário extra à noite, só nesta segunda
        criar("/disponibilidades", """
                {"tipo":"EXCECAO","medicoId":%d,"data":"%s","disponivel":false,"motivo":"Congresso",
                 "horaInicial":"08:00","horaFinal":"12:00"}
                """.formatted(medico, segunda));
        criar("/disponibilidades", """
                {"tipo":"EXCECAO","medicoId":%d,"data":"%s","disponivel":true,"horaInicial":"19:00","horaFinal":"20:00"}
                """.formatted(medico, segunda));

        mockMvc.perform(get("/disponibilidades/agenda").param("medicoId", String.valueOf(medico))
                        .param("data", segunda.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].horaInicial").value("14:00"))
                .andExpect(jsonPath("$[1].horaInicial").value("19:00"))
                .andExpect(jsonPath("$[1].tipo").value("EXCECAO"));

        // Na segunda seguinte, sem exceções: os dois horários semanais
        mockMvc.perform(get("/disponibilidades/agenda").param("medicoId", String.valueOf(medico))
                        .param("data", segunda.plusWeeks(1).toString()))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].horaInicial").value("08:00"));
    }

    @Test
    void funcionario_consultaDisponibilidade_masNaoCadastra() throws Exception {
        // Usuário só nestas requisições (o @BeforeEach precisa do ADMIN para preparar os dados)
        var recepcao = user("recepcao").roles("FUNCIONARIO");
        mockMvc.perform(get("/disponibilidades").with(recepcao)).andExpect(status().isOk());
        mockMvc.perform(post("/disponibilidades").with(recepcao).contentType(MediaType.APPLICATION_JSON)
                        .content(diaria(medico, "SEGUNDA", "08:00", "12:00", null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/custos-medicos").with(recepcao)).andExpect(status().isOk());
        mockMvc.perform(get("/custos-medicos").with(user("dr").roles("MEDICO"))).andExpect(status().isForbidden());
    }
}
