package br.com.heracles.heracles_api.config;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Cria um administrador inicial em desenvolvimento.
 *
 * Existe porque fechar a API (antes era permitAll) deixaria uma base recem
 * migrada sem nenhuma credencial para entrar. Fica restrito ao perfil "local"
 * e a uma propriedade explicita, entao nunca semeia senha conhecida em um
 * ambiente real — em producao o primeiro admin e criado por operacao.
 */
@Configuration
@Profile("local")
@ConditionalOnProperty(name = "heracles.seed.admin.enabled", havingValue = "true")
public class SeedAdminLocal {

    private static final Logger log = LoggerFactory.getLogger(SeedAdminLocal.class);

    @Bean
    public ApplicationRunner criarAdminInicial(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${heracles.seed.admin.email}") String email,
            @Value("${heracles.seed.admin.senha}") String senha) {

        return args -> {
            if (repository.existsByEmailIgnoreCase(email)) {
                return;
            }

            Usuario admin = new Usuario();
            admin.setNome("Administrador Heracles");
            admin.setEmail(email);
            admin.setCpf("00000000000");
            admin.setTelefone("(00) 00000-0000");
            admin.setTipoPerfil(TipoPerfil.ADMIN);
            admin.setStatus(StatusUsuario.ATIVO);
            admin.setSenhaHash(passwordEncoder.encode(senha));

            repository.save(admin);
            log.warn("Administrador de desenvolvimento criado: {} (perfil local apenas)", email);
        };
    }
}
