package br.edu.imepac.clinicalapi.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.common.dto.ErrorResponse;
import br.edu.imepac.clinicalapi.common.dto.ErrorResponse.FieldDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Implementação base do tratamento de exceções (Exception Handling no Spring MVC).
 * <p>
 * Não é um advice por si só: cada módulo cria o seu {@code GlobalExceptionHandler}
 * ({@code @RestControllerAdvice}) estendendo esta classe, no pacote {@code exceptions}.
 * Os métodos {@code @ExceptionHandler} são herdados, então o formato de erro é igual em todos os serviços.
 */
public abstract class BaseExceptionHandler {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(BaseExceptionHandler.class);

    // Cada @ExceptionHandler captura um tipo de exceção lançada em QUALQUER controller/service
    // e converte no JSON padrão (ErrorResponse). Assim ninguém precisa de try/catch nos controllers.
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Erro de integração em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("{} em {}: {}", ex.getStatus(), request.getRequestURI(), ex.getMessage());
        }
        return build(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        log.warn("Validação falhou em {}: {}", request.getRequestURI(), details);
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new FieldDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Corpo ilegível em {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", request, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName(), request,
                List.of(new FieldDetail(ex.getName(), "valor inválido: " + ex.getValue())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente: " + ex.getParameterName(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Violação de integridade dos dados", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado: " + request.getRequestURI(), request, List.of());
    }

    // Último recurso: qualquer exceção não prevista vira 500 sem vazar detalhes internos ao cliente
    // (o stack trace completo vai só para o log).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<FieldDetail> details) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
