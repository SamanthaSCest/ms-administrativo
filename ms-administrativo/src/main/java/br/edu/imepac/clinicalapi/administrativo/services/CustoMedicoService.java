package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.CustoMedicoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.CustoMedicoResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.Referencia;
import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedico;
import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedicoConsulta;
import br.edu.imepac.clinicalapi.administrativo.entities.CustoMedicoProcedimento;
import br.edu.imepac.clinicalapi.administrativo.entities.Especialidade;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.entities.Procedimento;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoCustoMedico;
import br.edu.imepac.clinicalapi.administrativo.repositories.CustoMedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Custos do médico (consulta por especialidade ou por procedimento), com vigência.
 * Regras: o médico precisa atender a especialidade (do procedimento, no caso de PROCEDIMENTO) -> 422;
 * fim da vigência não pode ser antes do início -> 422; não pode haver duas vigências sobrepostas para o
 * mesmo médico e item -> 409.
 */
@Service
@RequiredArgsConstructor
public class CustoMedicoService {

    private static final Logger log = LoggerFactory.getLogger(CustoMedicoService.class);

    private final CustoMedicoRepository repository;
    private final MedicoService medicoService;
    private final EspecialidadeRepository especialidadeRepository;
    private final ProcedimentoService procedimentoService;
    private final EventoService eventos;

    @Transactional
    public CustoMedicoResponse criar(CustoMedicoRequest request) {
        log.info("Criando custo {} do médico {}", request.tipo(), request.medicoId());
        CustoMedico custo = request.tipo() == TipoCustoMedico.CONSULTA
                ? new CustoMedicoConsulta() : new CustoMedicoProcedimento();
        preencher(custo, request);
        var salvo = repository.save(custo);
        eventos.criado(salvo);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public Page<CustoMedicoResponse> listar(Long medicoId, Pageable pageable) {
        Page<CustoMedico> pagina = medicoId == null ? repository.findAll(pageable)
                : repository.findByMedicoId(medicoId, pageable);
        return pagina.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustoMedicoResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    /** Custo vigente do médico numa data para a especialidade (consulta) ou para o procedimento. */
    @Transactional(readOnly = true)
    public CustoMedicoResponse buscarVigente(Long medicoId, Long especialidadeId, Long procedimentoId, LocalDate data) {
        if ((especialidadeId == null) == (procedimentoId == null)) {
            throw new BusinessRuleException("Informe especialidadeId (consulta) ou procedimentoId, apenas um deles");
        }
        return repository.findAllByMedicoId(medicoId).stream()
                .filter(CustoMedico::isAtivo)
                .filter(c -> c.estaVigente(data))
                .filter(c -> especialidadeId != null
                        ? c instanceof CustoMedicoConsulta cc && cc.getEspecialidade().getId().equals(especialidadeId)
                        : c instanceof CustoMedicoProcedimento cp && cp.getProcedimento().getId().equals(procedimentoId))
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum custo vigente encontrado para o médico " + medicoId
                        + " em " + data));
    }

    @Transactional
    public CustoMedicoResponse atualizar(Long id, CustoMedicoRequest request) {
        log.info("Atualizando custo {}", id);
        CustoMedico custo = buscarEntidade(id);
        if (custo.getTipo() != request.tipo()) {
            throw new BusinessRuleException("O tipo do custo não pode ser alterado; remova e cadastre outro");
        }
        var antes = eventos.retrato(toResponse(custo));
        preencher(custo, request);
        var depois = toResponse(repository.save(custo));
        eventos.atualizado(custo, antes, depois);
        return depois;
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo custo {}", id);
        var entidade = buscarEntidade(id);
        eventos.removido(entidade);
        repository.delete(entidade);
    }

    private CustoMedico buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Custo do médico", id));
    }

    private void preencher(CustoMedico custo, CustoMedicoRequest request) {
        if (request.vigenciaFim() != null && request.vigenciaFim().isBefore(request.vigenciaInicio())) {
            throw new BusinessRuleException("O fim da vigência não pode ser anterior ao início");
        }
        Medico medico = medicoService.buscarReferencia(request.medicoId());
        custo.setMedico(medico);
        custo.setValor(request.valor());
        custo.setVigenciaInicio(request.vigenciaInicio());
        custo.setVigenciaFim(request.vigenciaFim());

        if (custo instanceof CustoMedicoConsulta consulta) {
            if (request.especialidadeId() == null) {
                throw new BusinessRuleException("especialidadeId é obrigatório para custo de CONSULTA");
            }
            Especialidade especialidade = especialidadeRepository.findById(request.especialidadeId())
                    .orElseThrow(() -> new BusinessRuleException("Especialidade não encontrada: " + request.especialidadeId()));
            if (!medico.atende(especialidade)) {
                throw new BusinessRuleException("O médico não atende a especialidade " + especialidade.getNome());
            }
            if (!especialidade.isAtivo()) {
                throw new BusinessRuleException("Especialidade inativa: " + especialidade.getNome());
            }
            consulta.setEspecialidade(especialidade);
        } else if (custo instanceof CustoMedicoProcedimento porProcedimento) {
            if (request.procedimentoId() == null) {
                throw new BusinessRuleException("procedimentoId é obrigatório para custo de PROCEDIMENTO");
            }
            Procedimento procedimento = procedimentoService.buscarReferencia(request.procedimentoId());
            if (!medico.atende(procedimento.getEspecialidade())) {
                throw new BusinessRuleException("O médico não atende a especialidade do procedimento "
                        + procedimento.getCodigoTuss());
            }
            porProcedimento.setProcedimento(procedimento);
        }

        boolean sobreposto = repository.findAllByMedicoId(medico.getId()).stream()
                .filter(CustoMedico::isAtivo)
                .anyMatch(outro -> !Objects.equals(outro.getId(), custo.getId())
                        && outro.mesmoItem(custo)
                        && outro.sobrepoe(custo.getVigenciaInicio(), custo.getVigenciaFim()));
        if (sobreposto) {
            throw new ConflictException("Já existe custo com vigência sobreposta para este médico e item");
        }
    }

    private CustoMedicoResponse toResponse(CustoMedico c) {
        Referencia especialidade = null;
        Referencia procedimento = null;
        if (c instanceof CustoMedicoConsulta cc) {
            especialidade = new Referencia(cc.getEspecialidade().getId(), cc.getEspecialidade().getNome());
        } else if (c instanceof CustoMedicoProcedimento cp) {
            procedimento = new Referencia(cp.getProcedimento().getId(),
                    cp.getProcedimento().getCodigoTuss() + " - " + cp.getProcedimento().getNome());
        }
        return new CustoMedicoResponse(c.getId(), c.getTipo(),
                new Referencia(c.getMedico().getId(), c.getMedico().getNome()), especialidade, procedimento,
                c.getValor(), c.getVigenciaInicio(), c.getVigenciaFim(), c.estaVigente(LocalDate.now()),
                c.getUuid(), c.isAtivo(), c.getCriadoPor(), c.getAtualizadoPor(),
                c.getDataCriacao(), c.getDataModificacao());
    }
}
