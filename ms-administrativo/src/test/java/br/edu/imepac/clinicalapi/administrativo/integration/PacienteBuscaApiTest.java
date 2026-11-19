package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import br.edu.imepac.clinicalapi.administrativo.support.Acessos;
import br.edu.imepac.clinicalapi.administrativo.support.ComGrupo;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Listagem e busca de pacientes (RF02, RF03, RNF08, RN20). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ComGrupo(username = "recepcao", value = "RECEPCIONISTA")
class PacienteBuscaApiTest {

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

    private long maria;
    private long mariana;
    private long joao;

    @BeforeEach
    void preparar() throws Exception {
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        responsavelRepository.deleteAll();
        maria = criar("Maria Silva", "12345678909");
        mariana = criar("Mariana Costa", "11145678920");
        joao = criar("João Pereira", "22245678982");
        mockMvc.perform(patch("/pacientes/{id}/status", mariana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    private long criar(String nome, String cpf) throws Exception {
        MvcResult r = mockMvc.perform(post("/pacientes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","cpf":"%s","dataNascimento":"1990-01-01","telefone":"34999990000"}
                                """.formatted(nome, cpf)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions buscar(String filtro) throws Exception {
        return mockMvc.perform(post("/pacientes/busca").contentType(MediaType.APPLICATION_JSON).content(filtro));
    }

    @Test
    void listar_semSituacao_trazSoAtivos_comCpfMascarado() throws Exception {
        mockMvc.perform(get("/pacientes").param("sort", "nome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(joao))
                .andExpect(jsonPath("$.content[1].id").value(maria))
                .andExpect(jsonPath("$.content[1].cpf").value("***.456.789-**"));
    }

    @Test
    void listar_inativos() throws Exception {
        mockMvc.perform(get("/pacientes").param("ativo", "false"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(mariana));
    }

    @Test
    void getPorId_continuaComCpfCompleto() throws Exception {
        mockMvc.perform(get("/pacientes/{id}", maria)).andExpect(jsonPath("$.cpf").value("12345678909"));
    }

    @Test
    void busca_porParteDoNome_semDiferenciarMaiusculas() throws Exception {
        buscar("{\"nome\":\"MARI\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(maria));

        buscar("{\"nome\":\"mari\",\"ativo\":false}")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(mariana));
    }

    @Test
    void busca_porCpfCompleto_trazNoMaximoUm_comCpfMascarado() throws Exception {
        buscar("{\"cpf\":\"22245678982\"}")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(joao))
                .andExpect(jsonPath("$.content[0].cpf").value("***.456.789-**"));
    }

    @Test
    void busca_semFiltros_trazSoAtivos() throws Exception {
        buscar("{}").andExpect(jsonPath("$.page.totalElements").value(2));
    }

    @Test
    void busca_cpfComFormatoInvalido_retorna400() throws Exception {
        buscar("{\"cpf\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cpf"));
    }

    @Test
    void busca_medicoPodeConsultar() throws Exception {
        // A busca é leitura: médico consulta, mesmo sendo POST
        mockMvc.perform(post("/pacientes/busca").with(Acessos.grupo("dr", "MEDICO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"joão\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}
