package br.com.heracles.heracles_api.security;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-para-hs256";

    private SecretKeySpec chave(String segredo) {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private JwtService servico() {
        return new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(chave(SEGREDO))), Duration.ofHours(8));
    }

    private Usuario professor() {
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        usuario.setNome("Joao Treinador");
        usuario.setEmail("joao@heracles.com.br");
        usuario.setTipoPerfil(TipoPerfil.PROFESSOR);
        return usuario;
    }

    @Test
    @DisplayName("O token carrega identidade e perfil, e e validado pela mesma chave")
    void tokenCarregaIdentidadeEPerfil() {
        String token = servico().gerarToken(professor());

        Jwt decodificado = NimbusJwtDecoder.withSecretKey(chave(SEGREDO))
                .macAlgorithm(MacAlgorithm.HS256).build().decode(token);

        assertThat(decodificado.getSubject()).isEqualTo("joao@heracles.com.br");
        assertThat(decodificado.getClaimAsString("nome")).isEqualTo("Joao Treinador");
        assertThat(decodificado.getClaimAsStringList("roles")).containsExactly("PROFESSOR");
        assertThat(decodificado.getExpiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    @DisplayName("Token assinado com outra chave e rejeitado")
    void tokenComChaveDiferenteEhRejeitado() {
        String token = servico().gerarToken(professor());

        JwtDecoder decoderIntruso = NimbusJwtDecoder
                .withSecretKey(chave("outro-segredo-igualmente-longo-para-hs256!!"))
                .macAlgorithm(MacAlgorithm.HS256).build();

        assertThatThrownBy(() -> decoderIntruso.decode(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("A duracao configurada e refletida em expiraEmSegundos")
    void expiracaoConfiguravel() {
        assertThat(servico().expiracaoEmSegundos()).isEqualTo(8 * 60 * 60);
    }
}
