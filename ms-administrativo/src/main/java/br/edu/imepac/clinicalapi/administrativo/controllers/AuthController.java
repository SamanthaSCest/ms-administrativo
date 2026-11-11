package br.edu.imepac.clinicalapi.administrativo.controllers;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;
import br.edu.imepac.clinicalapi.administrativo.dto.auth.AlterarSenhaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.auth.LoginRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.auth.TokenResponse;
import br.edu.imepac.clinicalapi.administrativo.services.AuthService;
import br.edu.imepac.clinicalapi.administrativo.services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticação. {@code /auth/login} é público; os demais exigem Bearer token.
 * Contrato: static/contract/openapi-administrativo.yaml
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Dados do usuário dono do token (authentication.getName() = claim "sub" = login). */
    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return usuarioService.buscarPorLogin(authentication.getName());
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(Authentication authentication,
                                             @Valid @RequestBody AlterarSenhaRequest request) {
        authService.alterarSenha(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
