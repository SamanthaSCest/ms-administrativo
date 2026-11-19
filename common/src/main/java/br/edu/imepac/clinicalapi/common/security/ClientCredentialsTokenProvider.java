package br.edu.imepac.clinicalapi.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Token de acesso de um serviço (OAuth2 client credentials, ex.: Keycloak) para chamar outro serviço.
 * Guarda o token em memória e só pede outro quando falta menos de {@link #MARGEM} para expirar.
 * Classe simples (sem o spring-boot-starter-oauth2-client) para não ligar o Spring Security em quem só é cliente.
 */
public class ClientCredentialsTokenProvider {

    /** Renova um pouco antes de expirar: evita mandar um token que expira no caminho. */
    static final Duration MARGEM = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String token;
    private Instant expiraEm = Instant.MIN;

    public ClientCredentialsTokenProvider(RestClient.Builder builder, String tokenUri, String clientId,
                                          String clientSecret) {
        this(builder, tokenUri, clientId, clientSecret, Clock.systemUTC());
    }

    ClientCredentialsTokenProvider(RestClient.Builder builder, String tokenUri, String clientId, String clientSecret,
                                   Clock clock) {
        // Builder próprio (clone): os interceptors de quem usa o token não podem entrar no pedido do token
        this.restClient = builder.clone().build();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    /** Token válido (do cache ou recém-emitido). */
    public synchronized String token() {
        Instant agora = clock.instant();
        if (token == null || !agora.isBefore(expiraEm.minus(MARGEM))) {
            TokenResponse resposta = pedirToken();
            token = resposta.accessToken();
            expiraEm = agora.plusSeconds(resposta.expiresIn());
        }
        return token;
    }

    /** Descarta o token em cache (ex.: o servidor respondeu 401 com ele). */
    public synchronized void invalidar() {
        token = null;
    }

    /**
     * Interceptor que põe {@code Authorization: Bearer <token>} em toda requisição do RestClient. Se a resposta
     * for 401 (ex.: o Keycloak reiniciou e trocou as chaves), descarta o token e tenta mais uma vez com um novo.
     */
    public ClientHttpRequestInterceptor interceptor() {
        return (request, body, execution) -> {
            String usado = token();
            request.getHeaders().setBearerAuth(usado);
            ClientHttpResponse resposta = execution.execute(request, body);
            if (resposta.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                return resposta;
            }
            resposta.close();
            invalidarSeFor(usado);
            request.getHeaders().setBearerAuth(token());
            return execution.execute(request, body);
        };
    }

    /** Só descarta se ainda for o mesmo token (outra thread pode já ter renovado). */
    private synchronized void invalidarSeFor(String usado) {
        if (usado.equals(token)) {
            token = null;
        }
    }

    private TokenResponse pedirToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        TokenResponse resposta = restClient.post()
                .uri(tokenUri)
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (resposta == null || resposta.accessToken() == null) {
            throw new IllegalStateException("Resposta sem access_token do servidor de autorização: " + tokenUri);
        }
        return resposta;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }
}
