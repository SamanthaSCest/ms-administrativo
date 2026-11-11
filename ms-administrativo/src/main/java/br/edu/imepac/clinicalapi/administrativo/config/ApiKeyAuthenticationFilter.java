package br.edu.imepac.clinicalapi.administrativo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Autenticação serviço-a-serviço: se o header {@code X-Api-Key} bater com {@code seguranca.integracao.api-key},
 * a requisição é autenticada como "servico" com ROLE_SERVICO. Chave ausente ou errada: segue sem autenticação
 * (e as regras do SecurityConfig devolvem 401). Com a chave configurada vazia, o filtro fica inativo.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    static final String PRINCIPAL = "servico";

    private final byte[] chaveEsperada;

    public ApiKeyAuthenticationFilter(String apiKey) {
        this.chaveEsperada = apiKey == null || apiKey.isBlank() ? null : apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebida = request.getHeader(HEADER);
        if (chaveEsperada != null && recebida != null
                // Comparação em tempo constante (evita descobrir a chave medindo o tempo de resposta)
                && MessageDigest.isEqual(chaveEsperada, recebida.getBytes(StandardCharsets.UTF_8))) {
            var auth = new UsernamePasswordAuthenticationToken(PRINCIPAL, null,
                    AuthorityUtils.createAuthorityList("ROLE_SERVICO"));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
