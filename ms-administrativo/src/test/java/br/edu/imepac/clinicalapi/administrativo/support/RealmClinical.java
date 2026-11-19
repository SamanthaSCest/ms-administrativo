package br.edu.imepac.clinicalapi.administrativo.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê o realm versionado (keycloak/realm-clinical.json, na raiz do repositório): os testes usam as mesmas roles
 * por grupo e por service account que o Keycloak de verdade, sem duplicar a tabela de permissões no código.
 */
public final class RealmClinical {

    public static final String CLIENT = "ms-administrativo";
    /** Pseudo-grupo dos testes: a service account do ms-agendamento (client credentials). */
    public static final String SERVICO = "SERVICO";
    private static final String CLIENT_SERVICO = "ms-agendamento";

    private static final Map<String, List<String>> ROLES_POR_GRUPO = carregar();

    private RealmClinical() {
    }

    /** Roles do client ms-administrativo do grupo (ou da service account, para {@link #SERVICO}). */
    public static List<String> roles(String grupo) {
        List<String> roles = ROLES_POR_GRUPO.get(grupo);
        if (roles == null) {
            throw new IllegalArgumentException("Grupo não existe em realm-clinical.json: " + grupo);
        }
        return roles;
    }

    public static List<String> grupos() {
        return ROLES_POR_GRUPO.keySet().stream().filter(g -> !SERVICO.equals(g)).sorted().toList();
    }

    private static Map<String, List<String>> carregar() {
        JsonNode realm = ler();
        Map<String, List<String>> mapa = new LinkedHashMap<>();
        for (JsonNode grupo : realm.path("groups")) {
            mapa.put(grupo.path("name").asText(), lista(grupo.path("clientRoles").path(CLIENT)));
        }
        for (JsonNode usuario : realm.path("users")) {
            if (CLIENT_SERVICO.equals(usuario.path("serviceAccountClientId").asText())) {
                mapa.put(SERVICO, lista(usuario.path("clientRoles").path(CLIENT)));
            }
        }
        return mapa;
    }

    private static List<String> lista(JsonNode array) {
        List<String> itens = new ArrayList<>();
        array.forEach(n -> itens.add(n.asText()));
        return List.copyOf(itens);
    }

    /** Os testes rodam com o diretório do módulo como atual; aceita também a raiz do repositório. */
    private static JsonNode ler() {
        for (Path caminho : List.of(Path.of("..", "keycloak", "realm-clinical.json"),
                Path.of("keycloak", "realm-clinical.json"))) {
            if (Files.exists(caminho)) {
                try {
                    return new ObjectMapper().readTree(caminho.toFile());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        throw new IllegalStateException("keycloak/realm-clinical.json não encontrado a partir de "
                + Path.of("").toAbsolutePath());
    }
}
