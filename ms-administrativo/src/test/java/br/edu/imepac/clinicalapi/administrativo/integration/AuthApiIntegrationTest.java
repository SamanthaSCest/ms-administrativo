package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.entities.Funcionario;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import br.edu.imepac.clinicalapi.administrativo.services.UsuarioService;
import br.edu.imepac.clinicalapi.administrativo.support.Acessos;
import br.edu.imepac.clinicalapi.administrativo.support.IdentidadeEmMemoria;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação pelo Keycloak: validação do Bearer token, regras por role (grupos do realm) e o usuário do
 * Keycloak acompanhando o cadastro de funcionário/médico (criação, grupos, senha, situação e remoção).
 * O Keycloak é o {@link IdentidadeEmMemoria}; o token vem pronto de {@link Acessos} (login é com o Keycloak).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private EspecialidadeRepository especialidadeRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private IdentidadeEmMemoria keycloak;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private TransactionTemplate transacao;

    @BeforeEach
    void limpar() {
        // Remove pessoas (e seus usuários, em cascata)
        funcionarioRepository.deleteAll();
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadeRepository.deleteAll();
    }

    private static RequestPostProcessor admin() {
        return Acessos.grupo("admin", "ADMINISTRADOR");
    }

    private long criarPaciente(RequestPostProcessor quem, String cpf) throws Exception {
        MvcResult result = mockMvc.perform(post("/pacientes").with(quem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"João Pereira","cpf":"%s","dataNascimento":"1990-01-15"}
                                """.formatted(cpf)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long criarFuncionario(String cpf, String matricula, String cargo) throws Exception {
        MvcResult result = mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson(cpf, matricula, cargo)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long criarEspecialidade() throws Exception {
        MvcResult esp = mockMvc.perform(post("/especialidades").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Cardiologia\"}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(esp.getResponse().getContentAsString()).get("id").asLong();
    }

    private String medicoJson(long especialidadeId, String cpf) {
        return """
                {"nome":"Maria Souza","cpf":"%s","dataNascimento":"1985-04-20",
                 "crm":"CRM-1","ufCrm":"SP","especialidadeIds":[%d],"senha":"medicoSenha1"}
                """.formatted(cpf, especialidadeId);
    }

    /** cargo null = RECEPCIONISTA. */
    private String funcionarioJson(String cpf, String matricula, String cargo) {
        return """
                {"nome":"Ana Lima","cpf":"%s","email":"ana@clinica.com","telefone":"34999990000",
                 "dataNascimento":"1992-03-10","matricula":"%s","cargo":"%s","setor":"Recepção",
                 "dataAdmissao":"2024-02-01","senha":"funcSenha123"}
                """.formatted(cpf, matricula, cargo == null ? "RECEPCIONISTA" : cargo);
    }

    private String keycloakId(String login) {
        return usuarioRepository.findByLogin(login).orElseThrow().getKeycloakId();
    }

    @Test
    void me_devolveLoginGruposEPermissoesDoToken() throws Exception {
        mockMvc.perform(get("/auth/me").with(Acessos.grupo("11122233396", "RECEPCIONISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("11122233396"))
                .andExpect(jsonPath("$.grupos[0]").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.permissoes", hasItem("PACIENTE_GERENCIAR")))
                .andExpect(jsonPath("$.permissoes", not(hasItem("USUARIO_GERENCIAR"))));
    }

    @Test
    void loginESenhaSaoDoKeycloak_rotasAntigasNaoExistemMais() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"senha\":\"admin12345\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/auth/senha").with(admin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void semToken_retorna401NoFormatoPadrao() throws Exception {
        mockMvc.perform(get("/medicos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/medicos"));
    }

    @Test
    void tokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get("/medicos").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiKeyNaoAutenticaMais() throws Exception {
        mockMvc.perform(get("/integracao/especialidades").header("X-Api-Key", "dev-integracao-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotasPublicas_naoExigemToken() throws Exception {
        // (health responde 503 nos testes por não haver RabbitMQ; info basta para provar que é público)
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mockMvc.perform(get("/contract/openapi-administrativo.yaml")).andExpect(status().isOk());
    }

    @Test
    void cadastrarFuncionario_criaUsuarioNoKeycloakNoGrupoDoCargo() throws Exception {
        MvcResult result = mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson("11122233396", "F-001", null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.cargo").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.usuario.login").value("11122233396"))
                .andExpect(jsonPath("$.usuario.keycloakId").isNotEmpty())
                .andExpect(jsonPath("$.usuario.grupos[0]").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        var noKeycloak = keycloak.usuario(keycloakId("11122233396")).orElseThrow();
        assertThat(noKeycloak.login).isEqualTo("11122233396");
        assertThat(noKeycloak.senha).isEqualTo("funcSenha123");
        assertThat(noKeycloak.nome).isEqualTo("Ana Lima");
        assertThat(noKeycloak.email).isEqualTo("ana@clinica.com");
        assertThat(noKeycloak.grupos).containsExactly("RECEPCIONISTA");

        // Recepcionista (roles do grupo no realm) gerencia pacientes, mas não funcionários nem especialidades
        RequestPostProcessor recepcionista = Acessos.grupo("11122233396", "RECEPCIONISTA");
        criarPaciente(recepcionista, "98765432100");
        mockMvc.perform(get("/funcionarios").with(recepcionista))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(post("/especialidades").with(recepcionista)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Cardio\"}"))
                .andExpect(status().isForbidden());

        // Remover o funcionário remove o usuário daqui e do Keycloak
        String keycloakId = keycloakId("11122233396");
        mockMvc.perform(delete("/funcionarios/{id}", id).with(admin()))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.existsByLogin("11122233396")).isFalse();
        assertThat(keycloak.usuario(keycloakId)).isEmpty();
    }

    @Test
    void transacaoDesfeita_removeDoKeycloakOUsuarioRecemCriado() {
        Funcionario pessoa = new Funcionario();
        pessoa.setCpf("52998224725");
        pessoa.setNome("Carlos Dias");

        transacao.executeWithoutResult(status -> {
            usuarioService.novoUsuario(pessoa, "senhaForte123", Set.of("RECEPCIONISTA"));
            assertThat(keycloak.usuarioPorLogin("52998224725")).isPresent();
            status.setRollbackOnly();  // ex.: falha ao gravar no banco depois de criar no Keycloak
        });

        assertThat(keycloak.usuarioPorLogin("52998224725")).isEmpty();
    }

    @Test
    void atualizarFuncionario_trocaCargoESenhaNoKeycloak() throws Exception {
        long id = criarFuncionario("11122233396", "F-001", null);

        mockMvc.perform(put("/funcionarios/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson("11122233396", "F-001", "ESTOQUISTA")
                                .replace("funcSenha123", "outraSenha456")
                                .replace("Ana Lima", "Ana Lima Souza")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.grupos[0]").value("ESTOQUISTA"));

        var noKeycloak = keycloak.usuario(keycloakId("11122233396")).orElseThrow();
        assertThat(noKeycloak.grupos).containsExactly("ESTOQUISTA");
        assertThat(noKeycloak.senha).isEqualTo("outraSenha456");
        assertThat(noKeycloak.nome).isEqualTo("Ana Lima Souza");
    }

    @Test
    void funcionario_semSenha_retorna400() throws Exception {
        String semSenha = funcionarioJson("11122233396", "F-001", null).replace(",\"senha\":\"funcSenha123\"", "");

        mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semSenha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    @Test
    void funcionario_cargoInexistente_retorna400() throws Exception {
        mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson("11122233396", "F-001", "MEDICO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paciente_naoTemUsuario() throws Exception {
        // Mesmo que alguém envie "senha", o campo não existe para paciente e é ignorado
        mockMvc.perform(post("/pacientes").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"João Pereira","cpf":"98765432100","dataNascimento":"1990-01-15","senha":"qualquer123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").doesNotExist());

        assertThat(usuarioRepository.existsByLogin("98765432100")).isFalse();
        assertThat(keycloak.usuarioPorLogin("98765432100")).isEmpty();
    }

    @Test
    void admin_desativaUsuario_eOKeycloakAcompanha() throws Exception {
        criarFuncionario("11122233396", "F-001", null);
        long usuarioId = usuarioRepository.findByLogin("11122233396").orElseThrow().getId();

        mockMvc.perform(patch("/usuarios/{id}/status", usuarioId).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        assertThat(keycloak.usuario(keycloakId("11122233396")).orElseThrow().ativo).isFalse();
    }

    @Test
    void inativarFuncionario_desativaOUsuarioNoKeycloak() throws Exception {
        long id = criarFuncionario("11122233396", "F-001", null);

        mockMvc.perform(patch("/funcionarios/{id}/status", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk());

        assertThat(keycloak.usuario(keycloakId("11122233396")).orElseThrow().ativo).isFalse();
    }

    @Test
    void admin_naoPodeDesativarASiMesmo() throws Exception {
        criarFuncionario("55566677720", "F-002", "ADMINISTRADOR");
        long usuarioId = usuarioRepository.findByLogin("55566677720").orElseThrow().getId();

        mockMvc.perform(patch("/usuarios/{id}/status", usuarioId)
                        .with(Acessos.grupo("55566677720", "ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void detalheDoUsuario_trazGruposDoKeycloak() throws Exception {
        criarFuncionario("55566677720", "F-002", "ADMINISTRADOR");
        long usuarioId = usuarioRepository.findByLogin("55566677720").orElseThrow().getId();

        mockMvc.perform(get("/usuarios/{id}", usuarioId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos[0]").value("ADMINISTRADOR"));
        // Na listagem os grupos não vêm (evita uma consulta ao Keycloak por linha)
        mockMvc.perform(get("/usuarios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].grupos").doesNotExist());
    }

    @Test
    void listarGrupos_vemDoKeycloak() throws Exception {
        mockMvc.perform(get("/grupos").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("MEDICO")))
                .andExpect(jsonPath("$", hasItem("RECEPCIONISTA")));
        mockMvc.perform(get("/grupos").with(Acessos.grupo("rec", "RECEPCIONISTA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void medico_cadastradoEntraNoGrupoMedico() throws Exception {
        long espId = criarEspecialidade();

        mockMvc.perform(post("/medicos").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "11145678920")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.grupos[0]").value("MEDICO"));
        assertThat(keycloak.usuario(keycloakId("11145678920")).orElseThrow().grupos).containsExactly("MEDICO");

        RequestPostProcessor medico = Acessos.grupo("11145678920", "MEDICO");
        // Médico consulta pacientes, mas não cadastra
        mockMvc.perform(get("/pacientes").with(medico)).andExpect(status().isOk());
        mockMvc.perform(post("/pacientes").with(medico).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void funcionarioPodeTerOMesmoCpfDeUmPaciente() throws Exception {
        criarPaciente(admin(), "11122233396");

        // Paciente não ocupa login, então a funcionária que também é paciente pode ser cadastrada
        criarFuncionario("11122233396", "F-001", null);
    }

    @Test
    void cpfJaUsadoComoLogin_retorna409() throws Exception {
        criarFuncionario("11122233396", "F-001", null);
        long espId = criarEspecialidade();

        // Mesmo CPF já é login de um funcionário
        mockMvc.perform(post("/medicos").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "11122233396")))
                .andExpect(status().isConflict());
    }

    @Test
    void servico_comClientCredentials_leMasNaoApaga() throws Exception {
        long pacienteId = criarPaciente(admin(), "98765432100");
        RequestPostProcessor servico = Acessos.grupo("service-account-ms-agendamento", Acessos.SERVICO);

        mockMvc.perform(get("/pacientes/{id}", pacienteId).with(servico)).andExpect(status().isOk());
        mockMvc.perform(get("/integracao/pacientes/{id}", pacienteId).with(servico)).andExpect(status().isOk());
        mockMvc.perform(delete("/pacientes/{id}", pacienteId).with(servico)).andExpect(status().isForbidden());
    }
}
