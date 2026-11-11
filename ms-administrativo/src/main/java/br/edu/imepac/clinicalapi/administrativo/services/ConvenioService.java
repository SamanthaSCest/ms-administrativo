package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.ConvenioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.ConvenioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Convenio;
import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
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

/** Convênios: registro ANS e CNPJ únicos. */
@Service
@RequiredArgsConstructor
public class ConvenioService {

    private static final Logger log = LoggerFactory.getLogger(ConvenioService.class);

    private final ConvenioRepository repository;
    private final ModelMapper modelMapper;

    @Transactional
    public ConvenioResponse criar(ConvenioRequest request) {
        log.info("Criando convênio ANS {}", request.getRegistroAns());
        if (repository.existsByRegistroAns(request.getRegistroAns())) {
            throw new ConflictException("Já existe convênio com o registro ANS: " + request.getRegistroAns());
        }
        if (repository.existsByCnpj(request.getCnpj())) {
            throw new ConflictException("Já existe convênio com o CNPJ informado");
        }
        return toResponse(repository.save(modelMapper.map(request, Convenio.class)));
    }

    @Transactional(readOnly = true)
    public Page<ConvenioResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConvenioResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public ConvenioResponse atualizar(Long id, ConvenioRequest request) {
        log.info("Atualizando convênio {}", id);
        Convenio convenio = buscarEntidade(id);
        if (repository.existsByRegistroAnsAndIdNot(request.getRegistroAns(), id)) {
            throw new ConflictException("Já existe convênio com o registro ANS: " + request.getRegistroAns());
        }
        if (repository.existsByCnpjAndIdNot(request.getCnpj(), id)) {
            throw new ConflictException("Já existe convênio com o CNPJ informado");
        }
        modelMapper.map(request, convenio);
        return toResponse(repository.save(convenio));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo convênio {}", id);
        repository.delete(buscarEntidade(id));
    }

    private Convenio buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Convênio", id));
    }

    private ConvenioResponse toResponse(Convenio convenio) {
        return modelMapper.map(convenio, ConvenioResponse.class);
    }
}
