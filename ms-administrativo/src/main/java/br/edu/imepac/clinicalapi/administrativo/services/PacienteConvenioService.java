package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.PacienteConvenioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteConvenioResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteConvenioResumo;
import br.edu.imepac.clinicalapi.administrativo.entities.Convenio;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.entities.PacienteConvenio;
import br.edu.imepac.clinicalapi.administrativo.exceptions.NegocioException;
import br.edu.imepac.clinicalapi.administrativo.mapper.ResumoMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.ConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteConvenioRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Vínculo do paciente com convênio (RF06, HU04) e vigência (RN16, RF24).
 * Regras: convênio ativo (RN06, ADM-PCV-001), um vínculo ativo por convênio (RN05, ADM-PCV-002),
 * carteirinha não vencida (RN07, ADM-PCV-003), paciente ativo (ADM-PCV-004) e
 * vínculo desvinculado não muda mais (ADM-PCV-005).
 * Eventos: paciente.convenio-vinculado, paciente.convenio-atualizado e paciente.convenio-desvinculado,
 * com o id do paciente como agregadoId.
 */
@Service
public class PacienteConvenioService {

    private static final Logger log = LoggerFactory.getLogger(PacienteConvenioService.class);

    static final String EVENTO_VINCULADO = "paciente.convenio-vinculado";
    static final String EVENTO_ATUALIZADO = "paciente.convenio-atualizado";
    static final String EVENTO_DESVINCULADO = "paciente.convenio-desvinculado";

    private final PacienteConvenioRepository repository;
    private final PacienteRepository pacienteRepository;
    private final ConvenioRepository convenioRepository;
    private final ResumoMapper resumoMapper;
    private final OutboxService outbox;
    private final EventoService eventos;

    public PacienteConvenioService(PacienteConvenioRepository repository, PacienteRepository pacienteRepository,
                                   ConvenioRepository convenioRepository, ResumoMapper resumoMapper,
                                   OutboxService outbox, EventoService eventos) {
        this.repository = repository;
        this.pacienteRepository = pacienteRepository;
        this.convenioRepository = convenioRepository;
        this.resumoMapper = resumoMapper;
        this.outbox = outbox;
        this.eventos = eventos;
    }

