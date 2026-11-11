package br.edu.imepac.clinicalapi.administrativo.dto.auth;

import br.edu.imepac.clinicalapi.administrativo.dto.UsuarioResponse;

/**
 * Resposta do login: token JWT a ser enviado em {@code Authorization: Bearer <accessToken>}.
 *
 * @param expiresIn validade do token, em segundos
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) {
}
