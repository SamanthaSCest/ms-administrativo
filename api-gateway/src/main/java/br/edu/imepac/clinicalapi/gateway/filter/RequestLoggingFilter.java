package br.edu.imepac.clinicalapi.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Registra método, caminho, status e duração de cada requisição que passa pelo gateway. */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long inicio = System.currentTimeMillis();
        String metodo = exchange.getRequest().getMethod().name();
        String caminho = exchange.getRequest().getURI().getPath();
        return chain.filter(exchange).doFinally(signal -> log.info("{} {} -> {} ({} ms)", metodo, caminho,
                exchange.getResponse().getStatusCode(), System.currentTimeMillis() - inicio));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
