package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.SalaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.SalaResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Sala;
import br.edu.imepac.clinicalapi.administrativo.repositories.DisponibilidadeMedicaRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Salas da clínica: identificação única. */
@Service
@RequiredArgsConstructor
public class SalaService {

    private static final Logger log = LoggerFactory.getLogger(SalaService.class);

    private final SalaRepository repository;
    private final DisponibilidadeMedicaRepository disponibilidadeRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public SalaResponse criar(SalaRequest request) {
        log.info("Criando sala '{}'", request.getIdentificacao());
        if (repository.existsByIdentificacaoIgnoreCase(request.getIdentificacao())) {
            throw new ConflictException("Já existe sala com a identificação: " + request.getIdentificacao());
        }
        return toResponse(repository.save(modelMapper.map(request, Sala.class)));
    }

    @Transactional(readOnly = true)
    public Page<SalaResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SalaResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public SalaResponse atualizar(Long id, SalaRequest request) {
        log.info("Atualizando sala {}", id);
        Sala sala = buscarEntidade(id);
        if (repository.existsByIdentificacaoIgnoreCaseAndIdNot(request.getIdentificacao(), id)) {
            throw new ConflictException("Já existe sala com a identificação: " + request.getIdentificacao());
        }
        modelMapper.map(request, sala);
        return toResponse(repository.save(sala));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo sala {}", id);
        Sala sala = buscarEntidade(id);
        if (disponibilidadeRepository.existsBySalaId(id)) {
            throw new ConflictException("Sala possui disponibilidades médicas e não pode ser removida");
        }
        repository.delete(sala);
    }

    /** Para outros services: sala do corpo inexistente ou inativa -> 422. */
    @Transactional(readOnly = true)
    public Sala buscarReferencia(Long id) {
        Sala sala = repository.findById(id).orElseThrow(() -> new BusinessRuleException("Sala não encontrada: " + id));
        if (!sala.isAtivo()) {
            throw new BusinessRuleException("Sala inativa: " + sala.getIdentificacao());
        }
        return sala;
    }

    private Sala buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Sala", id));
    }

    private SalaResponse toResponse(Sala sala) {
        return modelMapper.map(sala, SalaResponse.class);
    }
}
