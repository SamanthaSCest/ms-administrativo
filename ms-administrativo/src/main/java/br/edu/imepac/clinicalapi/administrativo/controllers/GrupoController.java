package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.GrupoRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.GrupoResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.PermissaoResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.services.GrupoService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * RBAC: grupos de acesso e a lista de permissões existentes (permissão USUARIO_GERENCIAR, ver SecurityConfig).
 * Contrato: static/contract/openapi-administrativo.yaml
 */
@RestController
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoService service;

    /** Permissões são fixas no código: aqui só se consultam, para montar grupos e acessos. */
    @GetMapping("/permissoes")
    public List<PermissaoResponse> permissoes() {
        return Arrays.stream(Permissao.values()).map(PermissaoResponse::de).toList();
    }

    @PostMapping("/grupos")
    public ResponseEntity<GrupoResponse> criar(@Valid @RequestBody GrupoRequest request) {
        GrupoResponse criado = service.criar(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criado.id()).toUri()).body(criado);
    }

    @GetMapping("/grupos")
    public Page<GrupoResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/grupos/{id}")
    public GrupoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/grupos/{id}")
    public GrupoResponse atualizar(@PathVariable Long id, @Valid @RequestBody GrupoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/grupos/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
