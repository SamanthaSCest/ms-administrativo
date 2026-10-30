package br.com.clinica.ms_administrativo.comum.erro;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String BASE_TYPE = "https://clinica.local/erros/";

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ProblemDetail> negocio(NegocioException ex, HttpServletRequest req) {
        ProblemDetail pd = montar(ex.getStatus(), ex.getSlug(), ex.getTitulo(),
                                  ex.getMessage(), req);
        pd.setProperty("codigo", ex.getCodigo());
        return ResponseEntity.status(ex.getStatus()).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException ex,
                                                   HttpServletRequest req) {
        List<Map<String, String>> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("campo", e.getField(),
                                 "mensagem", String.valueOf(e.getDefaultMessage())))
                .toList();
        ProblemDetail pd = montar(HttpStatus.BAD_REQUEST, "validacao",
                "Dados inválidos", "Um ou mais campos são inválidos.", req);
        pd.setProperty("codigo", "ADM-GEN-400");
        pd.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> corpoInvalido(HttpMessageNotReadableException ex,
                                                       HttpServletRequest req) {
        ProblemDetail pd = montar(HttpStatus.BAD_REQUEST, "corpo-invalido",
                "Corpo da requisição inválido", "O JSON enviado está malformado.", req);
        pd.setProperty("codigo", "ADM-GEN-400");
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> inesperado(Exception ex, HttpServletRequest req) {
        ProblemDetail pd = montar(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno",
                "Erro interno", "Ocorreu um erro inesperado.", req);
        pd.setProperty("codigo", "ADM-GEN-500");
        return ResponseEntity.internalServerError().body(pd);
    }

    private ProblemDetail montar(HttpStatus status, String slug, String titulo,
                                 String detalhe, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setType(URI.create(BASE_TYPE + slug));
        pd.setTitle(titulo);
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }
}