    @Transactional
    public PacienteConvenioResponse vincular(Long pacienteId, PacienteConvenioRequest request) {
        log.info("Vinculando paciente {} ao convênio {}", pacienteId, request.convenioId());
        Paciente paciente = buscarPaciente(pacienteId);
        if (!paciente.isAtivo()) {
            throw negocio(HttpStatus.UNPROCESSABLE_ENTITY, "ADM-PCV-004",
                    "Paciente inativo não pode ser vinculado a convênio");
        }
        Convenio convenio = buscarConvenioAtivo(request.convenioId());
        if (repository.existsByPacienteIdAndConvenioIdAndAtivoTrue(pacienteId, convenio.getId())) {
            throw vinculoDuplicado();
        }
        validarCarteirinha(request);

        PacienteConvenio vinculo = new PacienteConvenio();
        vinculo.setPaciente(paciente);
        preencher(vinculo, convenio, request);
        PacienteConvenio salvo = repository.save(vinculo);
        outbox.registrar(EVENTO_VINCULADO, pacienteId, resumoMapper.pacienteConvenio(salvo), List.of());
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<PacienteConvenioResponse> listar(Long pacienteId) {
        buscarPaciente(pacienteId);
        return repository.findByPacienteIdOrderByAtivoDescIdAsc(pacienteId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PacienteConvenioResponse buscar(Long pacienteId, Long vinculoId) {
        return toResponse(buscarVinculo(pacienteId, vinculoId));
    }

    /** PUT substitui convênio, carteirinha e validade, com as mesmas regras do cadastro. */
    @Transactional
    public PacienteConvenioResponse atualizar(Long pacienteId, Long vinculoId, PacienteConvenioRequest request) {
        log.info("Atualizando vínculo {} do paciente {}", vinculoId, pacienteId);
        PacienteConvenio vinculo = buscarVinculo(pacienteId, vinculoId);
        if (!vinculo.isAtivo()) {
            throw negocio(HttpStatus.UNPROCESSABLE_ENTITY, "ADM-PCV-005",
                    "Vínculo desvinculado não pode ser alterado; crie um vínculo novo");
        }
        // Mantendo o mesmo convênio, a edição continua possível mesmo que ele tenha sido inativado
        Convenio convenio = vinculo.getConvenio().getId().equals(request.convenioId())
                ? vinculo.getConvenio()
                : buscarConvenioAtivo(request.convenioId());
        if (repository.existsByPacienteIdAndConvenioIdAndAtivoTrueAndIdNot(pacienteId, convenio.getId(), vinculoId)) {
            throw vinculoDuplicado();
        }
        validarCarteirinha(request);

        var antes = eventos.retrato(resumoMapper.pacienteConvenio(vinculo));
        preencher(vinculo, convenio, request);
        PacienteConvenio salvo = repository.save(vinculo);
        PacienteConvenioResumo depois = resumoMapper.pacienteConvenio(salvo);
        List<String> alterados = eventos.camposAlterados(antes, depois);
        if (!alterados.isEmpty()) {
            outbox.registrar(EVENTO_ATUALIZADO, pacienteId, depois, alterados);
        }
        return toResponse(salvo);
    }

    /** DELETE desvincula (inativa). Repetir não publica outro evento. */
    @Transactional
    public void desvincular(Long pacienteId, Long vinculoId) {
        log.info("Desvinculando vínculo {} do paciente {}", vinculoId, pacienteId);
        PacienteConvenio vinculo = buscarVinculo(pacienteId, vinculoId);
        if (!vinculo.isAtivo()) {
            return;
        }
        vinculo.inativar();
        PacienteConvenio salvo = repository.save(vinculo);
        outbox.registrar(EVENTO_DESVINCULADO, pacienteId, resumoMapper.pacienteConvenio(salvo), List.of("ativo"));
    }

    /** Integração (RF24): vínculos vigentes do paciente na data (hoje, se não informada). */
    @Transactional(readOnly = true)
    public List<PacienteConvenioResumo> vigentes(Long pacienteId, LocalDate data) {
        buscarPaciente(pacienteId);
        LocalDate referencia = data == null ? LocalDate.now() : data;
        return repository.findByPacienteIdAndAtivoTrueOrderByIdAsc(pacienteId).stream()
                .filter(v -> v.isVigente(referencia))
                .map(resumoMapper::pacienteConvenio)
                .toList();
    }

    private static void preencher(PacienteConvenio vinculo, Convenio convenio, PacienteConvenioRequest request) {
        vinculo.setConvenio(convenio);
        vinculo.setNumeroCarteirinha(request.numeroCarteirinha().trim());
        vinculo.setValidade(request.validade());
    }

    /** RN07: carteirinha vencida não entra. */
    private static void validarCarteirinha(PacienteConvenioRequest request) {
        if (request.validade() != null && request.validade().isBefore(LocalDate.now())) {
            throw negocio(HttpStatus.UNPROCESSABLE_ENTITY, "ADM-PCV-003", "Carteirinha vencida em " + request.validade());
        }
    }

    /** RN06: o convênio precisa existir e estar ativo. */
    private Convenio buscarConvenioAtivo(Long convenioId) {
        Convenio convenio = convenioRepository.findById(convenioId)
                .orElseThrow(() -> negocio(HttpStatus.UNPROCESSABLE_ENTITY, "ADM-PCV-001",
                        "Convênio não encontrado: " + convenioId));
        if (!convenio.isAtivo()) {
            throw negocio(HttpStatus.UNPROCESSABLE_ENTITY, "ADM-PCV-001", "Convênio inativo: " + convenioId);
        }
        return convenio;
    }

    private static NegocioException vinculoDuplicado() {
        return negocio(HttpStatus.CONFLICT, "ADM-PCV-002", "Paciente já tem vínculo ativo com este convênio");
    }

    private static NegocioException negocio(HttpStatus status, String codigo, String mensagem) {
        return new NegocioException(status, codigo, mensagem);
    }

    private Paciente buscarPaciente(Long id) {
        return pacienteRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Paciente", id));
    }

    private PacienteConvenio buscarVinculo(Long pacienteId, Long vinculoId) {
        buscarPaciente(pacienteId);
        return repository.findByIdAndPacienteId(vinculoId, pacienteId)
                .orElseThrow(() -> ResourceNotFoundException.of("Vínculo com convênio", vinculoId));
    }

    private PacienteConvenioResponse toResponse(PacienteConvenio v) {
        return new PacienteConvenioResponse(v.getId(), v.getUuid(), v.getPaciente().getId(),
                resumoMapper.convenio(v.getConvenio()), v.getNumeroCarteirinha(), v.getValidade(), v.isAtivo(),
                v.isVigente(LocalDate.now()), v.getCriadoPor(), v.getAtualizadoPor(), v.getDataCriacao(),
                v.getDataModificacao());
    }
}
