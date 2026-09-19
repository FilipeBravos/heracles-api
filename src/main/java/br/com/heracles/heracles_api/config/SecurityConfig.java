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

                        // Avaliacao fisica e trabalho de professor, nao da
                        // secretaria — diferente do cadastro e da anamnese,
                        // que ela tambem preenche.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/*/avaliacoes-fisicas")
                            .hasAnyRole("ADMIN", "PROFESSOR")
                        // Cadastro e manutencao de alunos: recepcao e administracao.
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/**").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/treinos")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        // Anamnese: quem monta a ficha (professor) precisa poder
                        // preenche-la tambem, sem ganhar escrita geral em usuarios.
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/anamnese")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasAnyRole("ADMIN", "SECRETARIA")
                        // Nao ha regra de DELETE em /api/usuarios porque nao
                        // ha DELETE: aluno nao se apaga, se inativa
                        // (PUT /{id}/status). Apagar quebraria o historico que
                        // aponta para ele — assinaturas, vendas, fichas. A
                        // regra existia e devolvia 405, descrevendo um poder
                        // que ninguem tem.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")

                        // A area do proprio usuario: qualquer autenticado le,
                        // porque o que ele le e dele. Nenhuma rota sob /api/eu
                        // aceita id de aluno — o sujeito sai do token, entao
                        // nao ha o que um perfil possa pedir de outro.
                        //
                        // E a unica rota que o perfil ALUNO alcanca.
                        .requestMatchers("/api/eu/**").authenticated()

                        // Prescricao de treino: professor e administracao.
                        //
                        // A leitura era authenticated(), o que dava ao aluno o
                        // catalogo de modelos de treino da rede inteira.
                        // core.treinos e catalogo compartilhado, nao ficha
                        // pessoal — nao ha nada ali que seja "o treino dele".
                        // Quem le e quem monta a ficha (professor, admin) e
                        // quem vincula ficha pronta ao aluno (secretaria).
                        .requestMatchers(HttpMethod.GET, "/api/treinos/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        .requestMatchers("/api/treinos/**").hasAnyRole("ADMIN", "PROFESSOR")

                        // Estrutura da rede: apenas administracao escreve.
                        //
                        // A leitura tambem era authenticated(), expondo ao
                        // aluno endereco e telefone de todas as unidades. Os
                        // formularios de produto, equipamento, plano e
                        // matricula precisam da lista; o aluno, nao.
                        .requestMatchers(HttpMethod.GET, "/api/unidades/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
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

                        // Matriculas: a recepcao matricula, renova e cobra; o
                        // cadastro de plano e a tabela de precos ficam com a
                        // administracao. Cancelar tambem: e irreversivel —
                        // o aluno precisa ser matriculado de novo.
                        .requestMatchers(HttpMethod.GET, "/api/planos/**")
                            .hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers("/api/planos/**").hasRole("ADMIN")

                        // A conferencia de acesso e pergunta de catraca: quem
                        // esta recebendo o aluno precisa poder responde-la.
                        .requestMatchers(HttpMethod.GET, "/api/assinaturas/acesso")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        // Historico de frequencia: mesmo publico da tela de Alunos,
                        // que e onde ele aparece.
                        .requestMatchers(HttpMethod.GET, "/api/assinaturas/checkins/**")
                            .hasAnyRole("ADMIN", "SECRETARIA", "PROFESSOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/assinaturas/**").hasRole("ADMIN")
                        .requestMatchers("/api/assinaturas/**").hasAnyRole("ADMIN", "SECRETARIA")

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
