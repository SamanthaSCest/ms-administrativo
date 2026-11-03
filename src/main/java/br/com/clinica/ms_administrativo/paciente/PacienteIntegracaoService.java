package br.com.clinica.ms_administrativo.paciente;

import br.com.clinica.ms_administrativo.dto.PacienteContato;
import br.com.clinica.ms_administrativo.dto.PacienteResumo;
import br.com.clinica.ms_administrativo.dto.SituacaoResponse;
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
