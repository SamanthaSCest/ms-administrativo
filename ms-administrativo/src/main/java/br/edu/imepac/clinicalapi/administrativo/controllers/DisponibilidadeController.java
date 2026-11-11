package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.DisponibilidadeRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.DisponibilidadeResponse;
import br.edu.imepac.clinicalapi.administrativo.services.DisponibilidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

/**
 * Disponibilidade dos médicos: o ADMIN cadastra; todos (e o ms-agendamento) consultam.
 * Contrato: static/contract/openapi-administrativo.yaml
 */
@RestController
@RequestMapping("/disponibilidades")
@RequiredArgsConstructor
public class DisponibilidadeController {

    private final DisponibilidadeService service;

    @PostMapping
    public ResponseEntity<DisponibilidadeResponse> criar(@Valid @RequestBody DisponibilidadeRequest request) {
        DisponibilidadeResponse criada = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.id()).toUri()).body(criada);
    }

    /** Lista todas ou só as do médico informado. */
    @GetMapping
    public Page<DisponibilidadeResponse> listar(@RequestParam(required = false) Long medicoId, Pageable pageable) {
        return service.listar(medicoId, pageable);
    }

    /** Horários em que o médico atende na data (semanais menos bloqueios, mais horários extras). */
    @GetMapping("/agenda")
    public List<DisponibilidadeResponse> agenda(@RequestParam Long medicoId,
                                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return service.agenda(medicoId, data);
    }

    @GetMapping("/{id}")
    public DisponibilidadeResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public DisponibilidadeResponse atualizar(@PathVariable Long id, @Valid @RequestBody DisponibilidadeRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
