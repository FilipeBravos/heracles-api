package br.com.heracles.heracles_api.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Politica de acesso da API.
 *
 * Antes: anyRequest().permitAll(), com um segredo de JWT no properties que
 * nenhuma classe lia. Agora o token existe de fato, toda rota exige
 * autenticacao fora o login, e a autorizacao e por TipoPerfil.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String segredoJwt;
    private final List<String> origensPermitidas;

    public SecurityConfig(
            @Value("${heracles.security.jwt.secret}") String segredoJwt,
            @Value("${heracles.cors.allowed-origins}") List<String> origensPermitidas) {

        if (segredoJwt == null || segredoJwt.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "heracles.security.jwt.secret precisa ter no minimo 32 bytes para assinatura HS256.");
        }
        this.segredoJwt = segredoJwt;
        this.origensPermitidas = origensPermitidas;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF nao se aplica: a API e stateless e autentica por bearer token,
                // nunca por cookie de sessao enviado automaticamente pelo navegador.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Cadastro e manutencao de alunos: recepcao e administracao.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/**").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/treinos")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")

                        // Prescricao de treino: professor e administracao.
                        .requestMatchers(HttpMethod.GET, "/api/treinos/**").authenticated()
                        .requestMatchers("/api/treinos/**").hasAnyRole("ADMIN", "PROFESSOR")

                        // Estrutura da rede: apenas administracao escreve.
                        .requestMatchers(HttpMethod.GET, "/api/unidades/**").authenticated()
                        .requestMatchers("/api/unidades/**").hasRole("ADMIN")

                        // Loja: a recepcao vende; o cadastro de produto e o
                        // ajuste de estoque sao da administracao.
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**")
                            .hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers("/api/produtos/**").hasRole("ADMIN")
                        .requestMatchers("/api/vendas/**").hasAnyRole("ADMIN", "SECRETARIA")

                        // Equipamentos: quem esta no salao abre chamado — o
                        // professor e quem costuma ver o aparelho quebrar.
                        // Cadastrar e dar baixa no reparo ficam com a administracao.
                        .requestMatchers(HttpMethod.GET, "/api/equipamentos/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        .requestMatchers(HttpMethod.POST, "/api/equipamentos/*/chamados")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        .requestMatchers("/api/equipamentos/**").hasRole("ADMIN")

                        .requestMatchers("/api/dashboard/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")

                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    /**
     * Origens permitidas vem de configuracao, nao de @CrossOrigin espalhado
     * pelos controllers — onde um deles (UnidadeController) havia ficado de fora.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveHmac()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chaveHmac())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Converte a claim "roles" do token em authorities ROLE_*. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private SecretKeySpec chaveHmac() {
        return new SecretKeySpec(segredoJwt.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
