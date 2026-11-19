package br.edu.imepac.clinicalapi.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * O gateway barra quem chega sem token válido (401) e deixa passar o resto para o serviço. Os serviços não estão
 * no ar no teste, então "passou" aparece como erro de roteamento (5xx), nunca 401.
 */
@SpringBootTest(properties = {
        "spring.cloud.gateway.httpclient.connect-timeout=200",
        // Portas fechadas: o roteamento falha rápido em vez de esperar
        "ADMINISTRATIVO_URL=http://localhost:1",
        "AGENDAMENTO_URL=http://localhost:1"
})
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient client;

    @Test
    void semToken_401() {
        client.get().uri("/administrativo/medicos").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*");
        client.get().uri("/agendamento/consultas").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void tokenInvalido_401() {
        client.get().uri("/administrativo/medicos").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void comToken_seguePara_oServico() {
        HttpStatus status = (HttpStatus) client.mutateWith(mockJwt()).get().uri("/administrativo/medicos")
                .exchange().returnResult(Void.class).getStatus();
        assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void contratoESwaggerDosServicos_saoPublicos() {
        HttpStatus contrato = (HttpStatus) client.get().uri("/administrativo/contract/openapi-administrativo.yaml")
                .exchange().returnResult(Void.class).getStatus();
        assertThat(contrato).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
