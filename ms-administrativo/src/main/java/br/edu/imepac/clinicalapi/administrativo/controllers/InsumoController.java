package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.InsumoCustoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.InsumoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.InsumoResponse;
import br.edu.imepac.clinicalapi.administrativo.services.InsumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Insumos. Escrita: ADMIN e ms-estoque (X-Api-Key). Contrato: static/contract/openapi-administrativo.yaml
 */
@RestController
@RequestMapping("/insumos")
@RequiredArgsConstructor
public class InsumoController {

    private final InsumoService service;

    @PostMapping
    public ResponseEntity<InsumoResponse> criar(@Valid @RequestBody InsumoRequest request) {
        InsumoResponse criado = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.getId()).toUri()).body(criado);
    }

    @GetMapping
    public Page<InsumoResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public InsumoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public InsumoResponse atualizar(@PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return service.atualizar(id, request);
    }

    /** Atualiza só o custo unitário (ex.: o ms-estoque após registrar uma compra). */
    @PatchMapping("/{id}/custo")
    public InsumoResponse atualizarCusto(@PathVariable Long id, @Valid @RequestBody InsumoCustoRequest request) {
        return service.atualizarCusto(id, request.custoUnitario());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
