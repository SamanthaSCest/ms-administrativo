package br.edu.imepac.clinicalapi.administrativo.support;

import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import br.edu.imepac.clinicalapi.administrativo.entities.Permissao;
import br.edu.imepac.clinicalapi.administrativo.repositories.GrupoRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Usuário de teste com as permissões reais de um grupo, por requisição:
 * {@code mockMvc.perform(get(...).with(Acessos.grupo("dr", "MEDICO")))}.
 * Substitui o {@code user(...).roles(...)} desde o RBAC.
 */
public final class Acessos {

    public static final String SERVICO = "SERVICO";

    private Acessos() {
    }

    public static RequestPostProcessor grupo(String username, String grupo) {
        return request -> {
            GrupoRepository repository = WebApplicationContextUtils
                    .getRequiredWebApplicationContext(request.getServletContext()).getBean(GrupoRepository.class);
            return user(username).authorities(authorities(repository, grupo)).postProcessRequest(request);
        };
    }

    static Collection<GrantedAuthority> authorities(GrupoRepository repository, String grupo) {
        Stream<String> nomes;
        if (SERVICO.equals(grupo)) {
            nomes = Stream.concat(Stream.of("ROLE_SERVICO"), Permissao.DO_SERVICO.stream().map(Enum::name));
        } else {
            Grupo encontrado = repository.findByNome(grupo)
                    .orElseThrow(() -> new IllegalArgumentException("Grupo não existe no banco de teste: " + grupo));
            nomes = encontrado.getPermissoes().stream().map(Enum::name);
        }
        List<GrantedAuthority> lista = nomes.map(n -> (GrantedAuthority) new SimpleGrantedAuthority(n)).toList();
        return lista;
    }
}
