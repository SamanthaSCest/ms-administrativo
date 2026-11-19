package br.edu.imepac.clinicalapi.administrativo.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Executa o teste autenticado com as permissões reais de um grupo de acesso (lidas do banco, como a migration
 * V14 as criou). {@code "SERVICO"} simula outro microserviço (X-Api-Key): ROLE_SERVICO + Permissao.DO_SERVICO.
 * Substitui o {@code @WithMockUser(roles = ...)} desde o RBAC.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = ComGrupoSecurityContextFactory.class)
public @interface ComGrupo {

    /** Nome do grupo (ex.: ADMINISTRADOR, RECEPCIONISTA, MEDICO) ou SERVICO. */
    String value();

    String username() default "usuario";
}
