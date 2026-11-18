package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioAcessosRequest;
import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.entities.PossuiUsuario;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import br.edu.imepac.clinicalapi.administrativo.repositories.GrupoRepository;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Regras do usuário de acesso: login único, senha sempre em hash BCrypt, vínculo com Médico/Funcionário
 * e acessos por RBAC (grupos + permissões avulsas). O login é o CPF. Paciente não tem usuário.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final GrupoRepository grupoRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria (sem salvar) o usuário da pessoa recém-cadastrada, já no grupo padrão do cargo (ou MEDICO);
     * é salvo em cascata junto com a pessoa.
     */
    public Usuario novoUsuario(String login, String senha, Grupo grupo) {
        if (repository.existsByLogin(login)) {
            throw new ConflictException("Já existe usuário com o login informado");
        }
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.adicionarGrupo(grupo);
        usuario.setAtivo(true);
        return usuario;
    }

    /**
     * Mantém o usuário coerente após atualizar a pessoa: login = CPF atual e, se uma senha nova vier, troca a senha.
     * Registros antigos sem usuário ganham um (no grupo informado) quando a senha é enviada.
     * Os grupos de um usuário que já existe não mudam aqui (ver {@link #trocarGrupo}).
     */
    public void sincronizar(PossuiUsuario pessoa, String novaSenha, Grupo grupo) {
        boolean temSenha = novaSenha != null && !novaSenha.isBlank();
        Usuario usuario = pessoa.getUsuario();
        if (usuario == null) {
            if (temSenha) {
                pessoa.setUsuario(novoUsuario(pessoa.getCpf(), novaSenha, grupo));
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
    }

    /**
     * Mudou o cargo: sai do grupo do cargo anterior e entra no do novo. Os demais grupos e as permissões
     * avulsas que o administrador deu continuam.
     */
    public void trocarGrupo(Usuario usuario, Grupo anterior, Grupo novo) {
        if (usuario == null || anterior.getId().equals(novo.getId())) {
            return;
        }
        usuario.removerGrupo(anterior);
        usuario.adicionarGrupo(novo);
    }

    /** Grupo padrão pelo nome (ADMINISTRADOR, RECEPCIONISTA, ..., MEDICO). Criado pela migration V14. */
    @Transactional(readOnly = true)
    public Grupo grupoPadrao(String nome) {
        return grupoRepository.findByNome(nome)
                .orElseThrow(() -> new BusinessRuleException("Grupo de acesso padrão não encontrado: " + nome));
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

    /**
     * Define os grupos e as permissões avulsas do usuário (substitui os atuais). Vale a partir do próximo login.
     * O administrador não altera os próprios acessos (evita tirar de si mesmo o acesso de gerenciar usuários).
     */
    @Transactional
    public UsuarioResponse definirAcessos(Long id, UsuarioAcessosRequest request, String loginAutenticado) {
        Usuario usuario = buscarEntidade(id);
        if (usuario.getLogin().equals(loginAutenticado)) {
            throw new BusinessRuleException("Não é possível alterar os próprios acessos");
        }
        Set<Long> ids = request.grupoIds() == null ? Set.of() : request.grupoIds();
        List<Grupo> grupos = grupoRepository.findAllById(ids);
        if (grupos.size() != ids.size()) {
            Set<Long> encontrados = new HashSet<>();
            grupos.forEach(g -> encontrados.add(g.getId()));
            Set<Long> faltando = new HashSet<>(ids);
            faltando.removeAll(encontrados);
            throw new BusinessRuleException("Grupo(s) de acesso não encontrado(s): " + faltando);
        }
        Set<Permissao> permissoes = request.permissoes() == null || request.permissoes().isEmpty()
                ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(request.permissoes());
        usuario.definirAcessos(new HashSet<>(grupos), permissoes);
        log.info("Acessos do usuário {} alterados: grupos {}, permissões avulsas {}", id, ids, permissoes);
        return toResponse(repository.save(usuario));
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.de(usuario);
    }

    private Usuario buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Usuário", id));
    }
}
