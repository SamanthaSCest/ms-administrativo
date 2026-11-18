package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioAcessosRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioStatusRequest;
import br.edu.imepac.clinicalapi.administrativo.services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestão de usuários de acesso (permissão USUARIO_GERENCIAR, ver SecurityConfig). Usuários são criados junto com
 * médico ou funcionário; aqui o administrador consulta, ativa/desativa e define os acessos (grupos e permissões).
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PatchMapping("/{id}/status")
    public UsuarioResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody UsuarioStatusRequest request,
                                         Authentication authentication) {
        return service.alterarStatus(id, request.getAtivo(), authentication.getName());
    }

    /** RBAC: substitui os grupos e as permissões avulsas do usuário. Vale a partir do próximo login. */
    @PutMapping("/{id}/acessos")
    public UsuarioResponse definirAcessos(@PathVariable Long id, @RequestBody UsuarioAcessosRequest request,
                                          Authentication authentication) {
        return service.definirAcessos(id, request, authentication.getName());
    }
}
