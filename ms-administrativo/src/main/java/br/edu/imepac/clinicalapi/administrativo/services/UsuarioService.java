package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Perfil;
import br.edu.imepac.clinicalapi.administrativo.entities.PossuiUsuario;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras do usuário de acesso: login único, senha sempre em hash BCrypt e vínculo com Médico/Funcionário.
 * O login é o CPF. Paciente não tem usuário.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    /**
     * Cria (sem salvar) o usuário da pessoa recém-cadastrada; é salvo em cascata junto com a pessoa.
     */
    public Usuario novoUsuario(String login, String senha, Perfil perfil) {
        if (repository.existsByLogin(login)) {
            throw new ConflictException("Já existe usuário com o login informado");
        }
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }

    /**
     * Mantém o usuário coerente após atualizar a pessoa: login = CPF atual, perfil informado
     * e, se uma senha nova vier, troca a senha. Registros antigos sem usuário ganham um quando a senha é enviada.
     */
    public void sincronizar(PossuiUsuario pessoa, String novaSenha, Perfil perfil) {
        boolean temSenha = novaSenha != null && !novaSenha.isBlank();
        Usuario usuario = pessoa.getUsuario();
        if (usuario == null) {
            if (temSenha) {
                pessoa.setUsuario(novoUsuario(pessoa.getCpf(), novaSenha, perfil));
            }
            return;
        }
        if (!usuario.getLogin().equals(pessoa.getCpf())) {
            if (repository.existsByLoginAndIdNot(pessoa.getCpf(), usuario.getId())) {
                throw new ConflictException("Já existe usuário com o login informado");
            }
            usuario.setLogin(pessoa.getCpf());
        }
        if (temSenha) {
            usuario.setSenha(passwordEncoder.encode(novaSenha));
        }
        usuario.setPerfil(perfil);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorLogin(String login) {
        return toResponse(repository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + login)));
    }

    /** Ativa/desativa o acesso. O administrador não pode desativar o próprio usuário (evita se trancar fora). */
    @Transactional
    public UsuarioResponse alterarStatus(Long id, boolean ativo, String loginAutenticado) {
        Usuario usuario = buscarEntidade(id);
        if (!ativo && usuario.getLogin().equals(loginAutenticado)) {
            throw new BusinessRuleException("Não é possível desativar o próprio usuário");
        }
        log.info("Usuário {} {}", id, ativo ? "ativado" : "desativado");
        usuario.setAtivo(ativo);
        return toResponse(repository.save(usuario));
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return modelMapper.map(usuario, UsuarioResponse.class);
    }

    private Usuario buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Usuário", id));
    }
}
