package br.edu.imepac.clinicalapi.administrativo.config;

import br.edu.imepac.clinicalapi.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Erros de segurança acontecem nos filtros, ANTES do @RestControllerAdvice. Esta classe devolve o mesmo
 * formato {@link ErrorResponse} para 401 (sem token / token inválido) e 403 (perfil sem permissão).
 */
@Component
public class SegurancaErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SegurancaErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        // Padrão RFC 6750: indica ao cliente que deve enviar um Bearer token
        response.setHeader("WWW-Authenticate", "Bearer");
        escrever(request, response, HttpStatus.UNAUTHORIZED, "Autenticação necessária ou token inválido");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN, "Acesso negado para o perfil do usuário");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensagem)
            throws IOException {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(mensagem)
                .path(request.getRequestURI())
                .details(List.of())
                .build();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
