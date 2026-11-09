package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Paciente;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteRepository;
import br.edu.imepac.clinicalapi.administrativo.utils.CpfMascara;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leituras do paciente para outros microserviços (somente leitura).
 * Diferente do CRUD, expõe o mínimo de dado pessoal: CPF mascarado e só o contato quando pedido.
 */
@Service
public class PacienteIntegracaoService {

    private final PacienteRepository repository;

    public PacienteIntegracaoService(PacienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PacienteResumo buscarResumo(Long id) {
        Paciente paciente = buscarEntidade(id);
        return new PacienteResumo(paciente.getId(), paciente.getNome(),
                CpfMascara.mascarar(paciente.getCpf()), paciente.getDataNascimento());
    }

    @Transactional(readOnly = true)
    public PacienteContato buscarContato(Long id) {
        Paciente paciente = buscarEntidade(id);
        return new PacienteContato(paciente.getNome(), paciente.getEmail(), paciente.getTelefone());
    }

    /** Responde sempre 200: id inexistente devolve existe = false (o chamador não trata como erro). */
    @Transactional(readOnly = true)
    public SituacaoResponse buscarSituacao(Long id) {
        boolean existe = repository.existsById(id);
        return new SituacaoResponse(existe, existe);
    }

    private Paciente buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Paciente", id));
    }
}
