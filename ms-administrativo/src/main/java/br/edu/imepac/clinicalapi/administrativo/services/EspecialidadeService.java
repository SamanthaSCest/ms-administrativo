package br.edu.imepac.clinicalapi.administrativo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.mapper.EspecialidadeMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de Especialidade.
 * A camada de serviço é onde ficam validações de negócio (nome único, vínculo com médicos),
 * transações e logs. O controller só delega para cá e o repository só acessa o banco.
 */
@Service
@RequiredArgsConstructor
public class EspecialidadeService {

    // Logger do SLF4J (API de logs) - o Spring Boot usa o Logback como implementação.
    // Níveis: debug (detalhe), info (fluxo normal), warn (erro do cliente), error (falha nossa).
    // Use sempre placeholders {} em vez de concatenar strings.
    private static final Logger log = LoggerFactory.getLogger(EspecialidadeService.class);

    private final EspecialidadeRepository repository;
    private final MedicoRepository medicoRepository;
    private final EspecialidadeMapper mapper;

    // @Transactional: tudo no método acontece numa única transação (se lançar exceção, faz rollback).
    @Transactional
    public EspecialidadeResponse criar(EspecialidadeRequest request) {
        log.info("Criando especialidade '{}'", request.getNome());
        // Regra de negócio: nome único (ignorando maiúsculas/minúsculas) -> 409 Conflict
        if (repository.existsByNomeIgnoreCase(request.getNome())) {
            log.warn("Especialidade duplicada: {}", request.getNome());
            throw new ConflictException("Já existe especialidade com o nome: " + request.getNome());
        }
        // DTO -> entidade -> banco -> DTO de resposta
        Especialidade salva = repository.save(mapper.toEntity(request));
        log.info("Especialidade criada com id {}", salva.getId());
        return mapper.toResponse(salva);
    }

    // readOnly = true: otimização para consultas (não grava nada)
    @Transactional(readOnly = true)
    public Page<EspecialidadeResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EspecialidadeResponse buscar(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional
    public EspecialidadeResponse atualizar(Long id, EspecialidadeRequest request) {
        log.info("Atualizando especialidade {}", id);
        Especialidade entidade = buscarEntidade(id);
        // AndIdNot: ignora o próprio registro, senão não poderia salvar sem mudar o nome
        if (repository.existsByNomeIgnoreCaseAndIdNot(request.getNome(), id)) {
            throw new ConflictException("Já existe especialidade com o nome: " + request.getNome());
        }
        mapper.updateEntity(request, entidade);
        return mapper.toResponse(repository.save(entidade));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo especialidade {}", id);
        Especialidade entidade = buscarEntidade(id);
        // Integridade: não remover especialidade que ainda tem médicos
        if (medicoRepository.existsByEspecialidadeId(id)) {
            log.warn("Especialidade {} possui médicos vinculados", id);
            throw new ConflictException("Especialidade possui médicos vinculados e não pode ser removida");
        }
        repository.delete(entidade);
    }

    /** Usado também pelo MedicoService. */
    @Transactional(readOnly = true)
    public Especialidade buscarEntidade(Long id) {
        // orElseThrow: se não existir, lança 404 (tratado no GlobalExceptionHandler)
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Especialidade", id));
    }
}
