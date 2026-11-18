package br.edu.imepac.clinicalapi.administrativo.support;

import br.edu.imepac.clinicalapi.administrativo.repositories.GrupoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/** Monta o contexto de segurança do {@link ComGrupo} com as permissões do grupo no banco. */
public class ComGrupoSecurityContextFactory implements WithSecurityContextFactory<ComGrupo> {

    @Autowired
    private GrupoRepository grupoRepository;

    @Override
    public SecurityContext createSecurityContext(ComGrupo anotacao) {
        var authorities = Acessos.authorities(grupoRepository, anotacao.value());
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                anotacao.username(), "senha", authorities));
        return contexto;
    }
}
