package br.edu.imepac.clinicalapi.administrativo.support;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Executa o teste autenticado como se o token viesse do Keycloak, com as roles reais de um grupo
 * (keycloak/realm-clinical.json). {@code "SERVICO"} simula outro microserviço: as roles da service account do
 * ms-agendamento (client credentials).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@WithSecurityContext(factory = ComGrupoSecurityContextFactory.class)
public @interface ComGrupo {

    /** Nome do grupo do realm (ex.: ADMINISTRADOR, RECEPCIONISTA, MEDICO) ou SERVICO. */
    String value();

    String username() default "usuario";
}
