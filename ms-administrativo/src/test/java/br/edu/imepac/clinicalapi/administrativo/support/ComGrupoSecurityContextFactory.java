package br.edu.imepac.clinicalapi.administrativo.support;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

/** Monta o contexto de segurança do {@link ComGrupo} com as roles do grupo em keycloak/realm-clinical.json. */
public class ComGrupoSecurityContextFactory implements WithSecurityContextFactory<ComGrupo> {

    @Override
    public SecurityContext createSecurityContext(ComGrupo anotacao) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(Acessos.token(anotacao.username(), anotacao.value()));
        return contexto;
    }
}
