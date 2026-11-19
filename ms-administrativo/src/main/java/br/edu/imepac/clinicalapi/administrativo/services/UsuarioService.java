package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;
import br.edu.imepac.clinicalapi.administrativo.entities.PossuiUsuario;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import br.edu.imepac.clinicalapi.administrativo.identidade.ProvedorIdentidade;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import br.edu.imepac.clinicalapi.common.exception.ConflictException;
import br.edu.imepac.clinicalapi.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Usuário de acesso de médicos e funcionários. Senha, grupos e situação moram no Keycloak
 * ({@link ProvedorIdentidade}); aqui fica o vínculo (keycloakId) e o login, que é o CPF. Paciente não tem usuário.
 * <p>
 * O Keycloak não participa da transação do banco. Por isso: o usuário criado no Keycloak é apagado lá se a
 * transação for desfeita (compensação), e a remoção no Keycloak só acontece depois do commit.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final ProvedorIdentidade identidade;

    /**
     * Cria o usuário da pessoa no Keycloak, já nos grupos informados (o do cargo, ou MEDICO, + extras), e devolve
     * o vínculo local (sem salvar: é salvo em cascata junto com a pessoa).
     */
    public Usuario novoUsuario(PossuiUsuario pessoa, String senha, Set<String> grupos) {
        if (repository.existsByLogin(pessoa.getCpf())) {
            throw new ConflictException("Já existe usuário com o login informado");
        }
        String keycloakId = identidade.criarUsuario(new ProvedorIdentidade.NovoUsuario(
                pessoa.getCpf(), senha, pessoa.getNome(), pessoa.getEmail(), true, grupos));
        desfazerSeHouverRollback(keycloakId);
        Usuario usuario = new Usuario();
        usuario.setLogin(pessoa.getCpf());
        usuario.setKeycloakId(keycloakId);
        usuario.setAtivo(true);
        return usuario;
    }

    /**
     * Mantém o usuário coerente após atualizar a pessoa: login = CPF atual, nome e e-mail no Keycloak e, se uma
     * senha nova vier, troca a senha. Quem ainda não tem usuário no Keycloak (registro antigo) ganha um, nos
     * grupos informados, quando a senha é enviada. Os grupos de um usuário que já existe não mudam aqui.
     */
    public void sincronizar(PossuiUsuario pessoa, String novaSenha, Set<String> gruposSeCriar) {
        boolean temSenha = novaSenha != null && !novaSenha.isBlank();
        Usuario usuario = pessoa.getUsuario();
        if (usuario == null) {
            if (temSenha) {
                pessoa.setUsuario(novoUsuario(pessoa, novaSenha, gruposSeCriar));
            }
            return;
        }
        if (!usuario.getLogin().equals(pessoa.getCpf())
                && repository.existsByLoginAndIdNot(pessoa.getCpf(), usuario.getId())) {
            throw new ConflictException("Já existe usuário com o login informado");
        }
        usuario.setLogin(pessoa.getCpf());
        if (usuario.getKeycloakId() == null) {
            if (temSenha) {
                vincularAoKeycloak(pessoa, usuario, novaSenha, gruposSeCriar);
            }
            return;
        }
        identidade.atualizarDados(usuario.getKeycloakId(), pessoa.getCpf(), pessoa.getNome(), pessoa.getEmail());
        if (temSenha) {
            identidade.definirSenha(usuario.getKeycloakId(), novaSenha);
        }
    }

    /**
     * Mudou o cargo: sai do grupo do cargo anterior e entra no do novo. Os demais grupos que o administrador deu
     * continuam.
     */
    public void trocarGrupo(Usuario usuario, String anterior, String novo) {
        if (usuario == null || usuario.getKeycloakId() == null || anterior.equals(novo)) {
            return;
        }
        Set<String> grupos = new HashSet<>(identidade.grupos(usuario.getKeycloakId()));
        grupos.remove(anterior);
        grupos.add(novo);
        identidade.definirGrupos(usuario.getKeycloakId(), grupos);
    }

    /**
     * Grupos escolhidos no cadastro do funcionário: o do cargo + os extras, no lugar dos atuais.
     * Quem chama já verificou a permissão USUARIO_GERENCIAR.
     */
    public void definirGrupos(Usuario usuario, String grupoDoCargo, Set<String> extras) {
        if (usuario == null || usuario.getKeycloakId() == null) {
            return;
        }
        identidade.definirGrupos(usuario.getKeycloakId(), comGrupoDoCargo(grupoDoCargo, extras));
    }

    /** O grupo do cargo (ou MEDICO) + os extras informados. */
    public static Set<String> comGrupoDoCargo(String grupoDoCargo, Set<String> extras) {
        Set<String> grupos = new HashSet<>();
        if (extras != null) {
            grupos.addAll(extras);
        }
        grupos.add(grupoDoCargo);
        return grupos;
    }

    /** Ativa/desativa o acesso, aqui e no Keycloak (desativado não consegue fazer login). */
    public void definirAtivo(Usuario usuario, boolean ativo) {
        if (usuario.isAtivo() == ativo) {
            return;
        }
        usuario.setAtivo(ativo);
        if (usuario.getKeycloakId() != null) {
            identidade.definirAtivo(usuario.getKeycloakId(), ativo);
        }
    }

    /** Remove o usuário do Keycloak depois do commit (o vínculo local sai em cascata com a pessoa). */
    public void removerDepoisDoCommit(Usuario usuario) {
        if (usuario == null || usuario.getKeycloakId() == null) {
            return;
        }
        String keycloakId = usuario.getKeycloakId();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            identidade.removerUsuario(keycloakId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    identidade.removerUsuario(keycloakId);
                } catch (RuntimeException e) {
                    // O cadastro já foi removido: o usuário fica órfão no Keycloak e é apagado à mão
                    log.error("Usuário {} removido do cadastro, mas não do Keycloak: {}", keycloakId, e.getMessage());
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return repository.findAll(pageable).map(UsuarioResponse::de);
    }

    /** Detalhe com os grupos atuais do Keycloak. */
    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        Usuario usuario = buscarEntidade(id);
        return UsuarioResponse.de(usuario, grupos(usuario));
    }

    /** Ativa/desativa o acesso. O administrador não pode desativar o próprio usuário (evita se trancar fora). */
    @Transactional
    public UsuarioResponse alterarStatus(Long id, boolean ativo, String loginAutenticado) {
        Usuario usuario = buscarEntidade(id);
        if (!ativo && usuario.getLogin().equals(loginAutenticado)) {
            throw new BusinessRuleException("Não é possível desativar o próprio usuário");
        }
        definirAtivo(usuario, ativo);
        log.info("Usuário {} {}", id, ativo ? "ativado" : "desativado");
        return UsuarioResponse.de(repository.save(usuario));
    }

    /** Nomes dos grupos do usuário no Keycloak (vazio se ainda não está lá). */
    public List<String> grupos(Usuario usuario) {
        return usuario == null || usuario.getKeycloakId() == null ? List.of() : identidade.grupos(usuario.getKeycloakId());
    }

    /** Registro antigo (sem keycloakId) que recebeu senha: cria no Keycloak e guarda o vínculo. */
    private void vincularAoKeycloak(PossuiUsuario pessoa, Usuario usuario, String senha, Set<String> grupos) {
        String keycloakId = identidade.criarUsuario(new ProvedorIdentidade.NovoUsuario(
                pessoa.getCpf(), senha, pessoa.getNome(), pessoa.getEmail(), usuario.isAtivo(), grupos));
        desfazerSeHouverRollback(keycloakId);
        usuario.setKeycloakId(keycloakId);
        log.info("Usuário {} vinculado ao Keycloak", usuario.getId());
    }

    /** Compensação: se a transação do banco for desfeita, o usuário recém-criado no Keycloak também sai. */
    private void desfazerSeHouverRollback(String keycloakId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    log.warn("Transação desfeita: removendo do Keycloak o usuário {} recém-criado", keycloakId);
                    try {
                        identidade.removerUsuario(keycloakId);
                    } catch (RuntimeException e) {
                        log.error("Não foi possível remover do Keycloak o usuário {}: {}", keycloakId, e.getMessage());
                    }
                }
            }
        });
    }

    private Usuario buscarEntidade(Long id) {
        return repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Usuário", id));
    }
}
