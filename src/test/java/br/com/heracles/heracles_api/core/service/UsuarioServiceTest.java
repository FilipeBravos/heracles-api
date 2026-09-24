package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.ContratoAssinado;
import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.AvaliacaoFisicaDtos;
import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import br.com.heracles.heracles_api.core.domain.AvaliacaoFisicaFoto;
import br.com.heracles.heracles_api.core.repository.AnamneseRepository;
import br.com.heracles.heracles_api.core.repository.AvaliacaoFisicaRepository;
import br.com.heracles.heracles_api.core.repository.ContratoAssinadoRepository;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.core.dto.LinhaReavaliacaoVencidaBruta;
import br.com.heracles.heracles_api.core.dto.LinhaAlunoAnamnese;
import br.com.heracles.heracles_api.core.dto.LinhaAvaliacaoParaEvolucao;
import br.com.heracles.heracles_api.core.dto.LinhaCoberturaAnamnesePorUnidade;
import br.com.heracles.heracles_api.core.dto.LinhaEvolucaoFisicaPorUnidade;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.dto.AlunoUnidade;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    private static final Long PLANO_ID = 10L;

    @Mock
    private UsuarioRepository repository;

    @Mock
    private TreinoRepository treinoRepository;

    @Mock
    private HistoricoTreinoAlunoRepository historicoTreinoRepository;

    @Mock
    private AnamneseRepository anamneseRepository;

    @Mock
    private AvaliacaoFisicaRepository avaliacaoFisicaRepository;

    @Mock
    private ContratoAssinadoRepository contratoAssinadoRepository;

    @Mock
    private PlanoRepository planoRepository;

    @Mock
    private AssinaturaRepository assinaturaRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** Por padrao o aluno ja tem anamnese: so os testes do proprio gate desligam isso. */
    @BeforeEach
    void alunoTemAnamnesePorPadrao() {
        given(anamneseRepository.existsByAlunoId(any())).willReturn(true);
        given(planoRepository.findById(PLANO_ID)).willReturn(Optional.of(planoAtivo(PLANO_ID)));
    }

    private UsuarioService servico() {
        return new UsuarioService(repository, treinoRepository, historicoTreinoRepository,
                anamneseRepository, avaliacaoFisicaRepository, contratoAssinadoRepository,
                planoRepository, assinaturaRepository, passwordEncoder);
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

    private Plano planoAtivo(Long id) {
        Plano plano = new Plano();
        plano.setId(id);
        plano.setNome("Plano Mensal");
        return plano;
    }

    private UsuarioRequests.Criar cadastro(String cpf) {
        return new UsuarioRequests.Criar("Maria Silva", cpf, "maria@email.com",
                "(11) 99999-9999", "Rua das Flores, 123", "01234-567",
                LocalDate.of(1990, 5, 20), null, null, PLANO_ID, TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);
    }

    @Test
    @DisplayName("A senha e cifrada com BCrypt antes de persistir")
    void senhaEhCifrada() {
        given(repository.save(any())).willAnswer(invocacao -> invocacao.getArgument(0));

        servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA), null);

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

        servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA), null);

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
        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01"), secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    @DisplayName("Cadastro de aluno e da secretaria — nem o admin faz")
    void alunoSoPelaSecretaria() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));

        // Matricular e da recepcao: o cadastro do aluno acompanha a
        // matricula, e quem recebe o aluno no balcao tem os documentos.
        assertThat(servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA), null).nome())
                .isEqualTo("Maria Silva");

        String admin = autor(TipoPerfil.ADMIN);
        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01"), admin, null))
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
                null, null, null, null, null, null, null, TipoPerfil.ADMIN, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(virandoAdmin, secretaria, null))
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
                    null, null, null, null, null, null, null, perfil, "SenhaForte123", "Maria Silva", true);
            assertThat(servico().criar(pedido, admin, null).tipoPerfil()).isEqualTo(perfil);
        }
    }

    @Test
    @DisplayName("Professor nao cadastra ninguem, nem aluno")
    void professorNaoCadastra() {
        String professor = autor(TipoPerfil.PROFESSOR);

        // A rota ja recusa o professor, mas a regra nao depende disso:
        // duas barreiras, e a de dentro nao presume a de fora.
        assertThatThrownBy(() -> servico().criar(cadastro("123.456.789-01"), professor, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Cadastro de aluno exige endereco")
    void cadastroDeAlunoExigeEndereco() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar semEndereco = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                null, "01234-567", LocalDate.of(1990, 5, 20), null, null, PLANO_ID,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(semEndereco, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("endereco");
    }

    @Test
    @DisplayName("Cadastro de aluno exige CEP")
    void cadastroDeAlunoExigeCep() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar semCep = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", null, LocalDate.of(1990, 5, 20), null, null, PLANO_ID,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(semCep, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("CEP");
    }

    @Test
    @DisplayName("Cadastro de aluno exige data de nascimento")
    void cadastroDeAlunoExigeDataNascimento() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar semNascimento = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", null, null, null, PLANO_ID,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(semNascimento, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("data de nascimento");
    }

    @Test
    @DisplayName("Cadastro de aluno exige plano escolhido")
    void cadastroDeAlunoExigePlano() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar semPlano = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20), null, null, null,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(semPlano, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("plano");
    }

    @Test
    @DisplayName("Cadastro de aluno exige nome digitado no contrato")
    void cadastroDeAlunoExigeAssinaturaContrato() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar semAssinatura = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20), null, null, PLANO_ID,
                TipoPerfil.ALUNO, "SenhaForte123", null, true);

        assertThatThrownBy(() -> servico().criar(semAssinatura, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("contrato");
    }

    @Test
    @DisplayName("Cadastro de aluno exige aceite do contrato, nao so o nome digitado")
    void cadastroDeAlunoExigeAceiteDoContrato() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar semAceite = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20), null, null, PLANO_ID,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", false);

        assertThatThrownBy(() -> servico().criar(semAceite, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("aceitar");
    }

    @Test
    @DisplayName("Cadastrar aluno assina o contrato com o nome digitado, o texto vigente e o IP de quem preencheu")
    void cadastroDeAlunoAssinaOContrato() {
        given(repository.save(any())).willAnswer(i -> {
            Usuario salvo = i.getArgument(0);
            salvo.setId(1L);
            return salvo;
        });
        given(contratoAssinadoRepository.save(any())).willAnswer(i -> i.getArgument(0));

        servico().criar(cadastro("123.456.789-01"), autor(TipoPerfil.SECRETARIA), "203.0.113.7");

        ArgumentCaptor<ContratoAssinado> captor = ArgumentCaptor.forClass(ContratoAssinado.class);
        verify(contratoAssinadoRepository).save(captor.capture());
        assertThat(captor.getValue().getNomeDigitado()).isEqualTo("Maria Silva");
        assertThat(captor.getValue().getTextoContrato()).isEqualTo(UsuarioService.TEXTO_CONTRATO_PADRAO);
        assertThat(captor.getValue().getIpOrigem()).isEqualTo("203.0.113.7");
        assertThat(captor.getValue().getAluno().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Cadastrar professor, secretaria ou admin nao assina contrato nenhum")
    void cadastroDeEquipeNaoAssinaContrato() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));

        servico().criar(new UsuarioRequests.Criar(
                "Fulano", "123.456.789-01", "fulano@email.com",
                null, null, null, null, null, null, null, TipoPerfil.PROFESSOR, "SenhaForte123", null, null),
                autor(TipoPerfil.ADMIN), null);

        verify(contratoAssinadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aluno sem contrato assinado (cadastro anterior a esta funcionalidade) devolve o estado ausente")
    void contratoAusenteParaCadastroAntigo() {
        given(repository.existsById(1L)).willReturn(true);
        given(contratoAssinadoRepository.findByAlunoId(1L)).willReturn(Optional.empty());

        assertThat(servico().buscarContrato(1L).assinado()).isFalse();
    }

    @Test
    @DisplayName("Plano inexistente e rejeitado")
    void planoInexistenteEhRejeitado() {
        given(planoRepository.findById(999L)).willReturn(Optional.empty());
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar comPlanoInexistente = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20), null, null, 999L,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(comPlanoInexistente, secretaria, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Plano fora de linha nao pode ser escolhido")
    void planoInativoNaoPodeSerEscolhido() {
        Plano inativo = planoAtivo(20L);
        inativo.setAtivo(false);
        given(planoRepository.findById(20L)).willReturn(Optional.of(inativo));

        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar comPlanoInativo = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20), null, null, 20L,
                TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(comPlanoInativo, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fora de linha");
    }

    @Test
    @DisplayName("Foto precisa vir com base64 e content-type juntos")
    void fotoExigeBase64EContentTypeJuntos() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar soComContentType = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20),
                null, "image/png", PLANO_ID, TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(soComContentType, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("juntos");
    }

    @Test
    @DisplayName("Foto em base64 invalido e rejeitada")
    void fotoInvalidaEmBase64EhRejeitada() {
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar fotoInvalida = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20),
                "isto-nao-e-base64!!!", "image/png", PLANO_ID, TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(fotoInvalida, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("invalido");
    }

    @Test
    @DisplayName("Foto acima de 3MB e rejeitada")
    void fotoAcimaDoLimiteEhRejeitada() {
        String base64Grande = Base64.getEncoder().encodeToString(new byte[3 * 1024 * 1024 + 1]);
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar fotoGrande = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20),
                base64Grande, "image/png", PLANO_ID, TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        assertThatThrownBy(() -> servico().criar(fotoGrande, secretaria, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("3 MB");
    }

    @Test
    @DisplayName("Foto valida e persistida com o tipo de conteudo")
    void fotoValidaEhPersistida() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
        String base64Valido = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        String secretaria = autor(TipoPerfil.SECRETARIA);
        UsuarioRequests.Criar comFoto = new UsuarioRequests.Criar(
                "Maria Silva", "123.456.789-01", "maria@email.com", "(11) 99999-9999",
                "Rua das Flores, 123", "01234-567", LocalDate.of(1990, 5, 20),
                base64Valido, "image/png", PLANO_ID, TipoPerfil.ALUNO, "SenhaForte123", "Maria Silva", true);

        servico().criar(comFoto, secretaria, null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getFoto()).containsExactly(1, 2, 3);
        assertThat(captor.getValue().getFotoContentType()).isEqualTo("image/png");
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

    @Test
    @DisplayName("Vincular ficha nova abre um periodo de historico")
    void vincularFichaAbreHistorico() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));

        Treino ficha = new Treino();
        ficha.setId(5L);
        ficha.setNome("Ficha A");
        ficha.setFoco("Hipertrofia");
        ficha.setNivel("Intermediario");
        given(treinoRepository.findAllById(List.of(5L))).willReturn(List.of(ficha));

        servico().sincronizarTreinos(1L, List.of(5L));

        ArgumentCaptor<HistoricoTreinoAluno> salvo = ArgumentCaptor.forClass(HistoricoTreinoAluno.class);
        verify(historicoTreinoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getTreinoNome()).isEqualTo("Ficha A");
        assertThat(salvo.getValue().getTreinoFoco()).isEqualTo("Hipertrofia");
        assertThat(salvo.getValue().getVinculadoEm()).isNotNull();
        // Ainda com o aluno: o periodo comeca aberto.
        assertThat(salvo.getValue().estaAberto()).isTrue();
    }

    @Test
    @DisplayName("Trocar a ficha fecha o periodo da antiga e abre o da nova")
    void trocarFichaFechaAAntigaEAbreANova() {
        Treino antiga = new Treino();
        antiga.setId(5L);
        antiga.setNome("Ficha A");

        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.getTreinos().add(antiga);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));

        Treino nova = new Treino();
        nova.setId(6L);
        nova.setNome("Ficha B");
        given(treinoRepository.findAllById(List.of(6L))).willReturn(List.of(nova));

        HistoricoTreinoAluno periodoAberto = new HistoricoTreinoAluno();
        given(historicoTreinoRepository.buscarAbertoPorAlunoETreino(1L, 5L))
                .willReturn(Optional.of(periodoAberto));

        servico().sincronizarTreinos(1L, List.of(6L));

        // A antiga foi fechada, nao apagada: e o registro de que o aluno a treinou.
        assertThat(periodoAberto.estaAberto()).isFalse();
        verify(historicoTreinoRepository).save(any());
    }

    @Test
    @DisplayName("Manter a mesma ficha nao mexe no historico")
    void manterAMesmaFichaNaoAbreNemFechaPeriodo() {
        Treino ficha = new Treino();
        ficha.setId(5L);

        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.getTreinos().add(ficha);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));
        given(treinoRepository.findAllById(List.of(5L))).willReturn(List.of(ficha));

        servico().sincronizarTreinos(1L, List.of(5L));

        verify(historicoTreinoRepository, never()).save(any());
        verify(historicoTreinoRepository, never()).buscarAbertoPorAlunoETreino(any(), any());
    }

    @Test
    @DisplayName("Sem anamnese preenchida, o aluno nao recebe ficha")
    void anamneseAusenteBloqueiaVincularFicha() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.setNome("Maria Silva");
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));
        given(anamneseRepository.existsByAlunoId(1L)).willReturn(false);

        assertThatThrownBy(() -> servico().sincronizarTreinos(1L, List.of(5L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("anamnese");

        verify(treinoRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Com anamnese preenchida, a ficha e vinculada normalmente")
    void anamnesePreenchidaPermiteVincularFicha() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));
        given(anamneseRepository.existsByAlunoId(1L)).willReturn(true);

        Treino ficha = new Treino();
        ficha.setId(5L);
        given(treinoRepository.findAllById(List.of(5L))).willReturn(List.of(ficha));

        assertThat(servico().sincronizarTreinos(1L, List.of(5L)).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Desvincular todas as fichas nao exige anamnese")
    void listaVaziaDesvinculaSemExigirAnamnese() {
        Treino ficha = new Treino();
        ficha.setId(5L);

        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.getTreinos().add(ficha);
        given(repository.findWithTreinosById(1L)).willReturn(Optional.of(aluno));
        given(anamneseRepository.existsByAlunoId(1L)).willReturn(false);
        given(treinoRepository.findAllById(List.of())).willReturn(List.of());

        assertThat(servico().sincronizarTreinos(1L, List.of()).treinos()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Avaliacao fisica
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Registrar avaliacao fisica calcula o IMC a partir de peso e altura")
    void registrarAvaliacaoFisicaCalculaImc() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        given(repository.findById(1L)).willReturn(Optional.of(aluno));
        given(avaliacaoFisicaRepository.save(any())).willAnswer(i -> i.getArgument(0));

        AvaliacaoFisicaDtos.Salvar request = new AvaliacaoFisicaDtos.Salvar(
                LocalDate.of(2026, 1, 10),
                new java.math.BigDecimal("80.0"), new java.math.BigDecimal("160.0"),
                null, null, null, null, null, null, null, null);

        AvaliacaoFisicaDtos.Response resposta = servico().registrarAvaliacaoFisica(1L, request);

        // 80 / 1.6^2 = 31.25 -> arredondado pra 31.3
        assertThat(resposta.imc()).isEqualByComparingTo("31.3");
        assertThat(resposta.data()).isEqualTo(LocalDate.of(2026, 1, 10));
    }

    @Test
    @DisplayName("Sem data informada, a avaliacao fisica vale hoje")
    void avaliacaoFisicaSemDataValeHoje() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        given(repository.findById(1L)).willReturn(Optional.of(aluno));
        given(avaliacaoFisicaRepository.save(any())).willAnswer(i -> i.getArgument(0));

        AvaliacaoFisicaDtos.Salvar request = new AvaliacaoFisicaDtos.Salvar(
                null, new java.math.BigDecimal("70"), new java.math.BigDecimal("170"),
                null, null, null, null, null, null, null, null);

        assertThat(servico().registrarAvaliacaoFisica(1L, request).data()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Registrar avaliacao fisica grava a circunferencia do peito e a galeria de fotos, na ordem enviada")
    void registrarAvaliacaoFisicaGravaPeitoEGaleria() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        given(repository.findById(1L)).willReturn(Optional.of(aluno));
        given(avaliacaoFisicaRepository.save(any())).willAnswer(i -> i.getArgument(0));

        AvaliacaoFisicaDtos.Salvar request = new AvaliacaoFisicaDtos.Salvar(
                null, new java.math.BigDecimal("70"), new java.math.BigDecimal("170"),
                null, null, null, null, null, new java.math.BigDecimal("95"), null,
                List.of(
                        new AvaliacaoFisicaDtos.Foto(Base64.getEncoder().encodeToString(new byte[]{1}), "image/png"),
                        new AvaliacaoFisicaDtos.Foto(Base64.getEncoder().encodeToString(new byte[]{2}), "image/jpeg")));

        AvaliacaoFisicaDtos.Response resposta = servico().registrarAvaliacaoFisica(1L, request);

        assertThat(resposta.circunferenciaPeito()).isEqualByComparingTo("95");
        assertThat(resposta.fotoIds()).hasSize(2);
    }

    @Test
    @DisplayName("Historico de avaliacao fisica de aluno inexistente e 404")
    void historicoAvaliacaoFisicaAlunoInexistente() {
        given(repository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> servico().historicoAvaliacoesFisicas(999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Foto de avaliacao de outro aluno nao e encontrada")
    void fotoDeAvaliacaoDeOutroAlunoNaoEhEncontrada() {
        Usuario outroAluno = new Usuario();
        outroAluno.setId(2L);

        AvaliacaoFisica avaliacao = new AvaliacaoFisica();
        avaliacao.setId(50L);
        avaliacao.setAluno(outroAluno);
        AvaliacaoFisicaFoto foto = new AvaliacaoFisicaFoto();
        foto.setId(500L);
        foto.setFoto(new byte[]{1, 2, 3});
        avaliacao.adicionarFoto(foto);
        given(avaliacaoFisicaRepository.findById(50L)).willReturn(Optional.of(avaliacao));

        // A avaliacao existe, mas e do aluno 2 — pedida pelo aluno 1, deve
        // dar 404 igual a se nao existisse, sem revelar que pertence a outro.
        assertThatThrownBy(() -> servico().buscarFotoAvaliacaoFisica(1L, 50L, 500L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Foto que nao existe na avaliacao e 404, mesmo sendo do aluno certo")
    void fotoInexistenteNaAvaliacaoNaoEhEncontrada() {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        AvaliacaoFisica avaliacao = new AvaliacaoFisica();
        avaliacao.setId(50L);
        avaliacao.setAluno(aluno);
        given(avaliacaoFisicaRepository.findById(50L)).willReturn(Optional.of(avaliacao));

        assertThatThrownBy(() -> servico().buscarFotoAvaliacaoFisica(1L, 50L, 999L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------------------------------------------------------------
    // Comparativo de avaliacoes fisicas
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Comparativo padrao e indisponivel com menos de duas avaliacoes")
    void comparativoIndisponivelComMenosDeDuasAvaliacoes() {
        given(repository.existsById(1L)).willReturn(true);
        AvaliacaoFisica unica = new AvaliacaoFisica();
        unica.setId(10L);
        given(avaliacaoFisicaRepository.findFirstByAlunoIdOrderByDataAsc(1L)).willReturn(Optional.of(unica));
        given(avaliacaoFisicaRepository.findFirstByAlunoIdOrderByDataDesc(1L)).willReturn(Optional.of(unica));

        assertThat(servico().compararAvaliacoesFisicas(1L, null, null).disponivel()).isFalse();
    }

    @Test
    @DisplayName("Comparativo padrao usa a primeira e a mais recente, com o delta certo")
    void comparativoPadraoUsaPrimeiraEMaisRecente() {
        given(repository.existsById(1L)).willReturn(true);

        AvaliacaoFisica primeira = new AvaliacaoFisica();
        primeira.setId(10L);
        primeira.setPesoKg(new java.math.BigDecimal("80"));
        primeira.setAlturaCm(new java.math.BigDecimal("170"));

        AvaliacaoFisica maisRecente = new AvaliacaoFisica();
        maisRecente.setId(20L);
        maisRecente.setPesoKg(new java.math.BigDecimal("75"));
        maisRecente.setAlturaCm(new java.math.BigDecimal("170"));

        given(avaliacaoFisicaRepository.findFirstByAlunoIdOrderByDataAsc(1L)).willReturn(Optional.of(primeira));
        given(avaliacaoFisicaRepository.findFirstByAlunoIdOrderByDataDesc(1L)).willReturn(Optional.of(maisRecente));

        AvaliacaoFisicaDtos.Comparativo comparativo = servico().compararAvaliacoesFisicas(1L, null, null);

        assertThat(comparativo.disponivel()).isTrue();
        assertThat(comparativo.de().id()).isEqualTo(10L);
        assertThat(comparativo.para().id()).isEqualTo(20L);
        assertThat(comparativo.delta().pesoKg()).isEqualByComparingTo("-5");
    }

    @Test
    @DisplayName("Comparativo com apenas um dos ids e erro de regra de negocio")
    void comparativoComApenasUmIdEErro() {
        given(repository.existsById(1L)).willReturn(true);

        assertThatThrownBy(() -> servico().compararAvaliacoesFisicas(1L, 10L, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Comparativo com avaliacao de outro aluno e 404")
    void comparativoComAvaliacaoDeOutroAlunoEhErro() {
        given(repository.existsById(1L)).willReturn(true);

        Usuario outroAluno = new Usuario();
        outroAluno.setId(2L);
        AvaliacaoFisica deOutroAluno = new AvaliacaoFisica();
        deOutroAluno.setId(30L);
        deOutroAluno.setAluno(outroAluno);
        given(avaliacaoFisicaRepository.findById(30L)).willReturn(Optional.of(deOutroAluno));

        assertThatThrownBy(() -> servico().compararAvaliacoesFisicas(1L, 30L, 31L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------------------------------------------------------------
    // Painel de aniversariantes do mes
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Aniversariantes do mes vem mapeados do repositorio, mes corrente")
    void aniversariantesDoMesMapeiaLinha() {
        Usuario aluno = new Usuario();
        aluno.setId(7L);
        aluno.setNome("Diego Ramos");
        aluno.setDataNascimento(LocalDate.of(1990, LocalDate.now().getMonthValue(), 12));
        aluno.setTelefone("11999990000");
        aluno.setEmail("diego@ex.com");
        given(repository.buscarAniversariantesDoMes(LocalDate.now().getMonthValue()))
                .willReturn(List.of(aluno));

        var linha = servico().aniversariantesDoMes().get(0);

        assertThat(linha.alunoId()).isEqualTo(7L);
        assertThat(linha.alunoNome()).isEqualTo("Diego Ramos");
        assertThat(linha.dataNascimento()).isEqualTo(LocalDate.of(1990, LocalDate.now().getMonthValue(), 12));
        assertThat(linha.telefone()).isEqualTo("11999990000");
        assertThat(linha.email()).isEqualTo("diego@ex.com");
    }

    // ---------------------------------------------------------------
    // Alerta de reavaliacao fisica vencida
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O resumo repassa a contagem do repositorio")
    void resumoReavaliacaoVencidaRepassaContagem() {
        given(repository.countReavaliacaoVencida(any())).willReturn(3L);

        assertThat(servico().resumoReavaliacaoVencida(90).total()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Dias sem avaliacao e calculado a partir da ultima avaliacao")
    void reavaliacaoVencidaCalculaDias() {
        LocalDate ultimaAvaliacao = LocalDate.now().minusDays(120);
        given(repository.buscarReavaliacaoVencida(any(), any())).willReturn(new PageImpl<>(List.of(
                new LinhaReavaliacaoVencidaBruta(7L, "Diego Ramos", "diego@ex.com", "11999990000", ultimaAvaliacao))));

        var linha = servico().reavaliacaoVencida(PageRequest.of(0, 20), 90).getContent().get(0);

        assertThat(linha.alunoId()).isEqualTo(7L);
        assertThat(linha.alunoNome()).isEqualTo("Diego Ramos");
        assertThat(linha.ultimaAvaliacao()).isEqualTo(ultimaAvaliacao);
        assertThat(linha.diasSemAvaliacao()).isEqualTo(120L);
    }

    @Test
    @DisplayName("Nunca avaliado sai com dias sem avaliacao nulo, nao zero")
    void reavaliacaoVencidaNuncaAvaliadoDiasNulo() {
        given(repository.buscarReavaliacaoVencida(any(), any())).willReturn(new PageImpl<>(List.of(
                new LinhaReavaliacaoVencidaBruta(8L, "Fabio Nunes", "fabio@ex.com", null, null))));

        var linha = servico().reavaliacaoVencida(PageRequest.of(0, 20), 90).getContent().get(0);

        assertThat(linha.ultimaAvaliacao()).isNull();
        assertThat(linha.diasSemAvaliacao()).isNull();
    }

    // ---------------------------------------------------------------
    // Evolucao fisica media por unidade
    // ---------------------------------------------------------------

    private LinhaAvaliacaoParaEvolucao avaliacaoEvolucao(Long alunoId, LocalDate data, String pesoKg, String alturaCm,
                                                          String percentualGordura) {
        return new LinhaAvaliacaoParaEvolucao(alunoId, data,
                pesoKg == null ? null : new java.math.BigDecimal(pesoKg),
                alturaCm == null ? null : new java.math.BigDecimal(alturaCm),
                percentualGordura == null ? null : new java.math.BigDecimal(percentualGordura));
    }

    @Test
    @DisplayName("Aluno com uma unica avaliacao no periodo nao entra na media — nao ha o que comparar")
    void evolucaoFisicaIgnoraAlunoComUmaSoAvaliacao() {
        given(avaliacaoFisicaRepository.avaliacoesParaEvolucaoDesde(any())).willReturn(List.of(
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(30), "80", "170", "20")));

        List<LinhaEvolucaoFisicaPorUnidade> resultado = servico().evolucaoFisicaMediaPorUnidade(365);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Calcula o delta medio de peso, percentual de gordura e IMC entre a primeira e a ultima avaliacao")
    void evolucaoFisicaCalculaDeltaMedio() {
        given(avaliacaoFisicaRepository.avaliacoesParaEvolucaoDesde(any())).willReturn(List.of(
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(180), "80", "170", "25"),
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(10), "75", "170", "20")));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of(
                new AlunoUnidade(1L, 5L, "Unidade Centro")));

        List<LinhaEvolucaoFisicaPorUnidade> resultado = servico().evolucaoFisicaMediaPorUnidade(365);

        assertThat(resultado).hasSize(1);
        LinhaEvolucaoFisicaPorUnidade linha = resultado.get(0);
        assertThat(linha.unidadeNome()).isEqualTo("Unidade Centro");
        assertThat(linha.quantidadeAlunos()).isEqualTo(1);
        assertThat(linha.deltaPesoMedio()).isEqualByComparingTo("-5.0");
        assertThat(linha.deltaPercentualGorduraMedio()).isEqualByComparingTo("-5.0");
        // IMC: 80/1.7^2=27.7 -> 75/1.7^2=26.0, delta = -1.7
        assertThat(linha.deltaImcMedio()).isEqualByComparingTo("-1.7");
    }

    @Test
    @DisplayName("Aluno sem assinatura vigente fica de fora do relatorio por unidade, mesmo com evolucao calculada")
    void evolucaoFisicaIgnoraAlunoSemAssinaturaVigente() {
        given(avaliacaoFisicaRepository.avaliacoesParaEvolucaoDesde(any())).willReturn(List.of(
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(180), "80", "170", "25"),
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(10), "75", "170", "20")));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of());

        List<LinhaEvolucaoFisicaPorUnidade> resultado = servico().evolucaoFisicaMediaPorUnidade(365);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Aluno de plano de rede conta a evolucao em cada unidade que o plano cobre")
    void evolucaoFisicaEspalhaAlunoDePlanoDeRedeEmCadaUnidade() {
        given(avaliacaoFisicaRepository.avaliacoesParaEvolucaoDesde(any())).willReturn(List.of(
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(180), "80", "170", "25"),
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(10), "75", "170", "20")));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of(
                new AlunoUnidade(1L, 5L, "Unidade Centro"),
                new AlunoUnidade(1L, 6L, "Unidade Norte")));

        List<LinhaEvolucaoFisicaPorUnidade> resultado = servico().evolucaoFisicaMediaPorUnidade(365);

        assertThat(resultado).extracting(LinhaEvolucaoFisicaPorUnidade::unidadeNome)
                .containsExactly("Unidade Centro", "Unidade Norte");
    }

    @Test
    @DisplayName("Delta ausente (falta peso numa das pontas) fica fora so daquela media, sem zerar o aluno")
    void evolucaoFisicaIgnoraDeltaAusenteNaMedia() {
        given(avaliacaoFisicaRepository.avaliacoesParaEvolucaoDesde(any())).willReturn(List.of(
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(180), null, "170", "25"),
                avaliacaoEvolucao(1L, LocalDate.now().minusDays(10), "75", "170", "20"),
                avaliacaoEvolucao(2L, LocalDate.now().minusDays(180), "80", "170", "25"),
                avaliacaoEvolucao(2L, LocalDate.now().minusDays(10), "70", "170", "20")));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of(
                new AlunoUnidade(1L, 5L, "Unidade Centro"),
                new AlunoUnidade(2L, 5L, "Unidade Centro")));

        List<LinhaEvolucaoFisicaPorUnidade> resultado = servico().evolucaoFisicaMediaPorUnidade(365);

        assertThat(resultado).hasSize(1);
        // So o aluno 2 tem delta de peso valido (-10); a media e sobre 1 valor, nao 2.
        assertThat(resultado.get(0).deltaPesoMedio()).isEqualByComparingTo("-10.0");
        assertThat(resultado.get(0).deltaPercentualGorduraMedio()).isEqualByComparingTo("-5.0");
    }

    // ---------------------------------------------------------------
    // Cobertura de anamnese por unidade
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Sem aluno com matricula vigente, o relatorio vem vazio")
    void coberturaAnamneseVaziaSemAlunos() {
        given(repository.buscarAlunosVigentesComAnamnese()).willReturn(List.of());

        assertThat(servico().coberturaAnamnesePorUnidade()).isEmpty();
    }

    @Test
    @DisplayName("Calcula o percentual de cobertura por unidade")
    void coberturaAnamneseCalculaPercentual() {
        given(repository.buscarAlunosVigentesComAnamnese()).willReturn(List.of(
                new LinhaAlunoAnamnese(1L, true),
                new LinhaAlunoAnamnese(2L, false)));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of(
                new AlunoUnidade(1L, 5L, "Unidade Centro"),
                new AlunoUnidade(2L, 5L, "Unidade Centro")));

        List<LinhaCoberturaAnamnesePorUnidade> resultado = servico().coberturaAnamnesePorUnidade();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).unidadeNome()).isEqualTo("Unidade Centro");
        assertThat(resultado.get(0).quantidadeAlunos()).isEqualTo(2);
        assertThat(resultado.get(0).quantidadeComAnamnese()).isEqualTo(1);
        assertThat(resultado.get(0).percentualCobertura()).isEqualByComparingTo("50.0");
    }

    @Test
    @DisplayName("Aluno sem assinatura vigente fica de fora do relatorio por unidade")
    void coberturaAnamneseIgnoraAlunoSemAssinaturaVigente() {
        given(repository.buscarAlunosVigentesComAnamnese()).willReturn(List.of(
                new LinhaAlunoAnamnese(1L, true)));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of());

        assertThat(servico().coberturaAnamnesePorUnidade()).isEmpty();
    }

    @Test
    @DisplayName("Aluno de plano de rede conta a cobertura em cada unidade que o plano cobre")
    void coberturaAnamneseEspalhaAlunoDePlanoDeRedeEmCadaUnidade() {
        given(repository.buscarAlunosVigentesComAnamnese()).willReturn(List.of(
                new LinhaAlunoAnamnese(1L, true)));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of(
                new AlunoUnidade(1L, 5L, "Unidade Centro"),
                new AlunoUnidade(1L, 6L, "Unidade Norte")));

        List<LinhaCoberturaAnamnesePorUnidade> resultado = servico().coberturaAnamnesePorUnidade();

        assertThat(resultado).extracting(LinhaCoberturaAnamnesePorUnidade::unidadeNome)
                .containsExactlyInAnyOrder("Unidade Centro", "Unidade Norte");
    }

    @Test
    @DisplayName("Ordena do pior pro melhor percentual de cobertura")
    void coberturaAnamneseOrdenaDoPiorProMelhor() {
        given(repository.buscarAlunosVigentesComAnamnese()).willReturn(List.of(
                new LinhaAlunoAnamnese(1L, true),
                new LinhaAlunoAnamnese(2L, true),
                new LinhaAlunoAnamnese(3L, false)));
        given(assinaturaRepository.buscarUnidadesVigentesPorAlunos(any())).willReturn(List.of(
                // Unidade Centro: 1 de 1 = 100%.
                new AlunoUnidade(1L, 5L, "Unidade Centro"),
                // Unidade Norte: 1 de 2 = 50%.
                new AlunoUnidade(2L, 6L, "Unidade Norte"),
                new AlunoUnidade(3L, 6L, "Unidade Norte")));

        List<LinhaCoberturaAnamnesePorUnidade> resultado = servico().coberturaAnamnesePorUnidade();

        assertThat(resultado).extracting(LinhaCoberturaAnamnesePorUnidade::unidadeNome)
                .containsExactly("Unidade Norte", "Unidade Centro");
    }
}
