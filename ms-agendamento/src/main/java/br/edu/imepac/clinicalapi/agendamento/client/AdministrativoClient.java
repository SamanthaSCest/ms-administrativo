package br.edu.imepac.clinicalapi.agendamento.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.agendamento.client.dto.MedicoResumo;
import br.edu.imepac.clinicalapi.agendamento.client.dto.PacienteResumo;
import br.edu.imepac.clinicalapi.common.exception.ExternalServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Comunicação síncrona (REST) com o ms-administrativo.
 * 404 vira {@code Optional.empty()}; qualquer outra falha vira {@link ExternalServiceException} (HTTP 503).
 */
@Component
public class AdministrativoClient {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(AdministrativoClient.class);

    private final RestClient restClient;

    public AdministrativoClient(RestClient.Builder builder,
                                @Value("${clients.administrativo.url}") String baseUrl) {
        // RestClient: cliente HTTP do Spring 6. A URL base vem do application.yml (clients.administrativo.url)
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public Optional<MedicoResumo> buscarMedico(Long id) {
        return buscar("/medicos/{id}", id, MedicoResumo.class);
    }

    public Optional<PacienteResumo> buscarPaciente(Long id) {
        return buscar("/pacientes/{id}", id, PacienteResumo.class);
    }

    private <T> Optional<T> buscar(String uri, Long id, Class<T> tipo) {
        log.debug("GET ms-administrativo {} (id={})", uri, id);
        try {
            return Optional.ofNullable(restClient.get().uri(uri, id).retrieve().body(tipo));
        } catch (HttpClientErrorException.NotFound e) {
            // 404 não é falha de infraestrutura: apenas "não existe"
            log.info("Recurso {} (id={}) não encontrado no ms-administrativo", uri, id);
            return Optional.empty();
        } catch (RestClientException e) {
            // timeout, conexão recusada, 5xx... => o serviço dependente está indisponível
            log.error("Falha ao chamar ms-administrativo {} (id={}): {}", uri, id, e.getMessage());
            throw new ExternalServiceException("ms-administrativo indisponível ou com erro", e);
        }
    }
}
