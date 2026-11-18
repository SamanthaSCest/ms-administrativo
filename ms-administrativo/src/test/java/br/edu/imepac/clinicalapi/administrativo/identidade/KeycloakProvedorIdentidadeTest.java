package br.edu.imepac.clinicalapi.administrativo.identidade;

import br.edu.imepac.clinicalapi.administrativo.config.KeycloakProperties;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Chamadas à Admin API do Keycloak (sem Keycloak de verdade: MockRestServiceServer). */
class KeycloakProvedorIdentidadeTest {

    private static final String KC = "http://kc";
    private static final String TOKEN = KC + "/realms/clinical/protocol/openid-connect/token";
    private static final String ADMIN = KC + "/admin/realms/clinical";
    private static final String GRUPOS = """
            [{"id":"g-adm","name":"ADMINISTRADOR","path":"/ADMINISTRADOR"},
             {"id":"g-rec","name":"RECEPCIONISTA","path":"/RECEPCIONISTA"},
             {"id":"g-med","name":"MEDICO","path":"/MEDICO"}]
            """;

    private MockRestServiceServer servidor;
    private KeycloakProvedorIdentidade provedor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        provedor = new KeycloakProvedorIdentidade(builder,
                new KeycloakProperties(KC, "clinical", KC + "/realms/clinical", "ms-administrativo", "segredo"));
    }

    private void esperarToken() {
        servidor.expect(once(), requestTo(TOKEN))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic bXMtYWRtaW5pc3RyYXRpdm86c2VncmVkbw=="))
                .andRespond(withSuccess("{\"access_token\":\"tk\",\"expires_in\":300}", MediaType.APPLICATION_JSON));
    }

    private void esperarListaDeGrupos() {
        servidor.expect(requestTo(ADMIN + "/groups?briefRepresentation=true&max=1000"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer tk"))
                .andRespond(withSuccess(GRUPOS, MediaType.APPLICATION_JSON));
    }

    @Test
    void criarUsuario_criaComSenhaENomeEDepoisEntraNosGrupos() {
        esperarToken();
        esperarListaDeGrupos();
        servidor.expect(requestTo(ADMIN + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("11122233396"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Lima Souza"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].value").value("senhaForte1"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andRespond(withStatus(HttpStatus.CREATED).location(URI.create(ADMIN + "/users/u-1")));
        servidor.expect(requestTo(ADMIN + "/users/u-1/groups/g-rec"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        String id = provedor.criarUsuario(new ProvedorIdentidade.NovoUsuario("11122233396", "senhaForte1",
                " Ana Lima Souza ", "", true, Set.of("RECEPCIONISTA")));

        assertThat(id).isEqualTo("u-1");
        servidor.verify();
    }

    @Test
    void criarUsuario_loginJaExiste_conflict() {
        esperarToken();
        servidor.expect(requestTo(ADMIN + "/users"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorMessage\":\"User exists with same username\"}"));

        assertThatThrownBy(() -> provedor.criarUsuario(new ProvedorIdentidade.NovoUsuario("111", "senhaForte1",
                "Ana", null, true, Set.of()))).isInstanceOf(ConflictException.class);
    }

    @Test
    void criarUsuario_grupoInexistente_422SemCriarNada() {
        esperarToken();
        esperarListaDeGrupos();

        assertThatThrownBy(() -> provedor.criarUsuario(new ProvedorIdentidade.NovoUsuario("111", "senhaForte1",
                "Ana", null, true, Set.of("RECEPCIONISTA", "NAO_EXISTE"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("NAO_EXISTE");
        servidor.verify();  // não houve POST /users
    }

    @Test
    void senhaRecusadaPelaPolitica_422ComAMensagemDoKeycloak() {
        esperarToken();
        servidor.expect(requestTo(ADMIN + "/users/u-1/reset-password"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalidPasswordMinLengthMessage\"}"));

        assertThatThrownBy(() -> provedor.definirSenha("u-1", "curta"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("invalidPasswordMinLengthMessage");
    }

    @Test
    void definirAtivo_devolveARepresentacaoInteiraSoComEnabledTrocado() {
        esperarToken();
        servidor.expect(requestTo(ADMIN + "/users/u-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"u-1","username":"111","enabled":true,"firstName":"Ana","email":"a@b.c",
                         "attributes":{"setor":["Recepção"]}}
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(ADMIN + "/users/u-1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.attributes.setor[0]").value("Recepção"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        provedor.definirAtivo("u-1", false);

        servidor.verify();
    }

    @Test
    void definirGrupos_sai_dosQueSobram_eEntra_nosQueFaltam_reusandoOToken() {
        esperarToken();  // once(): as quatro chamadas usam o mesmo token
        esperarListaDeGrupos();
        servidor.expect(requestTo(ADMIN + "/users/u-1/groups?briefRepresentation=true&max=1000"))
                .andRespond(withSuccess("""
                        [{"id":"g-rec","name":"RECEPCIONISTA","path":"/RECEPCIONISTA"},
                         {"id":"g-med","name":"MEDICO","path":"/MEDICO"}]
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(ADMIN + "/users/u-1/groups/g-rec"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        servidor.expect(requestTo(ADMIN + "/users/u-1/groups/g-adm"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        provedor.definirGrupos("u-1", Set.of("MEDICO", "ADMINISTRADOR"));

        servidor.verify();
    }

    @Test
    void removerUsuario_jaRemovido_naoEErro() {
        esperarToken();
        servidor.expect(requestTo(ADMIN + "/users/u-1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        provedor.removerUsuario("u-1");

        servidor.verify();
    }

    @Test
    void keycloakForaDoAr_503() {
        servidor.expect(requestTo(TOKEN)).andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> provedor.listarGrupos()).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void serviceAccountSemPermissao_503() {
        esperarToken();
        servidor.expect(requestTo(ADMIN + "/groups?briefRepresentation=true&max=1000"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> provedor.listarGrupos()).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void nome_separaPrimeiroNomeESobrenome() {
        assertThat(KeycloakProvedorIdentidade.primeiroNome("Maria")).isEqualTo("Maria");
        assertThat(KeycloakProvedorIdentidade.sobrenome("Maria")).isNull();
        assertThat(KeycloakProvedorIdentidade.primeiroNome("Maria da Silva")).isEqualTo("Maria");
        assertThat(KeycloakProvedorIdentidade.sobrenome("Maria da Silva")).isEqualTo("da Silva");
        assertThat(KeycloakProvedorIdentidade.primeiroNome("  ")).isNull();
    }
}
