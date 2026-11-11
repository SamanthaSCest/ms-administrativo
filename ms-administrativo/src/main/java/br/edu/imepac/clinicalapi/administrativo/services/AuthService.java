package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.auth.AlterarSenhaRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.auth.LoginRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.auth.TokenResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import br.edu.imepac.clinicalapi.administrativo.exceptions.CredenciaisInvalidasException;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login (troca de credenciais por JWT) e troca de senha do próprio usuário.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = repository.findByLogin(request.getLogin())
                .filter(u -> passwordEncoder.matches(request.getSenha(), u.getSenha()))
                .filter(Usuario::isAtivo)
                // Mesma mensagem para login inexistente, senha errada ou usuário inativo (não revela qual falhou)
                .orElseThrow(() -> {
                    log.warn("Tentativa de login inválida");
                    return new CredenciaisInvalidasException();
                });
        log.info("Login do usuário {} (perfil {})", usuario.getId(), usuario.getPerfil());
        return new TokenResponse(tokenService.gerar(usuario), "Bearer", tokenService.validade().toSeconds(),
                usuarioService.toResponse(usuario));
    }

    @Transactional
    public void alterarSenha(String login, AlterarSenhaRequest request) {
        Usuario usuario = repository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + login));
        // 422 (e não 401): o token é válido; o que está errado é um dado do corpo
        if (!passwordEncoder.matches(request.getSenhaAtual(), usuario.getSenha())) {
            throw new BusinessRuleException("Senha atual incorreta");
        }
        usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        repository.save(usuario);
        log.info("Senha alterada pelo usuário {}", usuario.getId());
    }
}
