package br.edu.imepac.clinicalapi.administrativo.identidade;

import br.edu.imepac.clinicalapi.administrativo.config.KeycloakProperties;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ExternalServiceException;
import br.edu.imepac.clinicalapi.common.security.ClientCredentialsTokenProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link ProvedorIdentidade} pela Admin API do Keycloak ({@code /admin/realms/clinical/...}), autenticada com a
 * service account do client ms-administrativo (roles manage-users, view-users, query-users e query-groups do
 * realm-management, ver keycloak/realm-clinical.json).
 */
@Component
public class KeycloakProvedorIdentidade implements ProvedorIdentidade {

    private static final Logger log = LoggerFactory.getLogger(KeycloakProvedorIdentidade.class);
    private static final ParameterizedTypeReference<List<GrupoKeycloak>> LISTA_GRUPOS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };
    /** Limite da listagem de grupos (o realm tem poucos; evita a paginação padrão de 100 do Keycloak). */
    private static final int MAX_GRUPOS = 1000;
    private static final String USUARIO = "/users/{id}";

    private final RestClient admin;

    public KeycloakProvedorIdentidade(RestClient.Builder builder, KeycloakProperties keycloak) {
        ClientCredentialsTokenProvider tokens = new ClientCredentialsTokenProvider(builder, keycloak.tokenUri(),
                keycloak.clientId(), keycloak.clientSecret());
        this.admin = builder.clone()
                .baseUrl(keycloak.adminUri())
                .requestInterceptor(tokens.interceptor())
                .build();
    }

    @Override
    public String criarUsuario(NovoUsuario usuario) {
        Map<String, Grupo> grupos = gruposPorNome(usuario.grupos());
        UsuarioKeycloak corpo = new UsuarioKeycloak(usuario.login(), usuario.ativo(), primeiroNome(usuario.nome()),
                sobrenome(usuario.nome()), vazioComoNulo(usuario.email()),
                List.of(new Credencial(usuario.senha())), null);
        ResponseEntity<Void> resposta = chamar("criar usuário", () -> admin.post().uri("/users").body(corpo)
                .retrieve().toBodilessEntity());
        String id = idDoLocation(resposta.getHeaders().getLocation());
        // Grupos depois de criar (por id): o "groups" do corpo de criação ignora grupo inexistente em silêncio
        grupos.values().forEach(g -> entrarNoGrupo(id, g));
        log.info("Usuário {} criado no Keycloak (grupos {})", id, grupos.keySet());
        return id;
    }

    @Override
    public void atualizarDados(String id, String login, String nome, String email) {
        alterar(id, "atualizar usuário", usuario -> {
            usuario.put("username", login);
            usuario.put("firstName", primeiroNome(nome));
            usuario.put("lastName", sobrenome(nome));
            usuario.put("email", vazioComoNulo(email));
        });
    }

    @Override
    public void definirSenha(String id, String senha) {
        chamar("definir senha", () -> admin.put().uri("/users/{id}/reset-password", id).body(new Credencial(senha))
                .retrieve().toBodilessEntity());
    }

    @Override
    public void definirAtivo(String id, boolean ativo) {
        alterar(id, "alterar situação do usuário", usuario -> usuario.put("enabled", ativo));
    }

    /**
     * Lê a representação inteira do usuário, aplica a mudança e devolve tudo no PUT: assim nenhum atributo que
     * não foi tocado se perde, qualquer que seja a regra de atualização parcial da versão do Keycloak.
     */
    private void alterar(String id, String operacao, Consumer<Map<String, Object>> mudanca) {
        Map<String, Object> atual = chamar("consultar usuário", () -> admin.get().uri(USUARIO, id)
                .retrieve().body(MAPA));
        if (atual == null) {
            throw new BusinessRuleException("Usuário não encontrado no Keycloak: " + id);
        }
        mudanca.accept(atual);
        chamar(operacao, () -> admin.put().uri(USUARIO, id).body(atual).retrieve().toBodilessEntity());
    }

    @Override
    public List<String> grupos(String id) {
        return gruposDoUsuario(id).stream().map(GrupoKeycloak::name).sorted().toList();
    }

    @Override
    public void definirGrupos(String id, Set<String> nomes) {
        Map<String, Grupo> desejados = gruposPorNome(nomes);
        List<GrupoKeycloak> atuais = gruposDoUsuario(id);
        Set<String> atuaisNomes = new HashSet<>();
        for (GrupoKeycloak atual : atuais) {
            atuaisNomes.add(atual.name());
            if (!desejados.containsKey(atual.name())) {
                chamar("remover do grupo", () -> admin.delete().uri("/users/{id}/groups/{grupo}", id, atual.id())
                        .retrieve().toBodilessEntity());
            }
        }
        desejados.values().stream().filter(g -> !atuaisNomes.contains(g.nome())).forEach(g -> entrarNoGrupo(id, g));
    }

    @Override
    public void removerUsuario(String id) {
        try {
            chamar("remover usuário", () -> admin.delete().uri(USUARIO, id).retrieve().toBodilessEntity());
        } catch (BusinessRuleException e) {
            // 404: já não existe, que é o que se queria
            log.debug("Usuário {} já não existia no Keycloak", id);
        }
    }

    @Override
    public List<String> listarGrupos() {
        return todosOsGrupos().stream().map(GrupoKeycloak::name).sorted(Comparator.naturalOrder()).toList();
    }

    private void entrarNoGrupo(String id, Grupo grupo) {
        chamar("adicionar ao grupo", () -> admin.put().uri("/users/{id}/groups/{grupo}", id, grupo.id())
                .retrieve().toBodilessEntity());
    }

    private List<GrupoKeycloak> gruposDoUsuario(String id) {
        List<GrupoKeycloak> grupos = chamar("consultar grupos do usuário", () -> admin.get()
                .uri("/users/{id}/groups?briefRepresentation=true&max={max}", id, MAX_GRUPOS)
                .retrieve().body(LISTA_GRUPOS));
        return grupos == null ? List.of() : grupos;
    }

    private List<GrupoKeycloak> todosOsGrupos() {
        List<GrupoKeycloak> grupos = chamar("listar grupos", () -> admin.get()
                .uri("/groups?briefRepresentation=true&max={max}", MAX_GRUPOS)
                .retrieve().body(LISTA_GRUPOS));
        return grupos == null ? List.of() : grupos;
    }

    /** Ids dos grupos pelos nomes; 422 se algum não existir no realm. */
    private Map<String, Grupo> gruposPorNome(Set<String> nomes) {
        Map<String, Grupo> encontrados = new HashMap<>();
        if (nomes == null || nomes.isEmpty()) {
            return encontrados;
        }
        todosOsGrupos().stream().filter(g -> nomes.contains(g.name()))
                .forEach(g -> encontrados.put(g.name(), new Grupo(g.id(), g.name())));
        Set<String> faltando = new HashSet<>(nomes);
        faltando.removeAll(encontrados.keySet());
        if (!faltando.isEmpty()) {
            throw new BusinessRuleException("Grupo(s) de acesso não encontrado(s): " + faltando.stream().sorted().toList());
        }
        return encontrados;
    }

    /**
     * Traduz os erros da Admin API: 409 (login/e-mail já usado) -> 409; 400/404 -> 422 com a mensagem do Keycloak;
     * demais (401/403 da service account, 5xx, rede) -> 503.
     */
    private <T> T chamar(String operacao, Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException("Já existe usuário com o login informado");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessRuleException("Keycloak recusou " + operacao + ": " + mensagem(e));
            }
            log.error("Keycloak respondeu {} ao {}: {}", e.getStatusCode(), operacao, e.getResponseBodyAsString());
            throw new ExternalServiceException("Falha ao " + operacao + " no Keycloak", e);
        } catch (RestClientResponseException e) {
            log.error("Keycloak respondeu {} ao {}", e.getStatusCode(), operacao);
            throw new ExternalServiceException("Falha ao " + operacao + " no Keycloak", e);
        } catch (RestClientException e) {
            log.error("Keycloak indisponível ao {}: {}", operacao, e.getMessage());
            throw new ExternalServiceException("Keycloak indisponível", e);
        }
    }

    /**
     * Texto legível do erro do Keycloak: {"errorMessage": "..."} (ex.: usuário já existe) ou
     * {"error": "<código>", "error_description": "..."} (ex.: política de senha). Sem corpo, vai o status.
     */
    private static String mensagem(RestClientResponseException e) {
        try {
            Map<?, ?> corpo = e.getResponseBodyAs(Map.class);
            if (corpo != null) {
                for (String campo : List.of("errorMessage", "error_description", "error")) {
                    if (corpo.get(campo) != null) {
                        return corpo.get(campo).toString();
                    }
                }
            }
        } catch (RuntimeException ignorada) {
            // corpo que não é JSON: cai no status
        }
        return e.getStatusText();
    }

    private static String idDoLocation(URI location) {
        if (location == null) {
            throw new ExternalServiceException("Keycloak não devolveu o id do usuário criado", null);
        }
        String caminho = location.getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    static String primeiroNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String limpo = nome.strip();
        int espaco = limpo.indexOf(' ');
        return espaco < 0 ? limpo : limpo.substring(0, espaco);
    }

    static String sobrenome(String nome) {
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String limpo = nome.strip();
        int espaco = limpo.indexOf(' ');
        return espaco < 0 ? null : limpo.substring(espaco + 1).strip();
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private record Grupo(String id, String nome) {
    }

    record GrupoKeycloak(String id, String name, String path) {
    }

    /** Campos nulos não vão no JSON: o PUT do Keycloak só altera o que foi enviado. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record UsuarioKeycloak(String username, Boolean enabled, String firstName, String lastName, String email,
                           List<Credencial> credentials, List<String> groups) {
    }

    record Credencial(String type, String value, boolean temporary) {
        Credencial(String senha) {
            this("password", senha, false);
        }
    }
}
