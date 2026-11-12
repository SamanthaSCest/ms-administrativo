package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.mapper.ResumoMapper;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Leituras do paciente para outros microserviços (somente leitura).
 * Diferente do CRUD, expõe o mínimo de dado pessoal: CPF mascarado e só o contato quando pedido.
 */
@Service
public class PacienteIntegracaoService {

    private final PacienteRepository repository;
    private final ResumoMapper resumoMapper;

    public PacienteIntegracaoService(PacienteRepository repository, ResumoMapper resumoMapper) {
        this.repository = repository;
        this.resumoMapper = resumoMapper;
    }

    @Transactional(readOnly = true)
    public PacienteResumo buscarResumo(Long id) {
        return resumoMapper.paciente(buscarEntidade(id));
    }

    /** Busca em lote (RF22): ids inexistentes são ignorados; a lista vem ordenada por id. */
    @Transactional(readOnly = true)
    public List<PacienteResumo> buscarLote(List<Long> ids) {
        IntegracaoLimites.validarLote(ids);
        return repository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Paciente::getId))
                .map(resumoMapper::paciente)
                .toList();
    }

    @Transactional(readOnly = true)
    public PacienteContato buscarContato(Long id) {
        Paciente paciente = buscarEntidade(id);
        return new PacienteContato(paciente.getNome(), paciente.getEmail(), paciente.getTelefone());
    }

    /** Responde sempre 200: id inexistente devolve existe = false (o chamador não trata como erro). */
    @Transactional(readOnly = true)
    public SituacaoResponse buscarSituacao(Long id) {
        // ativo vem da EntidadeBase: paciente inativado continua existindo, mas com ativo = false
        return repository.findById(id)
                .map(p -> new SituacaoResponse(true, p.isAtivo()))
                .orElse(new SituacaoResponse(false, false));
    }

    private Paciente buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Paciente", id));
    }
}
