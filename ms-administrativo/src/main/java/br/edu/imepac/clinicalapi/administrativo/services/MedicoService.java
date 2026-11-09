package br.edu.imepac.clinicalapi.administrativo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.MedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.mapper.MedicoMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de Médico: unicidade de CPF/CRM e existência da especialidade.
 */
@Service
@RequiredArgsConstructor
public class MedicoService {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(MedicoService.class);

    private final MedicoRepository repository;
    private final EspecialidadeRepository especialidadeRepository;
    private final MedicoMapper mapper;

    @Transactional
    public MedicoResponse criar(MedicoRequest request) {
        log.info("Criando médico CRM {}", request.getCrm());
        // Regras de unicidade -> 409 Conflict
        if (repository.existsByCpf(request.getCpf())) {
            log.warn("CPF de médico já cadastrado");
            throw new ConflictException("Já existe médico com o CPF informado");
        }
        if (repository.existsByCrm(request.getCrm())) {
            throw new ConflictException("Já existe médico com o CRM: " + request.getCrm());
        }
        Medico medico = mapper.toEntity(request);
        // O mapper não preenche a especialidade: buscamos pelo id informado (422 se não existir)
        medico.setEspecialidade(buscarEspecialidade(request.getEspecialidadeId()));
        log.debug("Salvando médico {}", medico.getNome());
        return mapper.toResponse(repository.save(medico));
    }

    @Transactional(readOnly = true)
    public Page<MedicoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscar(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public MedicoResponse atualizar(Long id, MedicoRequest request) {
        log.info("Atualizando médico {}", id);
        Medico medico = buscarEntidade(id);
        if (repository.existsByCpfAndIdNot(request.getCpf(), id)) {
            throw new ConflictException("Já existe médico com o CPF informado");
        }
        if (repository.existsByCrmAndIdNot(request.getCrm(), id)) {
            throw new ConflictException("Já existe médico com o CRM: " + request.getCrm());
        }
        // Copia os campos do request para a entidade já existente (mantém o id)
        mapper.updateEntity(request, medico);
        medico.setEspecialidade(buscarEspecialidade(request.getEspecialidadeId()));
        return mapper.toResponse(repository.save(medico));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo médico {}", id);
        repository.delete(buscarEntidade(id));
    }

    private Medico buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Médico", id));
    }

    private Especialidade buscarEspecialidade(Long id) {
        return especialidadeRepository.findById(id)
                // 422 (e não 404): o recurso da URL existe, mas um dado do corpo referencia algo inválido
                .orElseThrow(() -> new BusinessRuleException("Especialidade não encontrada: " + id));
    }
}
