package br.com.heracles.heracles_api.core.repository;

import br.com.heracles.heracles_api.core.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
