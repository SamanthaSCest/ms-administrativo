package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.support.Acessos;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import br.edu.imepac.clinicalapi.administrativo.support.ComGrupo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Insumos (inclusive por outro microserviço, com token client credentials) e procedimentos com insumos e custo. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ComGrupo(username = "admin", value = "ADMINISTRADOR")
class ProcedimentoInsumoApiTest {

    private static final RequestPostProcessor SERVICO =
            Acessos.grupo("service-account-ms-agendamento", Acessos.SERVICO);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProcedimentoRepository procedimentoRepository;
    @Autowired
    private InsumoRepository insumoRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;

    @BeforeEach
    @AfterEach  // não deixa procedimentos que impeçam outras classes de apagar especialidades
    void limpar() {
        procedimentoRepository.deleteAll();
        insumoRepository.deleteAll();
        medicoRepository.deleteAll();
        especialidadeRepository.deleteAll();
    }

    private long id(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private long insumo(String codigo, String custo) throws Exception {
        return id(mockMvc.perform(post("/insumos").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","nome":"Insumo %s","unidadeMedida":"UN","custoUnitario":%s}
                                """.formatted(codigo, codigo, custo)))
                .andExpect(status().isCreated()).andReturn());
    }

    private long especialidade() throws Exception {
        return id(mockMvc.perform(post("/especialidades").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Dermatologia\"}")).andExpect(status().isCreated()).andReturn());
    }

    private String procedimentoJson(long especialidadeId, String insumos) {
        return """
                {"especialidadeId":%d,"codigoTuss":"30101012","nome":"Biópsia de pele","duracaoMinutos":30,
                 "valorBase":250.00,"insumos":%s}
                """.formatted(especialidadeId, insumos);
    }

    @Test
    void procedimento_calculaCustoDosInsumos_eAtualizaLista() throws Exception {
        long luva = insumo("LUVA", "2.50");
        long gaze = insumo("GAZE", "0.3333");
        long agulha = insumo("AGULHA", "1.00");
        long esp = especialidade();

        long id = id(mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(procedimentoJson(esp, """
                                [{"insumoId":%d,"quantidade":4},{"insumoId":%d,"quantidade":3}]
                                """.formatted(luva, gaze))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.especialidade.nome").value("Dermatologia"))
                .andExpect(jsonPath("$.insumos", hasSize(2)))
                // 2.50 x 4 = 10.00 ; 0.3333 x 3 = 1.00 (arredondado) -> 11.00
                .andExpect(jsonPath("$.custoInsumos").value(11.00))
                .andReturn());

        // Muda a quantidade da luva, tira a gaze e inclui a agulha
        mockMvc.perform(put("/procedimentos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(procedimentoJson(esp, """
                                [{"insumoId":%d,"quantidade":2},{"insumoId":%d,"quantidade":1}]
                                """.formatted(luva, agulha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insumos", hasSize(2)))
                .andExpect(jsonPath("$.custoInsumos").value(6.00));

        // Insumo em uso não pode ser removido
        mockMvc.perform(delete("/insumos/{id}", luva)).andExpect(status().isConflict());
        // Gaze saiu do procedimento: pode ser removida
        mockMvc.perform(delete("/insumos/{id}", gaze)).andExpect(status().isNoContent());
    }

    /** Exame: mesmo cadastro, com tipo EXAME e código TUSS próprio. */
    private String exameJson(long especialidadeId, String tipo) {
        return """
                {"especialidadeId":%d,%s"codigoTuss":"40304361","nome":"Hemograma completo","duracaoMinutos":15,
                 "valorBase":35.00,"insumos":[]}
                """.formatted(especialidadeId, tipo == null ? "" : "\"tipo\":\"%s\",".formatted(tipo));
    }

    @Test
    void exame_cadastradoJuntoAosProcedimentos_eFiltradoPorTipo() throws Exception {
        long esp = especialidade();
        // Sem tipo: continua sendo procedimento (quem já usa a API não precisa mudar nada)
        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(procedimentoJson(esp, "[]")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PROCEDIMENTO"));
        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(exameJson(esp, "EXAME")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("EXAME"))
                .andExpect(jsonPath("$.codigoTuss").value("40304361"));

        mockMvc.perform(get("/procedimentos").param("tipo", "EXAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Hemograma completo"));
        mockMvc.perform(get("/procedimentos").param("tipo", "PROCEDIMENTO"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Biópsia de pele"));
        // Sem filtro: os dois
        mockMvc.perform(get("/procedimentos")).andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void exame_atualizarSemTipo_mantemExame_eComTipo_troca() throws Exception {
        long esp = especialidade();
        long id = id(mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(exameJson(esp, "EXAME")))
                .andExpect(status().isCreated()).andReturn());

        mockMvc.perform(put("/procedimentos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(exameJson(esp, null).replace("35.00", "40.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("EXAME"))
                .andExpect(jsonPath("$.valorBase").value(40.00));
        mockMvc.perform(put("/procedimentos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(exameJson(esp, "PROCEDIMENTO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PROCEDIMENTO"));
    }

    @Test
    void exame_tussUnicoEntreProcedimentosEExames_eTipoInvalido() throws Exception {
        long esp = especialidade();
        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                .content(procedimentoJson(esp, "[]"))).andExpect(status().isCreated());
        // Mesmo código TUSS do procedimento já cadastrado, agora como exame: 409
        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(procedimentoJson(esp, "[]").replace("\"codigoTuss\"", "\"tipo\":\"EXAME\",\"codigoTuss\"")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(exameJson(esp, "LABORATORIO")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/procedimentos").param("tipo", "LABORATORIO")).andExpect(status().isBadRequest());
    }

    @Test
    void procedimento_insumoRepetido_retorna422() throws Exception {
        long luva = insumo("LUVA", "2.50");
        long esp = especialidade();

        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(procedimentoJson(esp, """
                                [{"insumoId":%d,"quantidade":1},{"insumoId":%d,"quantidade":2}]
                                """.formatted(luva, luva))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void procedimento_tussDuplicado_retorna409_eInsumoInexistente_retorna422() throws Exception {
        long esp = especialidade();
        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                .content(procedimentoJson(esp, "[]"))).andExpect(status().isCreated());

        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                .content(procedimentoJson(esp, "[]"))).andExpect(status().isConflict());

        mockMvc.perform(post("/procedimentos").contentType(MediaType.APPLICATION_JSON)
                        .content(procedimentoJson(esp, "[{\"insumoId\":999999,\"quantidade\":1}]")
                                .replace("30101012", "40101012")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithAnonymousUser
    void servico_comToken_cadastraEAtualizaCustoDoInsumo() throws Exception {
        long id = id(mockMvc.perform(post("/insumos").with(SERVICO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"SERINGA\",\"nome\":\"Seringa 5ml\",\"unidadeMedida\":\"UN\",\"custoUnitario\":0.80}"))
                .andExpect(status().isCreated()).andReturn());

        mockMvc.perform(patch("/insumos/{id}/custo", id).with(SERVICO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"custoUnitario\":0.95}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custoUnitario").value(0.95));

        mockMvc.perform(get("/insumos/{id}", id).with(SERVICO)).andExpect(status().isOk());
        // Sem token: 401
        mockMvc.perform(get("/insumos/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    @ComGrupo(username = "dr", value = "MEDICO")
    void medico_consultaInsumos_masNaoAltera() throws Exception {
        mockMvc.perform(get("/insumos")).andExpect(status().isOk());
        mockMvc.perform(post("/insumos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X\",\"nome\":\"X\",\"unidadeMedida\":\"UN\",\"custoUnitario\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @ComGrupo(username = "estoque", value = "ESTOQUISTA")
    void estoquista_cadastraInsumo() throws Exception {
        mockMvc.perform(post("/insumos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"EST-1\",\"nome\":\"Luva\",\"unidadeMedida\":\"UN\",\"custoUnitario\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    @ComGrupo(username = "recepcao", value = "RECEPCIONISTA")
    void recepcionista_naoConsultaInsumos() throws Exception {
        mockMvc.perform(get("/insumos")).andExpect(status().isForbidden());
    }
}
