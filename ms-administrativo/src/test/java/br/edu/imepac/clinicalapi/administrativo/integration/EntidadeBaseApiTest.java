package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
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
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** EntidadeBase: uuid, ativo (inativar/reativar) e auditoria (criadoPor/atualizadoPor). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class EntidadeBaseApiTest {

    private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private ProcedimentoRepository procedimentoRepository;
    @Autowired
    private InsumoRepository insumoRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;

    private long cardio;

    @BeforeEach
    void preparar() throws Exception {
        limpar();
        cardio = id(criar("/especialidades", "{\"nome\":\"Cardiologia\"}"));
    }

    @AfterEach
    void limpar() {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        funcionarioRepository.deleteAll();
        procedimentoRepository.deleteAll();
        insumoRepository.deleteAll();
        especialidadeRepository.deleteAll();
    }

    private long id(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private MvcResult criar(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
    }

    private String medicoJson(String cpf, String crm, long especialidadeId) {
        return """
                {"nome":"Dr. %s","cpf":"%s","dataNascimento":"1980-01-01","crm":"%s","ufCrm":"MG",
                 "especialidadeIds":[%d],"senha":"medicoSenha1"}
                """.formatted(crm, cpf, crm, especialidadeId);
    }

    private org.springframework.test.web.servlet.ResultActions alterarStatus(String recurso, long id, boolean ativo)
            throws Exception {
        return mockMvc.perform(patch("/{recurso}/{id}/status", recurso, id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":" + ativo + "}"));
    }

    @Test
    void criacao_preencheUuidAtivoEAuditoria() throws Exception {
        mockMvc.perform(get("/especialidades/{id}", cardio))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(matchesPattern(UUID_REGEX)))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.criadoPor").value("admin"))
                .andExpect(jsonPath("$.atualizadoPor").value("admin"));
    }

    @Test
    void auditoria_chamadaDeServico_registraServico() throws Exception {
        mockMvc.perform(post("/insumos").header("X-Api-Key", "dev-integracao-key").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"LUVA\",\"nome\":\"Luva\",\"unidadeMedida\":\"UN\",\"custoUnitario\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.criadoPor").value("servico"));
    }

    @Test
    void inativarEReativar_especialidade_bloqueiaNovosVinculos() throws Exception {
        alterarStatus("especialidades", cardio, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false))
                .andExpect(jsonPath("$.atualizadoPor").value("admin"));
        mockMvc.perform(get("/especialidades/{id}", cardio)).andExpect(jsonPath("$.ativo").value(false));

        // Especialidade inativa não entra em vínculo novo
        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson("11145678920", "1001", cardio)))
                .andExpect(status().isUnprocessableEntity());

        alterarStatus("especialidades", cardio, true).andExpect(jsonPath("$.ativo").value(true));
        mockMvc.perform(post("/medicos").contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson("11145678920", "1001", cardio)))
                .andExpect(status().isCreated());
    }

    @Test
    void medicoInativo_naoFazLogin_naoRecebeCusto_eAgendaFicaVazia() throws Exception {
        long medico = id(criar("/medicos", medicoJson("11145678920", "1001", cardio)));
        criar("/disponibilidades", """
                {"tipo":"DIARIA","medicoId":%d,"diaSemana":"SEGUNDA","horaInicial":"08:00","horaFinal":"12:00"}
                """.formatted(medico));
        String segunda = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString();
        mockMvc.perform(get("/disponibilidades/agenda").param("medicoId", String.valueOf(medico)).param("data", segunda))
                .andExpect(jsonPath("$", hasSize(1)));

        alterarStatus("medicos", medico, false).andExpect(status().isOk());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"11145678920\",\"senha\":\"medicoSenha1\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/custos-medicos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"CONSULTA","medicoId":%d,"especialidadeId":%d,"valor":100,"vigenciaInicio":"2025-01-01"}
                                """.formatted(medico, cardio)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/disponibilidades/agenda").param("medicoId", String.valueOf(medico)).param("data", segunda))
                .andExpect(jsonPath("$", hasSize(0)));
        // Continua consultável (inativar não apaga)
        mockMvc.perform(get("/medicos/{id}", medico)).andExpect(jsonPath("$.ativo").value(false));

        // Reativar devolve o login
        alterarStatus("medicos", medico, true).andExpect(status().isOk());
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"11145678920\",\"senha\":\"medicoSenha1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void pacienteInativo_apareceNaSituacaoDaIntegracao() throws Exception {
        long paciente = id(criar("/pacientes", "{\"nome\":\"Ana\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\"}"));

        mockMvc.perform(get("/integracao/pacientes/{id}/situacao", paciente))
                .andExpect(jsonPath("$.ativo").value(true));
        alterarStatus("pacientes", paciente, false).andExpect(status().isOk());
        mockMvc.perform(get("/integracao/pacientes/{id}/situacao", paciente))
                .andExpect(jsonPath("$.existe").value(true))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void permissoes_seguemAsRegrasDeEscritaDeCadaRecurso() throws Exception {
        long paciente = id(criar("/pacientes", "{\"nome\":\"Ana\",\"cpf\":\"12345678909\",\"dataNascimento\":\"1990-01-01\"}"));
        long medico = id(criar("/medicos", medicoJson("11145678920", "1001", cardio)));
        var recepcao = user("recepcao").roles("FUNCIONARIO");

        // Funcionário gerencia pacientes, mas não médicos
        mockMvc.perform(patch("/pacientes/{id}/status", paciente).with(recepcao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atualizadoPor").value("recepcao"));
        mockMvc.perform(patch("/medicos/{id}/status", medico).with(recepcao)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminNaoInativaOProprioCadastro_eRecursoDesconhecidoRetorna404() throws Exception {
        long funcionario = id(criar("/funcionarios", """
                {"nome":"Chefe","cpf":"11122233396","dataNascimento":"1980-01-01","matricula":"F-1",
                 "cargo":"ADMINISTRADOR","dataAdmissao":"2024-01-01","senha":"senhaForte1"}
                """));

        mockMvc.perform(patch("/funcionarios/{id}/status", funcionario).with(user("11122233396").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}"))
                .andExpect(status().isUnprocessableEntity());

        alterarStatus("xpto", 1, false).andExpect(status().isNotFound());
        alterarStatus("especialidades", 999999, false).andExpect(status().isNotFound());
    }
}
