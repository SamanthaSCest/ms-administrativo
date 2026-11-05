package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.repositories.PacienteIntegracaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class PacienteIntegracaoService {

    private final PacienteIntegracaoRepository pacienteRepository;

    public PacienteIntegracaoService(PacienteIntegracaoRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    public PacienteResumo buscarResumo(UUID id) {
        return pacienteRepository.buscarResumo(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado"));
    }

    public PacienteContato buscarContato(UUID id) {
        return pacienteRepository.buscarContato(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado"));
    }

    public SituacaoResponse buscarSituacao(UUID id) {
        return pacienteRepository.buscarSituacao(id);
    }
}
