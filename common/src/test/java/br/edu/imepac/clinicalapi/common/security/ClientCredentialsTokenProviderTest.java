package br.edu.imepac.clinicalapi.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClientCredentialsTokenProviderTest {

    private static final String URI = "http://kc/realms/clinical/protocol/openid-connect/token";

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;
    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-01-01T10:00:00Z"));
    private final Clock relogio = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora.get();
        }
    };

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
    }

    private ClientCredentialsTokenProvider provedor() {
        return new ClientCredentialsTokenProvider(builder, URI, "ms-agendamento", "segredo", relogio);
    }

    private void esperarToken(String token) {
        servidor.expect(once(), requestTo(URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic bXMtYWdlbmRhbWVudG86c2VncmVkbw=="))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string("grant_type=client_credentials"))
                .andRespond(withSuccess("{\"access_token\":\"%s\",\"expires_in\":300,\"token_type\":\"Bearer\"}"
                        .formatted(token), MediaType.APPLICATION_JSON));
    }

    @Test
    void reusaOTokenAtePertoDeExpirar() {
        esperarToken("t1");
        ClientCredentialsTokenProvider provedor = provedor();

        assertThat(provedor.token()).isEqualTo("t1");
        agora.set(agora.get().plusSeconds(269));  // 300 - 30 de margem - 1
        assertThat(provedor.token()).isEqualTo("t1");
        servidor.verify();

        servidor.reset();
        esperarToken("t2");
        agora.set(agora.get().plusSeconds(1));  // entrou na margem: renova
        assertThat(provedor.token()).isEqualTo("t2");
        servidor.verify();
    }

    @Test
    void invalidar_forcaNovoPedido() {
        esperarToken("t1");
        ClientCredentialsTokenProvider provedor = provedor();
        provedor.token();
        servidor.verify();

        servidor.reset();
        esperarToken("t2");
        provedor.invalidar();
        assertThat(provedor.token()).isEqualTo("t2");
    }

    @Test
    void interceptor_mandaBearer_eNo401RenovaETentaDeNovoUmaVez() {
        // Um único servidor simulado atende o token e a API (o builder do provedor é um clone do mesmo)
        esperarToken("t1");
        servidor.expect(once(), requestTo("http://api/recurso"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer t1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        esperarToken("t2");
        servidor.expect(once(), requestTo("http://api/recurso"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer t2"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));
        ClientCredentialsTokenProvider provedor = provedor();
        RestClient api = builder.clone().requestInterceptor(provedor.interceptor()).build();

        assertThat(api.get().uri("http://api/recurso").retrieve().body(String.class)).isEqualTo("ok");
        servidor.verify();
    }

    @Test
    void respostaSemAccessToken_erro() {
        servidor.expect(requestTo(URI)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        ClientCredentialsTokenProvider provedor = provedor();

        assertThatThrownBy(provedor::token).isInstanceOf(IllegalStateException.class);
    }
}
