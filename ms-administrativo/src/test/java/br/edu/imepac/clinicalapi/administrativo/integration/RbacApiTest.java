package br.edu.imepac.clinicalapi.administrativo.integration;

import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.repositories.FuncionarioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import br.edu.imepac.clinicalapi.administrativo.support.Acessos;
import br.edu.imepac.clinicalapi.administrativo.support.IdentidadeEmMemoria;
import br.edu.imepac.clinicalapi.administrativo.support.RealmClinical;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC no Keycloak: o realm versionado tem que bater com o enum {@link Permissao}, e os grupos do usuário
 * acompanham o cadastro do funcionário (grupo do cargo + extras). Quem recebe cada permissão é o realm
 * (keycloak/realm-clinical.json); aqui só se checam as roles que chegam no token.
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
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private IdentidadeEmMemoria keycloak;

    @BeforeEach
    void limpar() {
        funcionarioRepository.deleteAll();
    }

    private static RequestPostProcessor admin() {
        return Acessos.grupo("admin", "ADMINISTRADOR");
    }

    private String funcionarioJson(String cargo) {
        return """
                {"nome":"Ana Lima","cpf":"%s","dataNascimento":"1992-03-10","matricula":"F-900",
                 "cargo":"%s","dataAdmissao":"2024-02-01","senha":"%s"}
                """.formatted(CPF_RECEPCAO, cargo, SENHA);
    }

    private static String comGrupos(String json, String grupos) {
        return json.replace("\"senha\"", "\"grupos\":[%s],\"senha\"".formatted(grupos));
    }

    private Set<String> gruposNoKeycloak() {
        String id = usuarioRepository.findByLogin(CPF_RECEPCAO).orElseThrow().getKeycloakId();
        return keycloak.usuario(id).orElseThrow().grupos;
    }

    @Test
    void realm_temUmaRoleParaCadaPermissao_eAdministradorTemTodas() {
        Set<String> doEnum = new HashSet<>(Arrays.stream(Permissao.values()).map(Enum::name).toList());

        assertThat(RealmClinical.roles("ADMINISTRADOR")).containsExactlyInAnyOrderElementsOf(doEnum);
        // Nenhum grupo (nem a service account) usa role que o SecurityConfig não conhece
        for (String grupo : RealmClinical.grupos()) {
            assertThat(doEnum).as("roles do grupo " + grupo).containsAll(RealmClinical.roles(grupo));
        }
        assertThat(doEnum).containsAll(RealmClinical.roles(RealmClinical.SERVICO));
    }

    @Test
    void realm_temUmGrupoParaCadaCargoEOGrupoMedico() {
        assertThat(RealmClinical.grupos()).containsExactlyInAnyOrder(
                "ADMINISTRADOR", "RECEPCIONISTA", "ESTOQUISTA", "RESPONSAVEL_FINANCEIRO", "MEDICO");
    }

    @Test
    void servico_soLeEEscreveInsumos() {
        List<String> servico = RealmClinical.roles(RealmClinical.SERVICO);
        assertThat(servico).contains("INTEGRACAO_CONSULTAR", "PACIENTE_CONSULTAR", "INSUMO_GERENCIAR")
                .noneMatch(r -> r.endsWith("_GERENCIAR") && !r.equals("INSUMO_GERENCIAR"));
    }

    @Test
    void cadastroDoFuncionario_comGrupoExtra_eTrocaNoPut() throws Exception {
        mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comGrupos(funcionarioJson("RECEPCIONISTA"), "\"MEDICO\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.grupos", containsInAnyOrder("MEDICO", "RECEPCIONISTA")));
        assertThat(gruposNoKeycloak()).containsExactlyInAnyOrder("MEDICO", "RECEPCIONISTA");
        long id = funcionarioRepository.findAll().getFirst().getId();

        // grupos vazio: fica só o grupo do cargo (agora ESTOQUISTA)
        mockMvc.perform(put("/funcionarios/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comGrupos(funcionarioJson("ESTOQUISTA"), "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.grupos", containsInAnyOrder("ESTOQUISTA")));
        assertThat(gruposNoKeycloak()).containsExactly("ESTOQUISTA");
    }

    @Test
    void trocarCargo_semInformarGrupos_trocaSoOGrupoDoCargo() throws Exception {
        mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comGrupos(funcionarioJson("RECEPCIONISTA"), "\"MEDICO\"")))
                .andExpect(status().isCreated());
        long id = funcionarioRepository.findAll().getFirst().getId();

        mockMvc.perform(put("/funcionarios/{id}", id).with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content(funcionarioJson("ESTOQUISTA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.grupos", containsInAnyOrder("MEDICO", "ESTOQUISTA")));
        assertThat(gruposNoKeycloak()).containsExactlyInAnyOrder("MEDICO", "ESTOQUISTA");
    }

    @Test
    void cadastroDoFuncionario_grupoInexistente_retorna422() throws Exception {
        mockMvc.perform(post("/funcionarios").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comGrupos(funcionarioJson("RECEPCIONISTA"), "\"NAO_EXISTE\"")))
                .andExpect(status().isUnprocessableEntity());
        assertThat(usuarioRepository.existsByLogin(CPF_RECEPCAO)).isFalse();
    }

    @Test
    void quemSoGerenciaFuncionarios_naoEscolheGrupos() throws Exception {
        // Um grupo "RH" criado no Keycloak: cadastra funcionários, mas não gerencia acessos
        RequestPostProcessor rh = Acessos.roles("rh", "FUNCIONARIO_CONSULTAR", "FUNCIONARIO_GERENCIAR");

        mockMvc.perform(post("/funcionarios").with(rh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comGrupos(funcionarioJson("RECEPCIONISTA"), "\"ADMINISTRADOR\"")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ADM-FUN-001"));

        // Sem "grupos" o cadastro segue normal (só o grupo do cargo)
        mockMvc.perform(post("/funcionarios").with(rh)
                        .contentType(MediaType.APPLICATION_JSON).content(funcionarioJson("RECEPCIONISTA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.grupos", containsInAnyOrder("RECEPCIONISTA")));
    }

    @Test
    void semRoles_naoAcessaNada_masVeOsProprios() throws Exception {
        RequestPostProcessor semAcesso = Acessos.roles("ninguem");
        mockMvc.perform(get("/pacientes").with(semAcesso)).andExpect(status().isForbidden());
        mockMvc.perform(get("/auth/me").with(semAcesso)).andExpect(status().isOk());
    }

    @Test
    void recepcionista_naoGerenciaAcessos() throws Exception {
        RequestPostProcessor recepcao = Acessos.grupo(CPF_RECEPCAO, "RECEPCIONISTA");
        mockMvc.perform(get("/grupos").with(recepcao)).andExpect(status().isForbidden());
        mockMvc.perform(get("/usuarios").with(recepcao)).andExpect(status().isForbidden());
    }
}
