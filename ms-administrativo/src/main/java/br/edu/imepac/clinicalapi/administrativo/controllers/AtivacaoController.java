package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.StatusRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.StatusResponse;
import br.edu.imepac.clinicalapi.administrativo.services.AtivacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * PATCH /{recurso}/{id}/status: ativa ou inativa um cadastro (EntidadeBase.inativar()/reativar()).
 * Quem pode chamar segue as regras de escrita de cada recurso no SecurityConfig
 * (ex.: pacientes -> ADMIN ou FUNCIONARIO; médicos -> ADMIN; insumos -> ADMIN ou ms-estoque).
 * Usuários têm o próprio endpoint: PATCH /usuarios/{id}/status.
 */
@RestController
@RequiredArgsConstructor
public class AtivacaoController {

    static final String RECURSOS = "especialidades|medicos|pacientes|funcionarios|convenios|salas|responsaveis"
            + "|insumos|procedimentos|custos-medicos|disponibilidades";

    private final AtivacaoService service;

    @PatchMapping("/{recurso:" + RECURSOS + "}/{id}/status")
    public StatusResponse alterarStatus(@PathVariable String recurso, @PathVariable Long id,
                                        @Valid @RequestBody StatusRequest request, Authentication authentication) {
        return service.alterarStatus(recurso, id, request.ativo(), authentication.getName());
    }
}
