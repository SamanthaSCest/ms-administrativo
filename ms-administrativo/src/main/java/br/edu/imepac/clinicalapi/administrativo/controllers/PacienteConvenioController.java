package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.PacienteConvenioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.PacienteConvenioResponse;
import br.edu.imepac.clinicalapi.administrativo.services.PacienteConvenioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Vínculos do paciente com convênios (RF06, HU04). Contrato: static/contract/openapi-administrativo.yaml */
@RestController
@RequestMapping("/pacientes/{pacienteId}/convenios")
public class PacienteConvenioController {

    private final PacienteConvenioService service;

    public PacienteConvenioController(PacienteConvenioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PacienteConvenioResponse> vincular(@PathVariable Long pacienteId,
                                                             @Valid @RequestBody PacienteConvenioRequest request) {
        PacienteConvenioResponse criado = service.vincular(pacienteId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    /** Todos os vínculos, inclusive os desvinculados (histórico); ativos primeiro. */
    @GetMapping
    public List<PacienteConvenioResponse> listar(@PathVariable Long pacienteId) {
        return service.listar(pacienteId);
    }

    @GetMapping("/{vinculoId}")
    public PacienteConvenioResponse buscar(@PathVariable Long pacienteId, @PathVariable Long vinculoId) {
        return service.buscar(pacienteId, vinculoId);
    }

    @PutMapping("/{vinculoId}")
    public PacienteConvenioResponse atualizar(@PathVariable Long pacienteId, @PathVariable Long vinculoId,
                                              @Valid @RequestBody PacienteConvenioRequest request) {
        return service.atualizar(pacienteId, vinculoId, request);
    }

    /** Desvincula (inativa): o vínculo continua no histórico. */
    @DeleteMapping("/{vinculoId}")
    public ResponseEntity<Void> desvincular(@PathVariable Long pacienteId, @PathVariable Long vinculoId) {
        service.desvincular(pacienteId, vinculoId);
        return ResponseEntity.noContent().build();
    }
}
