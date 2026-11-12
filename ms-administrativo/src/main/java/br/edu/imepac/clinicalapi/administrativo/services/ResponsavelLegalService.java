package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.ResponsavelLegalRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.ResponsavelLegalResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.ResponsavelLegal;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ResponsavelLegalRepository;
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

/** Responsáveis legais: CPF único; não remove responsável com pacientes vinculados. */
@Service
@RequiredArgsConstructor
public class ResponsavelLegalService {

    private static final Logger log = LoggerFactory.getLogger(ResponsavelLegalService.class);

    private final ResponsavelLegalRepository repository;
    private final PacienteRepository pacienteRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ResponsavelLegalResponse criar(ResponsavelLegalRequest request) {
        log.info("Criando responsável legal");
        if (repository.existsByCpf(request.getCpf())) {
            throw new ConflictException("Já existe responsável legal com o CPF informado");
        }
        return toResponse(repository.save(modelMapper.map(request, ResponsavelLegal.class)));
    }

    @Transactional(readOnly = true)
    public Page<ResponsavelLegalResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ResponsavelLegalResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public ResponsavelLegalResponse atualizar(Long id, ResponsavelLegalRequest request) {
        log.info("Atualizando responsável legal {}", id);
        ResponsavelLegal responsavel = buscarEntidade(id);
        if (repository.existsByCpfAndIdNot(request.getCpf(), id)) {
            throw new ConflictException("Já existe responsável legal com o CPF informado");
        }
        modelMapper.map(request, responsavel);
        return toResponse(repository.save(responsavel));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo responsável legal {}", id);
        ResponsavelLegal responsavel = buscarEntidade(id);
        if (pacienteRepository.existsByResponsavelId(id)) {
            throw new ConflictException("Responsável legal possui pacientes vinculados e não pode ser removido");
        }
        repository.delete(responsavel);
    }

    private ResponsavelLegal buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Responsável legal", id));
    }

    private ResponsavelLegalResponse toResponse(ResponsavelLegal responsavel) {
        return modelMapper.map(responsavel, ResponsavelLegalResponse.class);
    }
}
