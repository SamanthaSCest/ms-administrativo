package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.GrupoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC com login de verdade: grupos padrão da migration V14, gestão de grupos, permissões avulsas por usuário
 * (valem no próximo login) e troca do grupo do cargo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RbacApiTest {

    private static final String CPF_RECEPCAO = "66645678908";
    private static final String SENHA = "funcSenha123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private GrupoRepository grupoRepository;

    private String admin;

    @BeforeEach
    void preparar() throws Exception {
        limpar();
        admin = token("admin", "admin12345");
    }

    // Os usuários criados têm o CPF como login e os grupos extras têm nome único: limpar evita conflito
    @AfterEach
    void limpar() {
        funcionarioRepository.deleteAll();
        grupoRepository.findAll().stream().filter(g -> !g.isPadrao()).forEach(grupoRepository::delete);
    }

    private String token(String login, String senha) throws Exception {
        MvcResult r = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)))
                .andExpect(status().isOk()).andReturn();
        return json(r).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private JsonNode json(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    private String funcionarioJson(String cargo) {
        return """
                {"nome":"Ana Lima","cpf":"%s","dataNascimento":"1992-03-10","matricula":"F-900",
                 "cargo":"%s","dataAdmissao":"2024-02-01","senha":"%s"}
                """.formatted(CPF_RECEPCAO, cargo, SENHA);
    }

    /** Cria a recepcionista e devolve o JSON do funcionário (com usuario.id). */
    private JsonNode criarRecepcionista() throws Exception {
        return json(mockMvc.perform(post("/funcionarios").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(funcionarioJson("RECEPCIONISTA")))
                .andExpect(status().isCreated()).andReturn());
    }

    private long grupoId(String nome) {
        return grupoRepository.findByNome(nome).orElseThrow().getId();
    }

    private long criarGrupo(String nome, String permissoes) throws Exception {
        return json(mockMvc.perform(post("/grupos").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"permissoes\":[%s]}".formatted(nome, permissoes)))
                .andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    @Test
    void permissoes_listaTodasComDescricao() throws Exception {
        mockMvc.perform(get("/permissoes").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(Permissao.values().length)))
                .andExpect(jsonPath("$[0].codigo").value("USUARIO_GERENCIAR"))
                .andExpect(jsonPath("$[0].descricao").isNotEmpty());
    }

    @Test
    void gruposPadrao_daMigration() throws Exception {
        mockMvc.perform(get("/grupos/{id}", grupoId("RECEPCIONISTA")).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.padrao").value(true))
                .andExpect(jsonPath("$.permissoes", hasItem("PACIENTE_GERENCIAR")))
                .andExpect(jsonPath("$.permissoes", not(hasItem("FUNCIONARIO_CONSULTAR"))));
        mockMvc.perform(get("/grupos/{id}", grupoId("ADMINISTRADOR")).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(jsonPath("$.permissoes", hasSize(Permissao.values().length)));
    }

    @Test
    void grupo_criarAlterarERemover() throws Exception {
        long id = criarGrupo("AGENDAMENTO", "\"PACIENTE_CONSULTAR\"");

        // Nome único
        mockMvc.perform(post("/grupos").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"AGENDAMENTO\",\"permissoes\":[]}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"AGENDAMENTO\",\"permissoes\":[\"PACIENTE_CONSULTAR\",\"DISPONIBILIDADE_CONSULTAR\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.padrao").value(false))
                .andExpect(jsonPath("$.permissoes", containsInAnyOrder("PACIENTE_CONSULTAR", "DISPONIBILIDADE_CONSULTAR")));

        mockMvc.perform(delete("/grupos/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void grupo_permissaoInexistente_retorna400() throws Exception {
        mockMvc.perform(post("/grupos").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\",\"permissoes\":[\"VOAR\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grupoPadrao_naoPodeSerRemovidoNemRenomeado_eAdministradorNaoMuda() throws Exception {
        long recepcao = grupoId("RECEPCIONISTA");
        mockMvc.perform(delete("/grupos/{id}", recepcao).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/grupos/{id}", recepcao).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"RECEPCAO\",\"permissoes\":[]}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/grupos/{id}", grupoId("ADMINISTRADOR")).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"ADMINISTRADOR\",\"permissoes\":[]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void permissaoAvulsa_valeAPartirDoProximoLogin() throws Exception {
        long usuarioId = criarRecepcionista().get("usuario").get("id").asLong();
        String antes = token(CPF_RECEPCAO, SENHA);
        mockMvc.perform(get("/insumos").header(HttpHeaders.AUTHORIZATION, bearer(antes))).andExpect(status().isForbidden());

        mockMvc.perform(put("/usuarios/{id}/acessos", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoIds\":[%d],\"permissoes\":[\"INSUMO_CONSULTAR\"]}".formatted(grupoId("RECEPCIONISTA"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoes", containsInAnyOrder("INSUMO_CONSULTAR")))
                .andExpect(jsonPath("$.permissoesEfetivas", hasItem("INSUMO_CONSULTAR")))
                .andExpect(jsonPath("$.permissoesEfetivas", hasItem("PACIENTE_GERENCIAR")));

        // O token antigo carrega as permissões de antes
        mockMvc.perform(get("/insumos").header(HttpHeaders.AUTHORIZATION, bearer(antes))).andExpect(status().isForbidden());
        String depois = token(CPF_RECEPCAO, SENHA);
        mockMvc.perform(get("/insumos").header(HttpHeaders.AUTHORIZATION, bearer(depois))).andExpect(status().isOk());
    }

    @Test
    void semGrupos_eSemPermissoes_naoAcessaNada() throws Exception {
        long usuarioId = criarRecepcionista().get("usuario").get("id").asLong();
        mockMvc.perform(put("/usuarios/{id}/acessos", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grupoIds\":[],\"permissoes\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissoesEfetivas", hasSize(0)));

        String semAcesso = token(CPF_RECEPCAO, SENHA);
        mockMvc.perform(get("/pacientes").header(HttpHeaders.AUTHORIZATION, bearer(semAcesso)))
                .andExpect(status().isForbidden());
        // Os próprios dados continuam acessíveis
        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(semAcesso))).andExpect(status().isOk());
    }

    @Test
    void acessos_doProprioUsuario_retorna422() throws Exception {
        long meuId = json(mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andReturn()).get("id").asLong();
        mockMvc.perform(put("/usuarios/{id}/acessos", meuId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grupoIds\":[],\"permissoes\":[]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void acessos_grupoInexistente_retorna422() throws Exception {
        long usuarioId = criarRecepcionista().get("usuario").get("id").asLong();
        mockMvc.perform(put("/usuarios/{id}/acessos", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grupoIds\":[999999]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void grupoEmUso_naoPodeSerRemovido() throws Exception {
        long extra = criarGrupo("AGENDAMENTO", "\"DISPONIBILIDADE_CONSULTAR\"");
        long usuarioId = criarRecepcionista().get("usuario").get("id").asLong();
        mockMvc.perform(put("/usuarios/{id}/acessos", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoIds\":[%d,%d]}".formatted(grupoId("RECEPCIONISTA"), extra)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/grupos/{id}", extra).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void trocarCargo_trocaOGrupoDoCargo_eMantemOsDemais() throws Exception {
        long extra = criarGrupo("AGENDAMENTO", "\"DISPONIBILIDADE_CONSULTAR\"");
        JsonNode funcionario = criarRecepcionista();
        long id = funcionario.get("id").asLong();
        long usuarioId = funcionario.get("usuario").get("id").asLong();
        mockMvc.perform(put("/usuarios/{id}/acessos", usuarioId).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grupoIds\":[%d,%d]}".formatted(grupoId("RECEPCIONISTA"), extra)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/funcionarios/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(funcionarioJson("ESTOQUISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.grupos[*].nome", containsInAnyOrder("AGENDAMENTO", "ESTOQUISTA")));
    }

    @Test
    void recepcionista_naoGerenciaAcessos() throws Exception {
        criarRecepcionista();
        String recepcao = token(CPF_RECEPCAO, SENHA);
        mockMvc.perform(get("/grupos").header(HttpHeaders.AUTHORIZATION, bearer(recepcao)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(recepcao)))
                .andExpect(status().isForbidden());
    }
}
