package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.ProcedimentoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.ProcedimentoResponse;
import br.edu.imepac.clinicalapi.administrativo.services.ProcedimentoService;
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
@RequestMapping("/procedimentos")
@RequiredArgsConstructor
public class ProcedimentoController {

    private final ProcedimentoService service;

    @PostMapping
    public ResponseEntity<ProcedimentoResponse> criar(@Valid @RequestBody ProcedimentoRequest request) {
        ProcedimentoResponse criado = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.id()).toUri()).body(criado);
    }

    @GetMapping
    public Page<ProcedimentoResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ProcedimentoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ProcedimentoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProcedimentoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
