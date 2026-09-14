package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.AuthDtos;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AutenticacaoService(AuthenticationManager authenticationManager,
                               UsuarioRepository usuarioRepository,
                               JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse autenticar(AuthDtos.LoginRequest request) {
        // Delega a comparacao ao AuthenticationManager, que usa o BCryptPasswordEncoder.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.senha()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais invalidas"));

        return new AuthDtos.LoginResponse(
                jwtService.gerarToken(usuario),
                "Bearer",
                jwtService.expiracaoEmSegundos(),
                new AuthDtos.UsuarioAutenticado(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getTipoPerfil())
        );
    }
}
