package br.com.heracles.heracles_api.security;

import br.com.heracles.heracles_api.core.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emissao dos tokens de acesso.
 *
 * A validacao fica com o resource server do Spring Security, configurado
 * com o mesmo segredo em SecurityConfig — nao ha verificacao manual de
 * assinatura espalhada pelo codigo.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final Duration expiracao;

    public JwtService(JwtEncoder encoder,
                      @Value("${heracles.security.jwt.expiracao}") Duration expiracao) {
        this.encoder = encoder;
        this.expiracao = expiracao;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("heracles-api")
                .issuedAt(agora)
                .expiresAt(agora.plus(expiracao))
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                // O resource server converte "roles" em authorities ROLE_*.
                .claim("roles", java.util.List.of(usuario.getTipoPerfil().name()))
                .build();

        // O header precisa declarar HS256 explicitamente: com uma chave simetrica
        // o Nimbus nao consegue inferir o algoritmo e falha com
        // "Failed to select a JWK signing key".
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiracaoEmSegundos() {
        return expiracao.toSeconds();
    }
}
