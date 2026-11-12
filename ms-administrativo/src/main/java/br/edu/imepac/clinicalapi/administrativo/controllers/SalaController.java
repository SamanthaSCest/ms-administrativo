package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.SalaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.SalaResponse;
import br.edu.imepac.clinicalapi.administrativo.services.SalaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

/** Contrato: static/contract/openapi-administrativo.yaml */
@RestController
@RequestMapping("/salas")
@RequiredArgsConstructor
public class SalaController {

    private final SalaService service;

    @PostMapping
    public ResponseEntity<SalaResponse> criar(@Valid @RequestBody SalaRequest request) {
        SalaResponse criada = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.getId()).toUri()).body(criada);
    }

    @GetMapping
    public Page<SalaResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public SalaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public SalaResponse atualizar(@PathVariable Long id, @Valid @RequestBody SalaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
