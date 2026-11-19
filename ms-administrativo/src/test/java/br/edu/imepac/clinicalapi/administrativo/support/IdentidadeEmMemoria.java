package br.edu.imepac.clinicalapi.administrativo.support;

import br.edu.imepac.clinicalapi.administrativo.identidade.ProvedorIdentidade;
import br.edu.imepac.clinicalapi.common.exception.BusinessRuleException;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keycloak de mentira para os testes de integração (bean @Primary no lugar do KeycloakProvedorIdentidade):
 * guarda os usuários em memória, com a regra que importa aqui (grupo tem que existir no realm).
 * Os grupos válidos são os de keycloak/realm-clinical.json.
 */
@Component
@Primary
public class IdentidadeEmMemoria implements ProvedorIdentidade {

    private final Map<String, UsuarioFalso> usuarios = new ConcurrentHashMap<>();

    @Override
    public String criarUsuario(NovoUsuario novo) {
        validarGrupos(novo.grupos());
        // Os testes limpam o banco com deleteAll() (sem passar pelo serviço), então sobra usuário antigo aqui:
        // o mesmo login substitui o antigo. O 409 de login repetido é garantido antes, pelo banco (UsuarioService).
        usuarios.values().removeIf(u -> u.login.equals(novo.login()));
        String id = UUID.randomUUID().toString();
        usuarios.put(id, new UsuarioFalso(novo.login(), novo.senha(), novo.nome(), novo.email(), novo.ativo(),
                new HashSet<>(novo.grupos() == null ? Set.of() : novo.grupos())));
        return id;
    }

    @Override
    public void atualizarDados(String id, String login, String nome, String email) {
        UsuarioFalso u = obter(id);
        u.login = login;
        u.nome = nome;
        u.email = email;
    }

    @Override
    public void definirSenha(String id, String senha) {
        obter(id).senha = senha;
    }

    @Override
    public void definirAtivo(String id, boolean ativo) {
        obter(id).ativo = ativo;
    }

    @Override
    public List<String> grupos(String id) {
        return new TreeSet<>(obter(id).grupos).stream().toList();
    }

    @Override
    public void definirGrupos(String id, Set<String> grupos) {
        validarGrupos(grupos);
        UsuarioFalso u = obter(id);
        u.grupos.clear();
        u.grupos.addAll(grupos);
    }

    @Override
    public void removerUsuario(String id) {
        usuarios.remove(id);
    }

    @Override
    public List<String> listarGrupos() {
        return RealmClinical.grupos();
    }

    /** Para as asserções dos testes. */
    public Optional<UsuarioFalso> usuario(String id) {
        return Optional.ofNullable(id).map(usuarios::get);
    }

    public Optional<UsuarioFalso> usuarioPorLogin(String login) {
        return usuarios.values().stream().filter(u -> u.login.equals(login)).findFirst();
    }

    private UsuarioFalso obter(String id) {
        UsuarioFalso u = usuarios.get(id);
        if (u == null) {
            throw new BusinessRuleException("Keycloak recusou: User not found");
        }
        return u;
    }

    private static void validarGrupos(Set<String> grupos) {
        if (grupos == null) {
            return;
        }
        Set<String> faltando = new TreeSet<>(grupos);
        faltando.removeAll(RealmClinical.grupos());
        if (!faltando.isEmpty()) {
            throw new BusinessRuleException("Grupo(s) de acesso não encontrado(s): " + faltando);
        }
    }

    public static final class UsuarioFalso {
        public String login;
        public String senha;
        public String nome;
        public String email;
        public boolean ativo;
        public final Set<String> grupos;

        UsuarioFalso(String login, String senha, String nome, String email, boolean ativo, Set<String> grupos) {
            this.login = login;
            this.senha = senha;
            this.nome = nome;
            this.email = email;
            this.ativo = ativo;
            this.grupos = grupos;
        }
    }
}
