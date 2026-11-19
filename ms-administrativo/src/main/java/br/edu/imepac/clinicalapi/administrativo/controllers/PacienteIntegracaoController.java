package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteContato;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteConvenioResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.PacienteResumo;
import br.edu.imepac.clinicalapi.administrativo.dto.integracao.SituacaoResponse;
import br.edu.imepac.clinicalapi.administrativo.services.PacienteConvenioService;
import br.edu.imepac.clinicalapi.administrativo.services.PacienteIntegracaoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** API de integração (só leitura) do paciente. Contrato: static/contract/openapi-administrativo.yaml */
@RestController
@RequestMapping("/integracao/pacientes/{id}")
public class PacienteIntegracaoController {

    private final PacienteIntegracaoService service;
    private final PacienteConvenioService convenioService;

    public PacienteIntegracaoController(PacienteIntegracaoService service, PacienteConvenioService convenioService) {
        this.service = service;
        this.convenioService = convenioService;
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

    /** RF24: convênios vigentes do paciente na data (yyyy-MM-dd; hoje, se omitida). */
    @GetMapping("/convenios/vigentes")
    public List<PacienteConvenioResumo> conveniosVigentes(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return convenioService.vigentes(id, data);
    }
}
