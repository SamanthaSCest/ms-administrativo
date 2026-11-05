package br.edu.imepac.clinicalapi.administrativo.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Bean
    public RSAPublicKey jwtPublicKey(
            @Value("${JWT_CHAVE_PUBLICA:/run/secrets/jwt-public.pem}") String caminho)
            throws Exception {

        String pem = lerArquivo(caminho)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] der = Base64.getDecoder().decode(pem);

        return (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(der));
    }

    @Bean
    public RSAPrivateKey jwtPrivateKey(
            @Value("${JWT_CHAVE_PRIVADA:/run/secrets/jwt-private.pem}") String caminho)
            throws Exception {

        String pem = lerArquivo(caminho)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] der = Base64.getDecoder().decode(pem);

        return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey jwtPublicKey) {
        return NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(
            RSAPublicKey jwtPublicKey,
            RSAPrivateKey jwtPrivateKey) {

        RSAKey chave = new RSAKey.Builder(jwtPublicKey)
                .privateKey(jwtPrivateKey)
                .keyID("ms-administrativo")
                .build();

        return new NimbusJwtEncoder(
                new ImmutableJWKSet<>(new JWKSet(chave)));
    }

    private String lerArquivo(String caminho) throws Exception {
        Path arquivo = Path.of(caminho);

        if (!Files.isRegularFile(arquivo)) {
            throw new IllegalStateException(
                    "Arquivo de chave JWT não encontrado: "
                            + arquivo.toAbsolutePath());
        }

        return Files.readString(arquivo, StandardCharsets.UTF_8);
    }
}
