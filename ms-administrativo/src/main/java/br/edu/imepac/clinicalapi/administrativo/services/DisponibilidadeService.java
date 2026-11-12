package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.DisponibilidadeRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.DisponibilidadeResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.Referencia;
import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeDiaria;
import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeExcecao;
import br.edu.imepac.clinicalapi.administrativo.entities.DisponibilidadeMedica;
import br.edu.imepac.clinicalapi.administrativo.entities.TipoDisponibilidade;
import br.edu.imepac.clinicalapi.administrativo.repositories.DisponibilidadeMedicaRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Disponibilidade dos médicos, cadastrada pelo administrador.
 * Regras: hora final depois da inicial (422); DIARIA exige diaSemana e EXCECAO exige data e disponivel (422);
 * o mesmo médico não pode ter horários sobrepostos no mesmo dia, nem a mesma sala ser usada por dois horários
 * semanais sobrepostos (409).
 */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {

    private static final Logger log = LoggerFactory.getLogger(DisponibilidadeService.class);

    private final DisponibilidadeMedicaRepository repository;
    private final MedicoService medicoService;
    private final SalaService salaService;

    @Transactional
    public DisponibilidadeResponse criar(DisponibilidadeRequest request) {
        log.info("Criando disponibilidade {} do médico {}", request.tipo(), request.medicoId());
        DisponibilidadeMedica disponibilidade = request.tipo() == TipoDisponibilidade.DIARIA
                ? new DisponibilidadeDiaria() : new DisponibilidadeExcecao();
        preencher(disponibilidade, request);
        return toResponse(repository.save(disponibilidade));
    }

    @Transactional(readOnly = true)
    public Page<DisponibilidadeResponse> listar(Long medicoId, Pageable pageable) {
        Page<DisponibilidadeMedica> pagina = medicoId == null ? repository.findAll(pageable)
                : repository.findByMedicoId(medicoId, pageable);
        return pagina.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DisponibilidadeResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    /**
     * Horários em que o médico atende na data: os semanais daquele dia, exceto os que cruzam uma exceção
     * de bloqueio (disponivel = false), mais as exceções de horário extra (disponivel = true).
     */
    @Transactional(readOnly = true)
    public List<DisponibilidadeResponse> agenda(Long medicoId, LocalDate data) {
        // Médico inativo (ou disponibilidade inativa) não aparece na agenda
        if (!medicoService.buscarExistente(medicoId).isAtivo()) {
            return List.of();
        }
        List<DisponibilidadeMedica> doDia = repository.findAllByMedicoId(medicoId).stream()
                .filter(DisponibilidadeMedica::isAtivo)
                .filter(d -> d.aplicaEm(data))
                .toList();
        List<DisponibilidadeExcecao> bloqueios = doDia.stream()
                .filter(d -> d instanceof DisponibilidadeExcecao e && Boolean.FALSE.equals(e.getDisponivel()))
                .map(DisponibilidadeExcecao.class::cast)
                .toList();
        Stream<DisponibilidadeMedica> semanais = doDia.stream()
                .filter(DisponibilidadeDiaria.class::isInstance)
                .filter(d -> bloqueios.stream().noneMatch(d::sobrepoeHorario));
        Stream<DisponibilidadeMedica> extras = doDia.stream()
                .filter(d -> d instanceof DisponibilidadeExcecao e && Boolean.TRUE.equals(e.getDisponivel()));
        return Stream.concat(semanais, extras)
                .sorted(Comparator.comparing(DisponibilidadeMedica::getHoraInicial))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DisponibilidadeResponse atualizar(Long id, DisponibilidadeRequest request) {
        log.info("Atualizando disponibilidade {}", id);
        DisponibilidadeMedica disponibilidade = buscarEntidade(id);
        if (disponibilidade.getTipo() != request.tipo()) {
            throw new BusinessRuleException("O tipo da disponibilidade não pode ser alterado; remova e cadastre outra");
        }
        preencher(disponibilidade, request);
        return toResponse(repository.save(disponibilidade));
    }

    @Transactional
    public void remover(Long id) {
        log.info("Removendo disponibilidade {}", id);
        repository.delete(buscarEntidade(id));
    }

    private DisponibilidadeMedica buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Disponibilidade", id));
    }

    private void preencher(DisponibilidadeMedica d, DisponibilidadeRequest request) {
        d.setMedico(medicoService.buscarReferencia(request.medicoId()));
        d.setSala(request.salaId() == null ? null : salaService.buscarReferencia(request.salaId()));
        d.setHoraInicial(request.horaInicial());
        d.setHoraFinal(request.horaFinal());
        d.setObservacoes(request.observacoes());
        if (!d.horarioValido()) {
            throw new BusinessRuleException("A hora final deve ser depois da hora inicial");
        }

        if (d instanceof DisponibilidadeDiaria diaria) {
            if (request.diaSemana() == null) {
                throw new BusinessRuleException("diaSemana é obrigatório para disponibilidade DIARIA");
            }
            diaria.setDiaSemana(request.diaSemana());
        } else if (d instanceof DisponibilidadeExcecao excecao) {
            if (request.data() == null || request.disponivel() == null) {
                throw new BusinessRuleException("data e disponivel são obrigatórios para disponibilidade EXCECAO");
            }
            excecao.setData(request.data());
            excecao.setDisponivel(request.disponivel());
            excecao.setMotivo(request.motivo());
        }

        validarConflitos(d);
    }

    private void validarConflitos(DisponibilidadeMedica d) {
        boolean medicoOcupado = repository.findAllByMedicoId(d.getMedico().getId()).stream()
                .filter(DisponibilidadeMedica::isAtivo)
                .anyMatch(outra -> !Objects.equals(outra.getId(), d.getId())
                        && mesmoDia(outra, d)
                        && outra.sobrepoeHorario(d));
        if (medicoOcupado) {
            throw new ConflictException("O médico já tem disponibilidade sobreposta neste dia e horário");
        }
        if (d.getSala() != null && d instanceof DisponibilidadeDiaria) {
            boolean salaOcupada = repository.findAllBySalaId(d.getSala().getId()).stream()
                    .anyMatch(outra -> !Objects.equals(outra.getId(), d.getId())
                            && mesmoDia(outra, d)
                            && outra.sobrepoeHorario(d));
            if (salaOcupada) {
                throw new ConflictException("A sala já está reservada neste dia e horário");
            }
        }
    }

    /** Mesmo dia: duas semanais no mesmo dia da semana, ou duas exceções na mesma data. */
    private static boolean mesmoDia(DisponibilidadeMedica a, DisponibilidadeMedica b) {
        if (a instanceof DisponibilidadeDiaria da && b instanceof DisponibilidadeDiaria db) {
            return da.getDiaSemana() == db.getDiaSemana();
        }
        if (a instanceof DisponibilidadeExcecao ea && b instanceof DisponibilidadeExcecao eb) {
            return Objects.equals(ea.getData(), eb.getData());
        }
        // Semanal x exceção não conflitam: a exceção existe justamente para alterar o dia
        return false;
    }

    private DisponibilidadeResponse toResponse(DisponibilidadeMedica d) {
        Referencia sala = d.getSala() == null ? null : new Referencia(d.getSala().getId(), d.getSala().getIdentificacao());
        var diaSemana = d instanceof DisponibilidadeDiaria diaria ? diaria.getDiaSemana() : null;
        DisponibilidadeExcecao excecao = d instanceof DisponibilidadeExcecao e ? e : null;
        return new DisponibilidadeResponse(d.getId(), d.getTipo(),
                new Referencia(d.getMedico().getId(), d.getMedico().getNome()), sala,
                d.getHoraInicial(), d.getHoraFinal(), d.getObservacoes(), diaSemana,
                excecao == null ? null : excecao.getData(),
                excecao == null ? null : excecao.getDisponivel(),
                excecao == null ? null : excecao.getMotivo(),
                d.getUuid(), d.isAtivo(), d.getCriadoPor(), d.getAtualizadoPor(),
                d.getDataCriacao(), d.getDataModificacao());
    }
}
