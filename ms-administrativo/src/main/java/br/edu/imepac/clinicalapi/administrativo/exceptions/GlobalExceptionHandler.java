package br.edu.imepac.clinicalapi.administrativo.exceptions;

import br.edu.imepac.clinicalapi.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Erros de negócio: NegocioException e as exceções do common (404, 409, 422...)
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        HttpStatus status = ex.getStatus();
        String codigo = (ex instanceof NegocioException n) ? n.getCodigo() : "ADM-GEN-" + status.value();

        log.warn("{}: {}", codigo, ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("codigo", codigo);
        return ResponseEntity.status(status).body(pd);
    }

    // Bean Validation (@Valid) -> 400 com a lista de campos inválidos
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.");
        pd.setTitle("Dados inválidos");
        pd.setProperty("codigo", "ADM-GEN-400");
        pd.setProperty("campos", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(),
                                 "mensagem", String.valueOf(e.getDefaultMessage())))
                .toList());

        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    // Violação de unique/FK no banco -> 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("ADM-GEN-409: {}", ex.getMostSpecificCause().getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Violação de integridade dos dados.");
        pd.setTitle("Conflito");
        pd.setProperty("codigo", "ADM-GEN-409");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    // Último recurso: qualquer erro não previsto -> 500, sem vazar detalhe interno
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleInesperado(Exception ex) {
        log.error("ADM-GEN-500: erro inesperado", ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado.");
        pd.setTitle("Erro interno");
        pd.setProperty("codigo", "ADM-GEN-500");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    // Todo erro padrão do Spring (404 de rota, 405, JSON ilegível...) passa por aqui:
    // garante que a resposta sempre tenha o campo "codigo"
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        ResponseEntity<Object> resposta =
                super.handleExceptionInternal(ex, body, headers, statusCode, request);

        if (resposta != null && resposta.getBody() instanceof ProblemDetail pd) {
            if (pd.getProperties() == null || !pd.getProperties().containsKey("codigo")) {
                pd.setProperty("codigo", "ADM-GEN-" + pd.getStatus());
            }
        }
        return resposta;
    }
}