package br.com.bussola.security;

import java.time.Clock;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {
    private final NimbusJwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final Clock clock;

    public JwtTokenProvider(@Value("${bussola.jwt.secret}") String secret, Clock clock) {
        byte[] chave = Base64.getDecoder().decode(secret);
        if (chave.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes aleatórios em Base64.");
        }
        var key = new SecretKeySpec(chave, "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key).build();
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("bussola"));
        this.clock = clock;
    }

    public String gerar(Long usuarioId, String sessaoId) {
        // A validade por inatividade é conferida na sessão persistida a cada uso.
        var claims = JwtClaimsSet.builder().issuer("bussola").subject(usuarioId.toString())
                .id(sessaoId).issuedAt(clock.instant()).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt decodificar(String token) {
        return decoder.decode(token);
    }
}
