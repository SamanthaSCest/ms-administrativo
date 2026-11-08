package br.edu.imepac.clinicalapi.agendamento.controllers;

import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaRequest;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaResponse;
import br.edu.imepac.clinicalapi.agendamento.dto.ConsultaUpdateRequest;
import br.edu.imepac.clinicalapi.agendamento.entities.StatusConsulta;
import br.edu.imepac.clinicalapi.agendamento.services.ConsultaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Contrato: static/contract/openapi-agendamento.yaml */
@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService service;

    // @Valid dispara o Bean Validation do DTO; se falhar => 400 (tratado no GlobalExceptionHandler)
    @PostMapping
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody ConsultaRequest request) {
        ConsultaResponse criada = service.agendar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criada.getId()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public Page<ConsultaResponse> listar(@RequestParam(required = false) Long medicoId,
                                         @RequestParam(required = false) Long pacienteId,
                                         @RequestParam(required = false) StatusConsulta status,
                                         Pageable pageable) {
        return service.listar(medicoId, pacienteId, status, pageable);
    }

    @GetMapping("/{id}")
    public ConsultaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ConsultaResponse remarcar(@PathVariable Long id, @Valid @RequestBody ConsultaUpdateRequest request) {
        return service.remarcar(id, request);
    }

    // PATCH = alteração parcial; aqui usado para ações de mudança de status
    @PatchMapping("/{id}/cancelar")
    public ConsultaResponse cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    @PatchMapping("/{id}/realizar")
    public ConsultaResponse realizar(@PathVariable Long id) {
        return service.realizar(id);
    }
}
