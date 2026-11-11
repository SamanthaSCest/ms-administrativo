package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.EspecialidadeResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.ProcedimentoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.ProcedimentoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.entities.Insumo;
import br.edu.imepac.clinicalapi.administrativo.entities.InsumoProcedimento;
import br.edu.imepac.clinicalapi.administrativo.entities.Procedimento;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.InsumoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.ProcedimentoRepository;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Procedimentos: código TUSS único, especialidade e insumos existentes (422), insumo sem repetição (422).
 */
@Service
@RequiredArgsConstructor
public class ProcedimentoService {

    private static final Logger log = LoggerFactory.getLogger(ProcedimentoService.class);

    private final ProcedimentoRepository repository;
    private final EspecialidadeRepository especialidadeRepository;
    private final InsumoRepository insumoRepository;
    private final InsumoService insumoService;
    private final ModelMapper modelMapper;

    @Transactional
    public ProcedimentoResponse criar(ProcedimentoRequest request) {
        log.info("Criando procedimento TUSS {}", request.getCodigoTuss());
        if (repository.existsByCodigoTussIgnoreCase(request.getCodigoTuss())) {
            throw new ConflictException("Já existe procedimento com o código TUSS: " + request.getCodigoTuss());
        }
        Procedimento procedimento = new Procedimento();
        preencher(procedimento, request);
        return toResponse(repository.save(procedimento));
    }

    @Transactional(readOnly = true)
    public Page<ProcedimentoResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProcedimentoResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional
    public ProcedimentoResponse atualizar(Long id, ProcedimentoRequest request) {
        log.info("Atualizando procedimento {}", id);
        Procedimento procedimento = buscarEntidade(id);
        if (repository.existsByCodigoTussIgnoreCaseAndIdNot(request.getCodigoTuss(), id)) {
            throw new ConflictException("Já existe procedimento com o código TUSS: " + request.getCodigoTuss());
        }
        preencher(procedimento, request);
        return toResponse(repository.save(procedimento));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo procedimento {}", id);
        // Os insumos do procedimento são removidos em cascata (composição)
        repository.delete(buscarEntidade(id));
    }

    /** Para outros services (custo médico): procedimento informado no corpo que não existe -> 422. */
    @Transactional(readOnly = true)
    public Procedimento buscarReferencia(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessRuleException("Procedimento não encontrado: " + id));
    }

    private Procedimento buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Procedimento", id));
    }

    private void preencher(Procedimento procedimento, ProcedimentoRequest request) {
        Especialidade especialidade = especialidadeRepository.findById(request.getEspecialidadeId())
                .orElseThrow(() -> new BusinessRuleException("Especialidade não encontrada: " + request.getEspecialidadeId()));
        procedimento.setEspecialidade(especialidade);
        procedimento.setCodigoTuss(request.getCodigoTuss());
        procedimento.setNome(request.getNome());
        procedimento.setDescricao(request.getDescricao());
        procedimento.setDuracaoMinutos(request.getDuracaoMinutos());
        procedimento.setValorBase(request.getValorBase());
        sincronizarInsumos(procedimento, request.getInsumos() == null ? List.of() : request.getInsumos());
    }

    /**
     * Atualiza a lista de insumos sem apagar e reinserir tudo: muda a quantidade dos que continuam,
     * remove os que saíram e adiciona os novos (evita violar a chave única procedimento+insumo).
     */
    private void sincronizarInsumos(Procedimento procedimento, List<ProcedimentoRequest.ItemInsumo> itens) {
        Map<Long, ProcedimentoRequest.ItemInsumo> pedidos = new HashMap<>();
        for (ProcedimentoRequest.ItemInsumo item : itens) {
            if (pedidos.put(item.insumoId(), item) != null) {
                throw new BusinessRuleException("Insumo repetido no procedimento: " + item.insumoId());
            }
        }
        procedimento.getInsumos().removeIf(atual -> !pedidos.containsKey(atual.getInsumo().getId()));
        Set<Long> jaExistentes = new HashSet<>();
        for (InsumoProcedimento atual : procedimento.getInsumos()) {
            atual.setQuantidade(pedidos.get(atual.getInsumo().getId()).quantidade());
            jaExistentes.add(atual.getInsumo().getId());
        }
        for (ProcedimentoRequest.ItemInsumo item : itens) {
            if (!jaExistentes.contains(item.insumoId())) {
                Insumo insumo = insumoRepository.findById(item.insumoId())
                        .orElseThrow(() -> new BusinessRuleException("Insumo não encontrado: " + item.insumoId()));
                procedimento.adicionarInsumo(insumo, item.quantidade());
            }
        }
    }

    private ProcedimentoResponse toResponse(Procedimento p) {
        List<ProcedimentoResponse.ItemInsumo> itens = p.getInsumos().stream()
                .map(i -> new ProcedimentoResponse.ItemInsumo(insumoService.toResponse(i.getInsumo()),
                        i.getQuantidade(), i.calcularCusto()))
                .toList();
        return new ProcedimentoResponse(p.getId(), modelMapper.map(p.getEspecialidade(), EspecialidadeResponse.class),
                p.getCodigoTuss(), p.getNome(), p.getDescricao(), p.getDuracaoMinutos(), p.getValorBase(),
                itens, p.calcularCustoInsumos(), p.getDataCriacao(), p.getDataModificacao());
    }
}
