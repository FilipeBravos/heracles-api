package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.InscricaoAula;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.domain.StatusInscricao;
import br.com.heracles.heracles_api.agenda.dto.AulaGrupoDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaFaltaAluno;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
import br.com.heracles.heracles_api.agenda.repository.InscricaoAulaRepository;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.core.service.NotificacaoService;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AulaGrupoServiceTest {

    @Mock private AulaGrupoRepository repository;
    @Mock private InscricaoAulaRepository inscricaoRepository;
    @Mock private AgendamentoPersonalRepository agendamentoPersonalRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private NotificacaoService notificacaoService;

    private AulaGrupoService service;

    private Usuario professor;
    private Usuario aluno;
    private Unidade unidade;
    private final LocalDateTime amanha = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0);

    @BeforeEach
    void preparar() {
        service = new AulaGrupoService(
                repository, inscricaoRepository, agendamentoPersonalRepository, usuarioRepository,
                unidadeRepository, notificacaoService);

        professor = new Usuario();
        professor.setId(1L);
        professor.setNome("Prof Ana");
        professor.setTipoPerfil(TipoPerfil.PROFESSOR);
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(professor));

        aluno = new Usuario();
        aluno.setId(2L);
        aluno.setNome("Carla Souza");
        aluno.setTipoPerfil(TipoPerfil.ALUNO);
        given(usuarioRepository.findById(2L)).willReturn(Optional.of(aluno));
        given(usuarioRepository.findByEmailIgnoreCase("carla@ex.com")).willReturn(Optional.of(aluno));

        unidade = new Unidade();
        unidade.setId(3L);
        unidade.setNome("Unidade Centro");
        given(unidadeRepository.findById(3L)).willReturn(Optional.of(unidade));

        given(repository.findByProfessorIdAndStatus(1L, StatusAula.ATIVA)).willReturn(List.of());
        given(agendamentoPersonalRepository.findByProfessorIdAndStatus(eq(1L), any()))
                .willReturn(List.of());
    }

    private static <T> T eq(T valor) {
        return org.mockito.ArgumentMatchers.eq(valor);
    }

    private AulaGrupoDtos.Salvar pedido() {
        return new AulaGrupoDtos.Salvar("Spinning", 1L, 3L, amanha, 50, 15);
    }

    private AulaGrupo aulaSalva(long capacidade) {
        AulaGrupo aula = new AulaGrupo();
        aula.setId(10L);
        aula.setNome("Spinning");
        aula.setProfessor(professor);
        aula.setUnidade(unidade);
        aula.setDataHora(amanha);
        aula.setDuracaoMinutos(50);
        aula.setCapacidadeMaxima((int) capacidade);
        aula.setStatus(StatusAula.ATIVA);
        return aula;
    }

    @Test
    @DisplayName("So um usuario cadastrado como professor pode dar aula")
    void naoProfessorNaoDaAula() {
        Usuario naoProfessor = new Usuario();
        naoProfessor.setId(4L);
        naoProfessor.setNome("Carla Souza");
        naoProfessor.setTipoPerfil(TipoPerfil.ALUNO);
        given(usuarioRepository.findById(4L)).willReturn(Optional.of(naoProfessor));

        var request = new AulaGrupoDtos.Salvar("Spinning", 4L, 3L, amanha, 50, 15);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("professor");
    }

    @Test
    @DisplayName("O professor nao pode ter duas aulas no mesmo horario")
    void naoConflitaComOutraAula() {
        AulaGrupo existente = aulaSalva(10);
        existente.setDataHora(amanha.minusMinutes(20));
        given(repository.findByProfessorIdAndStatus(1L, StatusAula.ATIVA)).willReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(pedido()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("aula marcada");
    }

    @Test
    @DisplayName("O professor nao pode ter uma aula por cima de um personal ja marcado")
    void naoConflitaComPersonal() {
        br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal personal =
                new br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal();
        personal.setDataHora(amanha.minusMinutes(10));
        personal.setDuracaoMinutos(60);
        given(agendamentoPersonalRepository.findByProfessorIdAndStatus(eq(1L), any()))
                .willReturn(List.of(personal));

        assertThatThrownBy(() -> service.criar(pedido()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("personal");
    }

    @Test
    @DisplayName("Aula sem conflito e criada normalmente")
    void criaSemConflito() {
        given(repository.save(any())).willAnswer(i -> {
            AulaGrupo a = i.getArgument(0);
            a.setId(10L);
            return a;
        });

        AulaGrupoDtos.Response resposta = service.criar(pedido());

        assertThat(resposta.nome()).isEqualTo("Spinning");
        assertThat(resposta.status()).isEqualTo(StatusAula.ATIVA);
        assertThat(resposta.vagasOcupadas()).isZero();
    }

    @Test
    @DisplayName("Nao da para marcar vaga numa aula cancelada")
    void naoInscreveEmAulaCancelada() {
        AulaGrupo cancelada = aulaSalva(15);
        cancelada.setStatus(StatusAula.CANCELADA);
        given(repository.findById(10L)).willReturn(Optional.of(cancelada));

        assertThatThrownBy(() -> service.inscrever(10L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    @DisplayName("Aula lotada manda para a fila de espera em vez de recusar")
    void aulaLotadaEntraNaListaDeEspera() {
        AulaGrupo aula = aulaSalva(1);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        given(inscricaoRepository.countByAulaIdAndStatus(10L, StatusInscricao.INSCRITA)).willReturn(1L);
        given(inscricaoRepository.countByAulaIdAndStatus(10L, StatusInscricao.EM_ESPERA)).willReturn(1L);

        AulaGrupoDtos.ResultadoInscricao resultado = service.inscrever(10L, 2L);

        assertThat(resultado.status()).isEqualTo(StatusInscricao.EM_ESPERA);
        assertThat(resultado.posicaoEspera()).isEqualTo(1);

        ArgumentCaptor<InscricaoAula> captor = ArgumentCaptor.forClass(InscricaoAula.class);
        verify(inscricaoRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusInscricao.EM_ESPERA);
    }

    @Test
    @DisplayName("Aluno ja inscrito ou na espera nao se inscreve de novo")
    void naoInscreveDuasVezes() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        given(inscricaoRepository.existsByAulaIdAndAlunoIdAndStatusIn(
                10L, 2L, List.of(StatusInscricao.INSCRITA, StatusInscricao.EM_ESPERA)))
                .willReturn(true);

        assertThatThrownBy(() -> service.inscrever(10L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta inscrito");
    }

    @Test
    @DisplayName("Marcar vaga em nome do aluno grava a inscricao")
    void inscreverGravaInscricao() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));

        AulaGrupoDtos.ResultadoInscricao resultado = service.inscrever(10L, 2L);

        assertThat(resultado.status()).isEqualTo(StatusInscricao.INSCRITA);
        ArgumentCaptor<InscricaoAula> captor = ArgumentCaptor.forClass(InscricaoAula.class);
        verify(inscricaoRepository).save(captor.capture());
        assertThat(captor.getValue().getAluno()).isEqualTo(aluno);
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusInscricao.INSCRITA);
    }

    @Test
    @DisplayName("O self-service do aluno resolve o id pelo token, nao por parametro")
    void inscreverEuUsaOAlunoDoToken() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));

        service.inscreverEu("carla@ex.com", 10L);

        ArgumentCaptor<InscricaoAula> captor = ArgumentCaptor.forClass(InscricaoAula.class);
        verify(inscricaoRepository).save(captor.capture());
        assertThat(captor.getValue().getAluno().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Cancelar inscricao inexistente devolve 404")
    void cancelarInscricaoInexistente() {
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatusIn(
                10L, 2L, List.of(StatusInscricao.INSCRITA, StatusInscricao.EM_ESPERA)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelarInscricao(10L, 2L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Cancelar uma vaga marcada promove quem espera ha mais tempo")
    void cancelarPromoveDaEspera() {
        AulaGrupo aula = aulaSalva(1);
        aula.setId(10L);

        InscricaoAula minhaInscricao = new InscricaoAula();
        minhaInscricao.setAula(aula);
        minhaInscricao.setAluno(aluno);
        minhaInscricao.setStatus(StatusInscricao.INSCRITA);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatusIn(
                10L, 2L, List.of(StatusInscricao.INSCRITA, StatusInscricao.EM_ESPERA)))
                .willReturn(Optional.of(minhaInscricao));

        Usuario proximoDaFila = new Usuario();
        proximoDaFila.setId(5L);
        proximoDaFila.setNome("Bruno Espera");
        InscricaoAula naFila = new InscricaoAula();
        naFila.setAula(aula);
        naFila.setAluno(proximoDaFila);
        naFila.setStatus(StatusInscricao.EM_ESPERA);
        given(inscricaoRepository.findByAulaIdAndStatusOrderByInscritoEmAsc(10L, StatusInscricao.EM_ESPERA))
                .willReturn(List.of(naFila));

        service.cancelarInscricao(10L, 2L);

        assertThat(minhaInscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(naFila.getStatus()).isEqualTo(StatusInscricao.INSCRITA);
        verify(notificacaoService).notificarVagaLiberada(proximoDaFila, 10L, "Spinning");
    }

    @Test
    @DisplayName("Cancelar a propria espera nao promove ninguem — nenhuma vaga foi liberada")
    void cancelarEsperaNaoPromoveNinguem() {
        AulaGrupo aula = aulaSalva(1);
        aula.setId(10L);

        InscricaoAula minhaEspera = new InscricaoAula();
        minhaEspera.setAula(aula);
        minhaEspera.setAluno(aluno);
        minhaEspera.setStatus(StatusInscricao.EM_ESPERA);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatusIn(
                10L, 2L, List.of(StatusInscricao.INSCRITA, StatusInscricao.EM_ESPERA)))
                .willReturn(Optional.of(minhaEspera));

        service.cancelarInscricao(10L, 2L);

        assertThat(minhaEspera.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        verify(inscricaoRepository, org.mockito.Mockito.never())
                .findByAulaIdAndStatusOrderByInscritoEmAsc(any(), any());
    }

    @Test
    @DisplayName("Listar para o aluno marca se ele ja esta inscrito em cada aula")
    void listarParaAlunoMarcaInscrito() {
        AulaGrupo aula = aulaSalva(15);
        var pagina = new org.springframework.data.domain.PageImpl<>(List.of(aula));
        given(repository.findByStatusAndDataHoraGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(StatusAula.ATIVA), any(), any()))
                .willReturn(pagina);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.of(new InscricaoAula()));

        var resultado = service.listarParaAluno("carla@ex.com", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).inscrito()).isTrue();
    }

    @Test
    @DisplayName("Listar para o aluno mostra a posicao dele na fila de espera")
    void listarParaAlunoMostraPosicaoNaEspera() {
        AulaGrupo aula = aulaSalva(1);
        var pagina = new org.springframework.data.domain.PageImpl<>(List.of(aula));
        given(repository.findByStatusAndDataHoraGreaterThanEqual(
                org.mockito.ArgumentMatchers.eq(StatusAula.ATIVA), any(), any()))
                .willReturn(pagina);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.empty());

        InscricaoAula outroNaFila = new InscricaoAula();
        outroNaFila.setAluno(new Usuario());
        outroNaFila.getAluno().setId(9L);
        InscricaoAula minhaEspera = new InscricaoAula();
        minhaEspera.setAluno(aluno);
        given(inscricaoRepository.findByAulaIdAndStatusOrderByInscritoEmAsc(10L, StatusInscricao.EM_ESPERA))
                .willReturn(List.of(outroNaFila, minhaEspera));

        var resultado = service.listarParaAluno("carla@ex.com", org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(resultado.getContent().get(0).inscrito()).isFalse();
        assertThat(resultado.getContent().get(0).posicaoEspera()).isEqualTo(2);
    }

    // ---------------------------------------------------------------
    // Presenca
    // ---------------------------------------------------------------

    private AulaGrupo aulaPassada(long capacidade) {
        AulaGrupo aula = aulaSalva(capacidade);
        aula.setDataHora(LocalDateTime.now().minusHours(3));
        return aula;
    }

    @Test
    @DisplayName("So o proprio professor ve o roster da aula")
    void listarInscricoesSoProprioProfessor() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));

        Usuario outroProfessor = new Usuario();
        outroProfessor.setId(99L);
        outroProfessor.setEmail("outro@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("outro@heracles.com.br"))
                .willReturn(Optional.of(outroProfessor));

        assertThatThrownBy(() -> service.listarInscricoesEu("outro@heracles.com.br", 10L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O roster vem em ordem alfabetica, so quem tem vaga marcada")
    void listarInscricoesRetornaRoster() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        professor.setEmail("ana@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("ana@heracles.com.br")).willReturn(Optional.of(professor));

        InscricaoAula inscricao = new InscricaoAula();
        inscricao.setAluno(aluno);
        inscricao.setStatus(StatusInscricao.INSCRITA);
        given(inscricaoRepository.findByAulaIdAndStatusOrderByAluno_NomeAsc(10L, StatusInscricao.INSCRITA))
                .willReturn(List.of(inscricao));

        List<AulaGrupoDtos.LinhaPresenca> roster = service.listarInscricoesEu("ana@heracles.com.br", 10L);

        assertThat(roster).hasSize(1);
        assertThat(roster.get(0).alunoNome()).isEqualTo("Carla Souza");
        assertThat(roster.get(0).presente()).isNull();
    }

    @Test
    @DisplayName("So o proprio professor confirma presenca")
    void confirmarPresencaSoProprioProfessor() {
        AulaGrupo aula = aulaPassada(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));

        Usuario outroProfessor = new Usuario();
        outroProfessor.setId(99L);
        outroProfessor.setEmail("outro@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("outro@heracles.com.br"))
                .willReturn(Optional.of(outroProfessor));

        assertThatThrownBy(() -> service.confirmarPresencaEu("outro@heracles.com.br", 10L, 2L, true))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Nao da pra confirmar presenca de uma aula que ainda nao aconteceu")
    void naoConfirmaPresencaAntesDaAula() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        professor.setEmail("ana@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("ana@heracles.com.br")).willReturn(Optional.of(professor));

        InscricaoAula inscricao = new InscricaoAula();
        inscricao.setAula(aula);
        inscricao.setAluno(aluno);
        inscricao.setStatus(StatusInscricao.INSCRITA);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> service.confirmarPresencaEu("ana@heracles.com.br", 10L, 2L, true))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ainda nao aconteceu");
    }

    @Test
    @DisplayName("O professor confirma presenca da aula ja passada")
    void confirmarPresencaComSucesso() {
        AulaGrupo aula = aulaPassada(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        professor.setEmail("ana@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("ana@heracles.com.br")).willReturn(Optional.of(professor));

        InscricaoAula inscricao = new InscricaoAula();
        inscricao.setAula(aula);
        inscricao.setAluno(aluno);
        inscricao.setStatus(StatusInscricao.INSCRITA);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.of(inscricao));

        service.confirmarPresencaEu("ana@heracles.com.br", 10L, 2L, false);

        assertThat(inscricao.getPresente()).isFalse();
        assertThat(inscricao.getPresencaConfirmadaEm()).isNotNull();
    }

    @Test
    @DisplayName("Presenca ja confirmada nao se refaz")
    void naoConfirmaPresencaDuasVezes() {
        AulaGrupo aula = aulaPassada(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        professor.setEmail("ana@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("ana@heracles.com.br")).willReturn(Optional.of(professor));

        InscricaoAula inscricao = new InscricaoAula();
        inscricao.setAula(aula);
        inscricao.setAluno(aluno);
        inscricao.setStatus(StatusInscricao.INSCRITA);
        inscricao.setPresente(true);
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> service.confirmarPresencaEu("ana@heracles.com.br", 10L, 2L, false))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja foi confirmada");
    }

    @Test
    @DisplayName("So confirma presenca de quem tem vaga marcada")
    void confirmarPresencaInscricaoNaoEncontrada() {
        AulaGrupo aula = aulaPassada(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        professor.setEmail("ana@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("ana@heracles.com.br")).willReturn(Optional.of(professor));
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarPresencaEu("ana@heracles.com.br", 10L, 2L, true))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O relatorio calcula a taxa de comparecimento geral")
    void relatorioPresencaCalculaTaxa() {
        given(inscricaoRepository.totalConfirmadasDesde(any())).willReturn(10L);
        given(inscricaoRepository.totalFaltasDesde(any())).willReturn(3L);
        given(inscricaoRepository.faltasPorAlunoDesde(any(), any())).willReturn(
                List.of(new LinhaFaltaAluno(2L, "Carla Souza", 3L, 7L)));

        AulaGrupoDtos.PainelPresenca relatorio = service.relatorioPresenca(90);

        assertThat(relatorio.dias()).isEqualTo(90);
        assertThat(relatorio.totalConfirmadas()).isEqualTo(10L);
        assertThat(relatorio.totalFaltas()).isEqualTo(3L);
        // (10 - 3) / 10 * 100 = 70.0
        assertThat(relatorio.taxaComparecimento()).isEqualByComparingTo("70.0");
        assertThat(relatorio.maisFaltosos()).hasSize(1);
    }

    @Test
    @DisplayName("Sem presenca confirmada no periodo, a taxa e zero em vez de dividir por zero")
    void relatorioPresencaSemConfirmacaoTaxaZero() {
        given(inscricaoRepository.totalConfirmadasDesde(any())).willReturn(0L);
        given(inscricaoRepository.totalFaltasDesde(any())).willReturn(0L);
        given(inscricaoRepository.faltasPorAlunoDesde(any(), any())).willReturn(List.of());

        AulaGrupoDtos.PainelPresenca relatorio = service.relatorioPresenca(90);

        assertThat(relatorio.taxaComparecimento()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
        assertThat(relatorio.maisFaltosos()).isEmpty();
    }
}
