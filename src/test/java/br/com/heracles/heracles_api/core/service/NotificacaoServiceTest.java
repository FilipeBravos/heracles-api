package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Notificacao;
import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.TipoNotificacao;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.NotificacaoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificacaoServiceTest {

    @Mock private NotificacaoRepository repository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AssinaturaRepository assinaturaRepository;

    private NotificacaoService service;

    private Usuario aluno;
    private Usuario secretaria;

    @BeforeEach
    void preparar() {
        service = new NotificacaoService(repository, usuarioRepository, assinaturaRepository);

        aluno = new Usuario();
        aluno.setId(1L);
        aluno.setNome("Marina Alves");
        aluno.setEmail("marina@ex.com");
        aluno.setTipoPerfil(TipoPerfil.ALUNO);
        aluno.setStatus(StatusUsuario.ATIVO);

        secretaria = new Usuario();
        secretaria.setId(2L);
        secretaria.setNome("Paula");
        secretaria.setEmail("paula@heracles.com.br");
        secretaria.setTipoPerfil(TipoPerfil.SECRETARIA);
        secretaria.setStatus(StatusUsuario.ATIVO);

        given(usuarioRepository.findByEmailIgnoreCase(aluno.getEmail())).willReturn(Optional.of(aluno));
        given(usuarioRepository.findByTipoPerfilAndStatus(TipoPerfil.SECRETARIA, StatusUsuario.ATIVO))
                .willReturn(List.of(secretaria));
    }

    // ---------------------------------------------------------------
    // Leitura e marcacao como lida
    // ---------------------------------------------------------------

    @Test
    @DisplayName("So o dono marca a propria notificacao como lida")
    void soDonoMarcaComoLida() {
        Usuario outroAluno = new Usuario();
        outroAluno.setId(99L);

        Notificacao deOutraPessoa = new Notificacao();
        deOutraPessoa.setId(5L);
        deOutraPessoa.setDestinatario(outroAluno);
        given(repository.findById(5L)).willReturn(Optional.of(deOutraPessoa));

        assertThatThrownBy(() -> service.marcarComoLida(aluno.getEmail(), 5L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O dono marca a propria notificacao como lida")
    void donoMarcaComoLida() {
        Notificacao minha = new Notificacao();
        minha.setId(5L);
        minha.setDestinatario(aluno);
        given(repository.findById(5L)).willReturn(Optional.of(minha));

        service.marcarComoLida(aluno.getEmail(), 5L);

        assertThat(minha.isLida()).isTrue();
    }

    // ---------------------------------------------------------------
    // Matricula vencendo
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Gera aviso de matricula vencendo pra cada assinatura na janela")
    void geraAvisoDeMatriculaVencendo() {
        Assinatura assinatura = assinaturaDe(LocalDate.now().plusDays(3));
        given(assinaturaRepository.buscarAtivasVencendoEntre(any(), any())).willReturn(List.of(assinatura));

        int criadas = service.gerarNotificacoesMatriculaVencendo(7);

        assertThat(criadas).isEqualTo(1);
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(aluno);
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoNotificacao.MATRICULA_VENCENDO);
        assertThat(captor.getValue().getMensagem()).contains("3 dias");
        assertThat(captor.getValue().getReferenciaId()).isEqualTo(assinatura.getId());
    }

    @Test
    @DisplayName("Nao reavisa a mesma assinatura no mesmo dia")
    void naoReavisaMatriculaNoMesmoDia() {
        Assinatura assinatura = assinaturaDe(LocalDate.now().plusDays(3));
        given(assinaturaRepository.buscarAtivasVencendoEntre(any(), any())).willReturn(List.of(assinatura));
        given(repository.existeDesde(eq(1L), eq(TipoNotificacao.MATRICULA_VENCENDO), eq(assinatura.getId()), any()))
                .willReturn(true);

        int criadas = service.gerarNotificacoesMatriculaVencendo(7);

        assertThat(criadas).isZero();
        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Anamnese pendente
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Avisa a secretaria, nao o aluno, sobre anamnese pendente")
    void avisaSecretariaSobreAnamnesePendente() {
        given(usuarioRepository.buscarAlunosSemAnamnese()).willReturn(List.of(aluno));

        int criadas = service.gerarNotificacoesAnamnesePendente();

        assertThat(criadas).isEqualTo(1);
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(secretaria);
        assertThat(captor.getValue().getMensagem()).contains("1 aluno");
    }

    @Test
    @DisplayName("Sem ninguem pendente, nao gera nada")
    void semPendenteNaoGeraNada() {
        given(usuarioRepository.buscarAlunosSemAnamnese()).willReturn(List.of());

        assertThat(service.gerarNotificacoesAnamnesePendente()).isZero();
        verify(repository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Aniversario
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Avisa a secretaria do aniversario do aluno, com o nome na mensagem")
    void avisaSecretariaDeAniversario() {
        given(usuarioRepository.buscarAniversariantesDoDia(any(Integer.class), any(Integer.class)))
                .willAnswer(i -> List.of(aluno));

        int criadas = service.gerarNotificacoesAniversario();

        assertThat(criadas).isEqualTo(1);
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDestinatario()).isEqualTo(secretaria);
        assertThat(captor.getValue().getReferenciaId()).isEqualTo(aluno.getId());
        assertThat(captor.getValue().getMensagem()).contains("Marina Alves");
    }

    private Assinatura assinaturaDe(LocalDate vencimento) {
        Plano plano = new Plano();
        plano.setId(3L);
        plano.setNome("Mensal Centro");

        Assinatura assinatura = new Assinatura();
        assinatura.setId(7L);
        assinatura.setAluno(aluno);
        assinatura.setPlano(plano);
        assinatura.setOrigem(OrigemAssinatura.DIRETO);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setDataVencimento(vencimento);
        return assinatura;
    }
}
