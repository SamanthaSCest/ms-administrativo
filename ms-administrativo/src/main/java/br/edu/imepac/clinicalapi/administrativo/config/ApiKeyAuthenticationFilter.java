package br.edu.imepac.clinicalapi.administrativo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Stream;

/**
 * Autenticação serviço-a-serviço: se o header {@code X-Api-Key} bater com {@code seguranca.integracao.api-key},
 * a requisição é autenticada como "servico" com ROLE_SERVICO e as permissões de {@link Permissao#DO_SERVICO}
 * (leitura dos cadastros e escrita de insumos, para o ms-estoque). Chave ausente ou errada: segue sem autenticação
 * (e as regras do SecurityConfig devolvem 401). Com a chave configurada vazia, o filtro fica inativo.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    static final String PRINCIPAL = "servico";
    /** ROLE_SERVICO identifica o chamador; as permissões são as de {@link Permissao#DO_SERVICO}. */
    static final List<GrantedAuthority> AUTHORITIES = Stream.concat(Stream.of("ROLE_SERVICO"),
                    Permissao.DO_SERVICO.stream().map(Enum::name))
            .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
            .toList();

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
            var auth = new UsernamePasswordAuthenticationToken(PRINCIPAL, null, AUTHORITIES);
            // Contexto novo (e não getContext().setAuthentication): não altera um contexto compartilhado
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(auth);
            SecurityContextHolder.setContext(contexto);
        }
        chain.doFilter(request, response);
    }
}
