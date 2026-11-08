package br.edu.imepac.clinicalapi.agendamento.exceptions;

import br.edu.imepac.clinicalapi.common.exception.BaseExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento CENTRAL de exceções do ms-agendamento (Exception Handling no Spring MVC).
 * <p>
 * {@code @RestControllerAdvice} faz esta classe valer para TODOS os controllers: qualquer exceção
 * lançada em controller/service/repository é capturada aqui e convertida no JSON padrão ({@code ErrorResponse}).
 * Por isso controllers e services não usam try/catch: apenas lançam exceções
 * ({@code ResourceNotFoundException} 404, {@code ConflictException} 409, {@code BusinessRuleException} 422,
 * {@code ExternalServiceException} 503...).
 * <p>
 * Os handlers genéricos (validação 400, JSON inválido, integridade de dados, erro 500...) vêm de
 * {@link BaseExceptionHandler}. Para um tratamento específico deste módulo, adicione aqui um método:
 * <pre>
 * &#64;ExceptionHandler(MinhaExcecao.class)
 * public ResponseEntity&lt;ErrorResponse&gt; handle(MinhaExcecao ex, HttpServletRequest request) { ... }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseExceptionHandler {
}
