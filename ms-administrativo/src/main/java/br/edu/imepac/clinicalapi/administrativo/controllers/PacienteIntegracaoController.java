package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.services.PacienteIntegracaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de integração (só leitura) do paciente. Contrato: static/contract/openapi-administrativo.yaml */
@RestController
@RequestMapping("/integracao/pacientes/{id}")
public class PacienteIntegracaoController {

    private final PacienteIntegracaoService service;

    public PacienteIntegracaoController(PacienteIntegracaoService service) {
        this.service = service;
    }

    @GetMapping
    public PacienteResumo buscarResumo(@PathVariable Long id) {
        return service.buscarResumo(id);
    }

    @GetMapping("/situacao")
    public SituacaoResponse buscarSituacao(@PathVariable Long id) {
        return service.buscarSituacao(id);
    }

    @GetMapping("/contato")
    public PacienteContato buscarContato(@PathVariable Long id) {
        return service.buscarContato(id);
    }
}
