package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.services.PacienteIntegracaoService;
import org.springframework.web.bind.annotation.*;

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