package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo real de autenticação (sem @WithMockUser): login com o admin criado pelo AdminInicializador,
 * uso do Bearer token, regras por perfil e autenticação de serviço via X-Api-Key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final String ADMIN_SENHA = "admin12345";
    private static final String API_KEY = "dev-integracao-key";

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

    @BeforeEach
    void limpar() {
        // Remove pessoas (e seus usuários, em cascata); o admin inicial permanece
        funcionarioRepository.deleteAll();
        medicoRepository.deleteAll();
        pacienteRepository.deleteAll();
        especialidadeRepository.deleteAll();
    }

    private String token(String login, String senha) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private long criarPaciente(String adminToken, String cpf) throws Exception {
        MvcResult result = mockMvc.perform(post("/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"João Pereira","cpf":"%s","dataNascimento":"1990-01-15"}
                                """.formatted(cpf)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void criarFuncionario(String adminToken, String cpf, String matricula) throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson(cpf, matricula, null)))
                .andExpect(status().isCreated());
    }

    private long criarEspecialidade(String adminToken) throws Exception {
        MvcResult esp = mockMvc.perform(post("/especialidades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
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

    @Test
    void login_admin_retornaTokenEMe() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"senha\":\"admin12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(7200))
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("accessToken").asText()).contains(".");

        mockMvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(json.get("accessToken").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(ADMIN_LOGIN))
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void login_senhaErrada_retorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Login ou senha inválidos"));
    }

    @Test
    void login_semCampos_retorna400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
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
    void rotasPublicas_naoExigemToken() throws Exception {
        // (health responde 503 nos testes por não haver RabbitMQ; info basta para provar que é público)
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
        mockMvc.perform(get("/contract/openapi-administrativo.yaml")).andExpect(status().isOk());
    }

    @Test
    void admin_cadastraFuncionario_eFuncionarioFazLoginComCpf() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);

        MvcResult result = mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson("11122233344", "F-001", null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.matricula").value("F-001"))
                .andExpect(jsonPath("$.cargo").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.dataAdmissao").value("2024-02-01"))
                .andExpect(jsonPath("$.usuario.login").value("11122233344"))
                .andExpect(jsonPath("$.usuario.perfil").value("FUNCIONARIO"))
                .andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        String funcionario = token("11122233344", "funcSenha123");

        // Funcionário gerencia pacientes...
        criarPaciente(funcionario, "98765432100");
        // ...mas não cadastra funcionários nem médicos
        mockMvc.perform(get("/funcionarios").header(HttpHeaders.AUTHORIZATION, bearer(funcionario)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(post("/especialidades").header(HttpHeaders.AUTHORIZATION, bearer(funcionario))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Cardio\"}"))
                .andExpect(status().isForbidden());

        // Remover o funcionário remove o usuário (login deixa de funcionar)
        mockMvc.perform(delete("/funcionarios/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isNoContent());
        assertThat(usuarioRepository.existsByLogin("11122233344")).isFalse();
    }

    @Test
    void funcionario_semSenha_retorna400() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        String semSenha = funcionarioJson("11122233344", "F-001", null).replace(",\"senha\":\"funcSenha123\"", "");

        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(semSenha))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("senha"));
    }

    @Test
    void funcionario_cargoInexistente_retorna400() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);

        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson("11122233344", "F-001", "MEDICO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void funcionario_cargoAdministrador_acessaGestaoDeUsuarios() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        mockMvc.perform(post("/funcionarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(funcionarioJson("55566677788", "F-002", "ADMINISTRADOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.perfil").value("ADMIN"));

        String novoAdmin = token("55566677788", "funcSenha123");
        mockMvc.perform(get("/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(novoAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void paciente_naoTemUsuarioNemLogin() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        // Mesmo que alguém envie "senha", o campo não existe para paciente e é ignorado
        mockMvc.perform(post("/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"João Pereira","cpf":"98765432100","dataNascimento":"1990-01-15","senha":"qualquer123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").doesNotExist());

        assertThat(usuarioRepository.existsByLogin("98765432100")).isFalse();
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"98765432100\",\"senha\":\"qualquer123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_desativaUsuario_eLoginPassaAFalhar() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        criarFuncionario(admin, "11122233344", "F-001");
        long usuarioId = usuarioRepository.findByLogin("11122233344").orElseThrow().getId();

        mockMvc.perform(patch("/usuarios/{id}/status", usuarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"11122233344\",\"senha\":\"funcSenha123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_naoPodeDesativarASiMesmo() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        long adminId = usuarioRepository.findByLogin(ADMIN_LOGIN).orElseThrow().getId();

        mockMvc.perform(patch("/usuarios/{id}/status", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void alterarSenha_doProprioUsuario() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        criarFuncionario(admin, "11122233344", "F-001");
        String funcionario = token("11122233344", "funcSenha123");

        mockMvc.perform(put("/auth/senha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(funcionario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"errada\",\"novaSenha\":\"novaSenha123\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/auth/senha")
                        .header(HttpHeaders.AUTHORIZATION, bearer(funcionario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\":\"funcSenha123\",\"novaSenha\":\"novaSenha123\"}"))
                .andExpect(status().isNoContent());

        token("11122233344", "novaSenha123");
    }

    @Test
    void medico_cadastradoRecebeUsuarioComPerfilMedico() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        long espId = criarEspecialidade(admin);

        mockMvc.perform(post("/medicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "12345678901")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.perfil").value("MEDICO"));

        String medico = token("12345678901", "medicoSenha1");
        // Médico consulta pacientes, mas não cadastra
        mockMvc.perform(get("/pacientes").header(HttpHeaders.AUTHORIZATION, bearer(medico)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/pacientes").header(HttpHeaders.AUTHORIZATION, bearer(medico))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void funcionarioPodeTerOMesmoCpfDeUmPaciente() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        criarPaciente(admin, "11122233344");

        // Paciente não ocupa login, então a funcionária que também é paciente pode ser cadastrada
        criarFuncionario(admin, "11122233344", "F-001");
    }

    @Test
    void cpfJaUsadoComoLogin_retorna409() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        criarFuncionario(admin, "11122233344", "F-001");
        long espId = criarEspecialidade(admin);

        // Mesmo CPF já é login de um funcionário
        mockMvc.perform(post("/medicos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(medicoJson(espId, "11122233344")))
                .andExpect(status().isConflict());
    }

    @Test
    void servico_comApiKey_leMedicoEPaciente() throws Exception {
        String admin = token(ADMIN_LOGIN, ADMIN_SENHA);
        long pacienteId = criarPaciente(admin, "98765432100");

        mockMvc.perform(get("/pacientes/{id}", pacienteId).header("X-Api-Key", API_KEY))
                .andExpect(status().isOk());
        mockMvc.perform(get("/integracao/pacientes/{id}", pacienteId).header("X-Api-Key", API_KEY))
                .andExpect(status().isOk());
        // Serviço só lê
        mockMvc.perform(delete("/pacientes/{id}", pacienteId).header("X-Api-Key", API_KEY))
                .andExpect(status().isForbidden());
        // Chave errada = não autenticado
        mockMvc.perform(get("/pacientes/{id}", pacienteId).header("X-Api-Key", "errada"))
                .andExpect(status().isUnauthorized());
    }
}
