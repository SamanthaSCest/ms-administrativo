package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.identidade.ProvedorIdentidade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Nomes dos grupos de acesso do Keycloak (permissão USUARIO_GERENCIAR), para o front oferecer no campo "grupos"
 * do cadastro de funcionário. Criar grupos e definir as permissões de cada um é feito no Keycloak.
 */
@RestController
@RequiredArgsConstructor
public class GrupoController {

    private final ProvedorIdentidade identidade;

    @GetMapping("/grupos")
    public List<String> listar() {
        return identidade.listarGrupos();
    }
}
