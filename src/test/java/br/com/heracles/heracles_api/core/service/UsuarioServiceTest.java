package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private TreinoRepository treinoRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService servico() {
        return new UsuarioService(repository, treinoRepository, passwordEncoder);
    }

    private UsuarioRequests.Criar cadastro(String cpf) {
        return new UsuarioRequests.Criar("Maria Silva", cpf, "maria@email.com",
                "(11) 99999-9999", TipoPerfil.ALUNO, "SenhaForte123");
    }

    @Test
    @DisplayName("A senha e cifrada com BCrypt antes de persistir")
    void senhaEhCifrada() {
        given(repository.save(any())).willAnswer(invocacao -> invocacao.getArgument(0));

        servico().criar(cadastro("123.456.789-01"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        Usuario salvo = captor.getValue();

        // O texto puro nunca chega ao banco.
        assertThat(salvo.getSenhaHash()).isNotEqualTo("SenhaForte123");
        assertThat(salvo.getSenhaHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("SenhaForte123", salvo.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("O CPF e normalizado para apenas digitos")
    void cpfEhNormalizado() {
        given(repository.save(any())).willAnswer(invocacao -> invocacao.getArgument(0));

        servico().criar(cadastro("123.456.789-01"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        // Sem isso, "123.456.789-01" e "12345678901" passariam os dois pela UNIQUE.
        assertThat(captor.getValue().getCpf()).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("CPF ja cadastrado vira conflito de regra de negocio")
    void cpfDuplicado() {
        given(repository.existsByCpf("12345678901")).willReturn(true);

        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    @DisplayName("Alternar status inverte ATIVO e INATIVO")
    void alternarStatus() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.setStatus(StatusUsuario.ATIVO);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));

        assertThat(servico().alternarStatus(1L).status()).isEqualTo(StatusUsuario.INATIVO);
        assertThat(servico().alternarStatus(1L).status()).isEqualTo(StatusUsuario.ATIVO);
    }

    @Test
    @DisplayName("Vincular treino inexistente falha em vez de gravar vinculo parcial")
    void treinoInexistenteNaoVinculaParcialmente() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));

        Treino existente = new Treino();
        existente.setId(5L);
        // O cliente pediu dois treinos; o repositorio so encontrou um.
        given(treinoRepository.findAllById(List.of(5L, 999L))).willReturn(List.of(existente));

        assertThatThrownBy(() -> servico().sincronizarTreinos(1L, List.of(5L, 999L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
