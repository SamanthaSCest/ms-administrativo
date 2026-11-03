package br.com.clinica.ms_administrativo.controller;

import br.com.clinica.ms_administrativo.paciente.PacienteIntegracaoService;
import br.com.clinica.ms_administrativo.dto.PacienteResumo;
import org.springframework.web.bind.annotation.*;
import br.com.clinica.ms_administrativo.dto.SituacaoResponse;
import br.com.clinica.ms_administrativo.dto.PacienteContato;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integracao/pacientes")
public class PacienteIntegracaoController {

    private final PacienteIntegracaoService pacienteService;

    public PacienteIntegracaoController(PacienteIntegracaoService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping("/{id}")
    public PacienteResumo buscarPorId(@PathVariable("id") UUID id) {
        return pacienteService.buscarResumo(id);
    }

    @GetMapping("/{id}/situacao")
    public SituacaoResponse consultarSituacao(@PathVariable("id") UUID id) {
        return pacienteService.buscarSituacao(id);
    }

    @GetMapping("/{id}/contato")
    public PacienteContato consultarContato(@PathVariable("id") UUID id) {
        return pacienteService.buscarContato(id);
    }
}