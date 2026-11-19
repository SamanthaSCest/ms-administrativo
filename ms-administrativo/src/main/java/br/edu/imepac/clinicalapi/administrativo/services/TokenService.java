package br.edu.imepac.clinicalapi.administrativo.services;

import br.edu.imepac.clinicalapi.administrativo.config.SecurityConfig;
import br.edu.imepac.clinicalapi.administrativo.config.SegurancaProperties;
import br.edu.imepac.clinicalapi.administrativo.entities.Grupo;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emite o JWT de acesso. Claims: iss (este serviço), sub (login), uid (id do usuário), grupos (nomes, informativo),
 * permissoes (efetivas: grupos + avulsas; viram as authorities), iat e exp.
 * Mudanças de grupo/permissão valem a partir do próximo login (o token já emitido carrega as anteriores).
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final SegurancaProperties props;

    public TokenService(JwtEncoder encoder, SegurancaProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().emissor())
                .subject(usuario.getLogin())
                .issuedAt(agora)
                .expiresAt(agora.plus(validade()))
                .claim(SecurityConfig.CLAIM_USUARIO_ID, usuario.getId())
                .claim(SecurityConfig.CLAIM_GRUPOS, usuario.getGrupos().stream().map(Grupo::getNome).sorted().toList())
                .claim(SecurityConfig.CLAIM_PERMISSOES, usuario.permissoesEfetivas().stream().map(Enum::name).toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Duration validade() {
        return props.jwt().expiracao();
    }
}
