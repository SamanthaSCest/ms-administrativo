package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Insumos (inclusive pelo ms-estoque via X-Api-Key) e procedimentos com insumos e custo. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class ProcedimentoInsumoApiTest {

    private static final String API_KEY = "dev-integracao-key";

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
    void estoque_comApiKey_cadastraEAtualizaCustoDoInsumo() throws Exception {
        long id = id(mockMvc.perform(post("/insumos").header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"SERINGA\",\"nome\":\"Seringa 5ml\",\"unidadeMedida\":\"UN\",\"custoUnitario\":0.80}"))
                .andExpect(status().isCreated()).andReturn());

        mockMvc.perform(patch("/insumos/{id}/custo", id).header("X-Api-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"custoUnitario\":0.95}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custoUnitario").value(0.95));

        mockMvc.perform(get("/insumos/{id}", id).header("X-Api-Key", API_KEY)).andExpect(status().isOk());
        // Sem a chave: 401
        mockMvc.perform(get("/insumos/{id}", id)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "recepcao", roles = "FUNCIONARIO")
    void funcionario_consultaInsumos_masNaoAltera() throws Exception {
        mockMvc.perform(get("/insumos")).andExpect(status().isOk());
        mockMvc.perform(post("/insumos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X\",\"nome\":\"X\",\"unidadeMedida\":\"UN\",\"custoUnitario\":1}"))
                .andExpect(status().isForbidden());
    }
}
