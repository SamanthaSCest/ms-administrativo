package br.edu.imepac.clinicalapi.administrativo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.mapper.PacienteMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de Paciente: unicidade de CPF.
 */
@Service
@RequiredArgsConstructor
public class PacienteService {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(PacienteService.class);

    private final PacienteRepository repository;
    private final PacienteMapper mapper;

    @Transactional
    public PacienteResponse criar(PacienteRequest request) {
        log.info("Criando paciente");
        // CPF único -> 409 Conflict
        if (repository.existsByCpf(request.getCpf())) {
            log.warn("CPF de paciente já cadastrado");
            throw new ConflictException("Já existe paciente com o CPF informado");
        }
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscar(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public PacienteResponse atualizar(Long id, PacienteRequest request) {
        log.info("Atualizando paciente {}", id);
        Paciente paciente = buscarEntidade(id);
        if (repository.existsByCpfAndIdNot(request.getCpf(), id)) {
            throw new ConflictException("Já existe paciente com o CPF informado");
        }
        mapper.updateEntity(request, paciente);
        return mapper.toResponse(repository.save(paciente));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo paciente {}", id);
        repository.delete(buscarEntidade(id));
    }

    private Paciente buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Paciente", id));
    }
}
