package br.edu.imepac.clinicalapi.administrativo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades {@code keycloak.*} do application.yml.
 *
 * @param url          endereço do Keycloak que este serviço usa para buscar as chaves públicas e chamar a Admin API
 *                     (no docker-compose, o endereço interno {@code http://keycloak:8080})
 * @param realm        realm da clínica
 * @param issuer       valor esperado no claim "iss" dos tokens (o endereço público do Keycloak + realm)
 * @param clientId     client deste serviço: dono das roles de permissão e da service account da Admin API
 * @param clientSecret segredo desse client (client credentials para a Admin API)
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(String url, String realm, String issuer, String clientId, String clientSecret) {

    public String jwkSetUri() {
        return realmUri() + "/protocol/openid-connect/certs";
    }

    public String tokenUri() {
        return realmUri() + "/protocol/openid-connect/token";
    }

    /** Base da Admin API do realm: {@code <url>/admin/realms/<realm>}. */
    public String adminUri() {
        return semBarraFinal(url) + "/admin/realms/" + realm;
    }

    private String realmUri() {
        return semBarraFinal(url) + "/realms/" + realm;
    }

    private static String semBarraFinal(String valor) {
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }
}
