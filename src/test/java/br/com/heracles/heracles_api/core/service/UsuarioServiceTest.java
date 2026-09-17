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

    /** Registra na base quem esta criando — o autor sai do token. */
    private String autor(TipoPerfil perfil) {
        Usuario quemCria = new Usuario();
        quemCria.setId(99L);
        quemCria.setEmail(perfil.name().toLowerCase() + "@heracles.com.br");
        quemCria.setTipoPerfil(perfil);
        given(repository.findByEmailIgnoreCase(quemCria.getEmail())).willReturn(Optional.of(quemCria));
        return quemCria.getEmail();
    }

    private UsuarioRequests.Criar cadastro(String cpf) {
        return new UsuarioRequests.Criar("Maria Silva", cpf, "maria@email.com",
                "(11) 99999-9999", TipoPerfil.ALUNO, "SenhaForte123");
    }

    @Test
    @DisplayName("A senha e cifrada com BCrypt antes de persistir")
    void senhaEhCifrada() {
        given(repository.save(any())).willAnswer(invocacao -> invocacao.getArgument(0));

        servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA));

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

        servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        // Sem isso, "123.456.789-01" e "12345678901" passariam os dois pela UNIQUE.
        assertThat(captor.getValue().getCpf()).isEqualTo("12345678901");
    }

    @Test
    @DisplayName("CPF ja cadastrado vira conflito de regra de negocio")
    void cpfDuplicado() {
        given(repository.existsByCpf("12345678901")).willReturn(true);

        String secretaria = autor(TipoPerfil.SECRETARIA);
        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01"), secretaria))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    @DisplayName("Cadastro de aluno e da secretaria — nem o admin faz")
    void alunoSoPelaSecretaria() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));

        // Matricular e da recepcao: o cadastro do aluno acompanha a
        // matricula, e quem recebe o aluno no balcao tem os documentos.
        assertThat(servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA)).nome())
                .isEqualTo("Maria Silva");

        String admin = autor(TipoPerfil.ADMIN);
        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01"), admin))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("aluno e da secretaria");
    }

    @Test
    @DisplayName("A secretaria nao cria um ADMIN — era escalacao de privilegio")
    void secretariaNaoCriaAdmin() {
        // tipoPerfil vem do corpo da requisicao. Sem esta regra bastava a
        // secretaria mandar "ADMIN" para criar uma conta de administrador,
        // entrar com ela e cadastrar o que quisesse.
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar virandoAdmin = new UsuarioRequests.Criar(
                "Invasor", "123.456.789-01", "invasor@email.com",
                null, TipoPerfil.ADMIN, "SenhaForte123");

        assertThatThrownBy(() -> servico().criar(virandoAdmin, secretaria))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("administracao");
    }

    @Test
    @DisplayName("Professor e secretaria sao cadastrados pela administracao")
    void equipeEhCadastradaPeloAdmin() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
        String admin = autor(TipoPerfil.ADMIN);

        for (TipoPerfil perfil : List.of(TipoPerfil.PROFESSOR, TipoPerfil.SECRETARIA, TipoPerfil.ADMIN)) {
            UsuarioRequests.Criar pedido = new UsuarioRequests.Criar(
                    "Fulano", "123.456.789-01", perfil + "@email.com",
                    null, perfil, "SenhaForte123");
            assertThat(servico().criar(pedido, admin).tipoPerfil()).isEqualTo(perfil);
        }
    }

    @Test
    @DisplayName("Professor nao cadastra ninguem, nem aluno")
    void professorNaoCadastra() {
        String professor = autor(TipoPerfil.PROFESSOR);

        // A rota ja recusa o professor, mas a regra nao depende disso:
        // duas barreiras, e a de dentro nao presume a de fora.
        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01"), professor))
                .isInstanceOf(RegraNegocioException.class);
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
