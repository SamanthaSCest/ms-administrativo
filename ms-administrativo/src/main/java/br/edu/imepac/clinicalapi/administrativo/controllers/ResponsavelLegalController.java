package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.ResponsavelLegalRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.ResponsavelLegalResponse;
import br.edu.imepac.clinicalapi.administrativo.services.ResponsavelLegalService;
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
@RequestMapping("/responsaveis")
@RequiredArgsConstructor
public class ResponsavelLegalController {

    private final ResponsavelLegalService service;

    @PostMapping
    public ResponseEntity<ResponsavelLegalResponse> criar(@Valid @RequestBody ResponsavelLegalRequest request) {
        ResponsavelLegalResponse criado = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.getId()).toUri()).body(criado);
    }

    @GetMapping
    public Page<ResponsavelLegalResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ResponsavelLegalResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public ResponsavelLegalResponse atualizar(@PathVariable Long id, @Valid @RequestBody ResponsavelLegalRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
