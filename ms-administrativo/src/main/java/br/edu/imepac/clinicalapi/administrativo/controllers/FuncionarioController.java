package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioBuscaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.FuncionarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.exceptions.NegocioException;
import br.edu.imepac.clinicalapi.administrativo.services.FuncionarioService;
import br.edu.imepac.clinicalapi.administrativo.validation.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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

import java.net.URI;

/**
 * Cadastro de funcionários (FUNCIONARIO_CONSULTAR / FUNCIONARIO_GERENCIAR, ver SecurityConfig).
 * Contrato: static/contract/openapi-administrativo.yaml
 */
@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioService service;

    // OnCreate: no cadastro a senha é obrigatória
    @PostMapping
    public ResponseEntity<FuncionarioResponse> criar(
            @Validated({Default.class, OnCreate.class}) @RequestBody FuncionarioRequest request,
            Authentication authentication) {
        exigirGestaoDeAcessosSeHouverGrupos(request, authentication);
        FuncionarioResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(criado.getId()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    /** Sem "ativo", traz só os ativos. CPF mascarado. Filtros por nome/CPF ficam no POST /busca (RNF08). */
    @GetMapping
    public Page<FuncionarioResponse> listar(@RequestParam(required = false) Boolean ativo, Pageable pageable) {
        return service.listar(ativo, pageable);
    }

    /** Busca com dados pessoais no corpo, e não na URL (RNF08). Paginação continua na query (?page=&size=). */
    @PostMapping("/busca")
    public Page<FuncionarioResponse> buscar(@Valid @RequestBody FuncionarioBuscaRequest filtro, Pageable pageable) {
        return service.buscar(filtro, pageable);
    }

    @GetMapping("/{id}")
    public FuncionarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PutMapping("/{id}")
    public FuncionarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioRequest request,
                                         Authentication authentication) {
        exigirGestaoDeAcessosSeHouverGrupos(request, authentication);
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Escolher grupos é dar acesso: além de FUNCIONARIO_GERENCIAR (rota), exige USUARIO_GERENCIAR.
     * Sem isso, quem só cadastra funcionários poderia dar a alguém (ou a um cúmplice) mais acesso do que tem.
     */
    private static void exigirGestaoDeAcessosSeHouverGrupos(FuncionarioRequest request, Authentication authentication) {
        boolean podeGerenciarAcessos = authentication.getAuthorities().stream()
                .anyMatch(a -> Permissao.USUARIO_GERENCIAR.name().equals(a.getAuthority()));
        if (request.getGrupoIds() != null && !podeGerenciarAcessos) {
            throw new NegocioException(HttpStatus.FORBIDDEN, "ADM-FUN-001",
                    "Escolher grupos de acesso exige a permissão USUARIO_GERENCIAR");
        }
    }
}
