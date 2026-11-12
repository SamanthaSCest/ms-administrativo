package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.AtendeResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.ConvenioResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.EspecialidadeResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.MedicoResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SalaResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Medico;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoSala;
import br.edu.imepac.clinicalapi.administrativo.mapper.ResumoMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.EspecialidadeRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.MedicoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.SalaRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * API de integração (somente leitura, RN18) de especialidades, médicos, salas e convênios para os outros
 * microserviços. Devolve os mesmos Resumos usados nos eventos. Pacientes: PacienteIntegracaoService.
 */
@Service
@RequiredArgsConstructor
public class IntegracaoService {

    private final EspecialidadeRepository especialidadeRepository;
    private final MedicoRepository medicoRepository;
    private final SalaRepository salaRepository;
    private final ConvenioRepository convenioRepository;
    private final ResumoMapper resumoMapper;

    // ---------- Especialidades ----------

    /** Especialidades ativas, por nome (RF20). */
    @Transactional(readOnly = true)
    public List<EspecialidadeResumo> especialidadesAtivas() {
        return especialidadeRepository.findByAtivoTrueOrderByNomeAsc().stream().map(resumoMapper::especialidade).toList();
    }

    // ---------- Médicos ----------

    @Transactional(readOnly = true)
    public MedicoResumo medico(Long id) {
        return resumoMapper.medico(buscarMedico(id));
    }

    /** Sempre 200: id inexistente devolve existe = false (RF21). */
    @Transactional(readOnly = true)
    public SituacaoResponse situacaoMedico(Long id) {
        return medicoRepository.findById(id)
                .map(m -> new SituacaoResponse(true, m.isAtivo()))
                .orElse(new SituacaoResponse(false, false));
    }

    /**
     * Busca em lote e filtro (RF22): por ids (até 100, qualquer status) e/ou médicos ativos de uma especialidade.
     * Com os dois, devolve a interseção.
     */
    @Transactional(readOnly = true)
    public List<MedicoResumo> medicos(List<Long> ids, Long especialidadeId) {
        if ((ids == null || ids.isEmpty()) && especialidadeId == null) {
            throw new BusinessRuleException("Informe ids e/ou especialidadeId");
        }
        Stream<Medico> medicos;
        if (ids != null && !ids.isEmpty()) {
            IntegracaoLimites.validarLote(ids);
            medicos = medicoRepository.findAllById(ids).stream();
            if (especialidadeId != null) {
                medicos = medicos.filter(m -> m.isAtivo() && atendeId(m, especialidadeId));
            }
        } else {
            medicos = medicoRepository.findByEspecialidadesIdAndAtivoTrueOrderByNomeAsc(especialidadeId).stream();
        }
        return medicos.sorted(Comparator.comparing(Medico::getNome)).map(resumoMapper::medico).toList();
    }

    /** O médico atende a especialidade? Médico inexistente ou inativo: false (RF25). */
    @Transactional(readOnly = true)
    public AtendeResponse atende(Long medicoId, Long especialidadeId) {
        boolean atende = medicoRepository.findById(medicoId)
                .map(m -> m.isAtivo() && atendeId(m, especialidadeId))
                .orElse(false);
        return new AtendeResponse(atende);
    }

    // ---------- Salas ----------

    @Transactional(readOnly = true)
    public SalaResumo sala(Long id) {
        return resumoMapper.sala(salaRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Sala", id)));
    }

    /** Salas ativas, opcionalmente de um tipo (consultório, cirurgia, procedimento). */
    @Transactional(readOnly = true)
    public List<SalaResumo> salasAtivas(TipoSala tipo) {
        var salas = tipo == null ? salaRepository.findByAtivoTrueOrderByNumeroAsc()
                : salaRepository.findByTipoAndAtivoTrueOrderByNumeroAsc(tipo);
        return salas.stream().map(resumoMapper::sala).toList();
    }

    // ---------- Convênios (planos ficam com outro grupo) ----------

    @Transactional(readOnly = true)
    public ConvenioResumo convenio(Long id) {
        return resumoMapper.convenio(convenioRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Convênio", id)));
    }

    @Transactional(readOnly = true)
    public List<ConvenioResumo> conveniosAtivos() {
        return convenioRepository.findByAtivoTrueOrderByNomeAsc().stream().map(resumoMapper::convenio).toList();
    }

    private Medico buscarMedico(Long id) {
        return medicoRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Médico", id));
    }

    /** Atende uma especialidade ativa com esse id? */
    private static boolean atendeId(Medico medico, Long especialidadeId) {
        return medico.getEspecialidades().stream()
                .anyMatch(e -> e.isAtivo() && e.getId().equals(especialidadeId));
    }
}
