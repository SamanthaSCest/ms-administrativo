package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Listagem e busca de funcionários por nome, CPF e situação (RNF08, RN20). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class FuncionarioBuscaApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FuncionarioRepository funcionarioRepository;

    private long ana;
    private long anabela;
    private long bruno;

    @BeforeEach
    void preparar() throws Exception {
        funcionarioRepository.deleteAll();
        ana = criar("Ana Lima", "12345678909", "F-101");
        anabela = criar("Anabela Souza", "11145678920", "F-102");
        bruno = criar("Bruno Alves", "22245678982", "F-103");
        mockMvc.perform(patch("/funcionarios/{id}/status", anabela).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ativo\":false}")).andExpect(status().isOk());
    }

    private long criar(String nome, String cpf, String matricula) throws Exception {
        MvcResult r = mockMvc.perform(post("/funcionarios").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","cpf":"%s","dataNascimento":"1992-03-10","matricula":"%s",
                                 "cargo":"RECEPCIONISTA","dataAdmissao":"2024-02-01","senha":"funcSenha123"}
                                """.formatted(nome, cpf, matricula)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions buscar(String filtro) throws Exception {
        return mockMvc.perform(post("/funcionarios/busca").contentType(MediaType.APPLICATION_JSON).content(filtro));
    }

    @Test
    void listar_semSituacao_trazSoAtivos_comCpfMascarado() throws Exception {
        mockMvc.perform(get("/funcionarios").param("sort", "nome"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(ana))
                .andExpect(jsonPath("$.content[0].cpf").value("***.456.789-**"))
                .andExpect(jsonPath("$.content[1].id").value(bruno));
    }

    @Test
    void listar_inativos() throws Exception {
        mockMvc.perform(get("/funcionarios").param("ativo", "false"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(anabela));
    }

    @Test
    void getPorId_continuaComCpfCompleto() throws Exception {
        mockMvc.perform(get("/funcionarios/{id}", ana)).andExpect(jsonPath("$.cpf").value("12345678909"));
    }

    @Test
    void busca_porParteDoNome_semDiferenciarMaiusculas() throws Exception {
        buscar("{\"nome\":\"ANA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(ana));

        buscar("{\"nome\":\"ana\",\"ativo\":false}")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(anabela));
    }

    @Test
    void busca_porCpfCompleto_comCpfMascarado() throws Exception {
        buscar("{\"cpf\":\"22245678982\"}")
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(bruno))
                .andExpect(jsonPath("$.content[0].cpf").value("***.456.789-**"));
    }

    @Test
    void busca_semResultado_devolvePaginaVazia() throws Exception {
        buscar("{\"nome\":\"inexistente\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void busca_cpfComFormatoInvalido_retorna400() throws Exception {
        buscar("{\"cpf\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("cpf"));
    }

    @Test
    void busca_somenteAdmin() throws Exception {
        mockMvc.perform(post("/funcionarios/busca").with(user("recepcao").roles("FUNCIONARIO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
