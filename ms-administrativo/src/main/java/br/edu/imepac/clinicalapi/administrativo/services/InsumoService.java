package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.InsumoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.InsumoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Insumo;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
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

import java.math.BigDecimal;

/**
 * Insumos: código único; não remove insumo usado em procedimento.
 * Também usado pelo ms-estoque (X-Api-Key), que pode cadastrar, alterar e atualizar o custo.
 */
@Service
@RequiredArgsConstructor
public class InsumoService {

    private static final Logger log = LoggerFactory.getLogger(InsumoService.class);

    private final InsumoRepository repository;
    private final ProcedimentoRepository procedimentoRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public InsumoResponse criar(InsumoRequest request) {
        log.info("Criando insumo {}", request.getCodigo());
        if (repository.existsByCodigoIgnoreCase(request.getCodigo())) {
            throw new ConflictException("Já existe insumo com o código: " + request.getCodigo());
        }
        return toResponse(repository.save(modelMapper.map(request, Insumo.class)));
    }

    @Transactional(readOnly = true)
    public Page<InsumoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InsumoResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public InsumoResponse atualizar(Long id, InsumoRequest request) {
        log.info("Atualizando insumo {}", id);
        Insumo insumo = buscarEntidade(id);
        if (repository.existsByCodigoIgnoreCaseAndIdNot(request.getCodigo(), id)) {
            throw new ConflictException("Já existe insumo com o código: " + request.getCodigo());
        }
        modelMapper.map(request, insumo);
        return toResponse(repository.save(insumo));
    }

    @Transactional
    public InsumoResponse atualizarCusto(Long id, BigDecimal custoUnitario) {
        log.info("Atualizando custo do insumo {}", id);
        Insumo insumo = buscarEntidade(id);
        insumo.setCustoUnitario(custoUnitario);
        return toResponse(repository.save(insumo));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo insumo {}", id);
        Insumo insumo = buscarEntidade(id);
        if (procedimentoRepository.existsByInsumosInsumoId(id)) {
            throw new ConflictException("Insumo é usado em procedimentos e não pode ser removido");
        }
        repository.delete(insumo);
    }

    private Insumo buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Insumo", id));
    }

    InsumoResponse toResponse(Insumo insumo) {
        return modelMapper.map(insumo, InsumoResponse.class);
    }
}
