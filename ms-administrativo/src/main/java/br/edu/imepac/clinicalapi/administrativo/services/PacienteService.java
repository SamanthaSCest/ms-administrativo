package br.edu.imepac.clinicalapi.administrativo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.entities.ResponsavelLegal;
import br.edu.imepac.clinicalapi.administrativo.mapper.PacienteMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de Paciente: unicidade de CPF e responsável legal obrigatório para menores de idade.
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
    private final ResponsavelLegalRepository responsavelRepository;

    @Transactional
    public PacienteResponse criar(PacienteRequest request) {
        log.info("Criando paciente");
        // CPF único -> 409 Conflict
        if (repository.existsByCpf(request.getCpf())) {
            log.warn("CPF de paciente já cadastrado");
            throw new ConflictException("Já existe paciente com o CPF informado");
        }
        Paciente paciente = mapper.toEntity(request);
        definirResponsavel(paciente, request.getResponsavelId());
        return mapper.toResponse(repository.save(paciente));
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
        definirResponsavel(paciente, request.getResponsavelId());
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

    /** Resolve o responsável pelo id (422 se não existir) e exige responsável para menores de idade (422). */
    private void definirResponsavel(Paciente paciente, Long responsavelId) {
        ResponsavelLegal responsavel = null;
        if (responsavelId != null) {
            responsavel = responsavelRepository.findById(responsavelId)
                    .orElseThrow(() -> new BusinessRuleException("Responsável legal não encontrado: " + responsavelId));
        }
        if (responsavel == null && paciente.ehMenorDeIdade()) {
            throw new BusinessRuleException("Paciente menor de idade precisa de um responsável legal");
        }
        paciente.setResponsavel(responsavel);
    }
}
