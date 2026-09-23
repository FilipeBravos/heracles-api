package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.DiaSemana;
import br.com.heracles.heracles_api.agenda.domain.HorarioProfessor;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaCancelamentoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaMinutosOcupadosProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaOcupacaoPersonal;
import br.com.heracles.heracles_api.agenda.dto.LinhaSessaoPersonalFinalizada;
import br.com.heracles.heracles_api.agenda.dto.LinhaSessoesPorProfessor;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
import br.com.heracles.heracles_api.agenda.repository.HorarioProfessorRepository;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoPersonalServiceTest {

    @Mock private AgendamentoPersonalRepository repository;
    @Mock private AulaGrupoRepository aulaGrupoRepository;
    @Mock private HorarioProfessorRepository horarioProfessorRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;

    private AgendamentoPersonalService service;

    private Usuario aluno;
    private Usuario professor;
    private Unidade unidade;
    private final LocalDateTime amanha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

    @BeforeEach
    void preparar() {
        service = new AgendamentoPersonalService(
                repository, aulaGrupoRepository, horarioProfessorRepository, usuarioRepository, unidadeRepository);

        aluno = new Usuario();
        aluno.setId(1L);
        aluno.setNome("Carla Souza");
        aluno.setTipoPerfil(TipoPerfil.ALUNO);
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(aluno));

        professor = new Usuario();
        professor.setId(2L);
        professor.setNome("Prof Ana");
        professor.setTipoPerfil(TipoPerfil.PROFESSOR);
        given(usuarioRepository.findById(2L)).willReturn(Optional.of(professor));

        unidade = new Unidade();
        unidade.setId(3L);
        unidade.setNome("Unidade Centro");
        given(unidadeRepository.findById(3L)).willReturn(Optional.of(unidade));

        given(repository.findByProfessorIdAndStatus(2L, StatusAgendamento.AGENDADO)).willReturn(List.of());
        given(aulaGrupoRepository.findByProfessorIdAndStatus(2L, StatusAula.ATIVA)).willReturn(List.of());
    }

    private AgendamentoPersonalDtos.Salvar pedido() {
        return new AgendamentoPersonalDtos.Salvar(1L, 2L, 3L, amanha, 60, "Foco em pernas");
    }

    @Test
    @DisplayName("So um usuario cadastrado como aluno tem sessao de personal marcada")
    void naoAlunoNaoAgenda() {
        Usuario naoAluno = new Usuario();
        naoAluno.setId(9L);
        naoAluno.setNome("Prof Carlos");
        naoAluno.setTipoPerfil(TipoPerfil.PROFESSOR);
        given(usuarioRepository.findById(9L)).willReturn(Optional.of(naoAluno));

        var request = new AgendamentoPersonalDtos.Salvar(9L, 2L, 3L, amanha, 60, null);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("aluno");
    }

    @Test
    @DisplayName("So um usuario cadastrado como professor da personal")
    void naoProfessorNaoDaPersonal() {
        Usuario naoProfessor = new Usuario();
        naoProfessor.setId(8L);
        naoProfessor.setNome("Diego Ramos");
        naoProfessor.setTipoPerfil(TipoPerfil.ALUNO);
        given(usuarioRepository.findById(8L)).willReturn(Optional.of(naoProfessor));

        var request = new AgendamentoPersonalDtos.Salvar(1L, 8L, 3L, amanha, 60, null);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("professor");
    }

    @Test
    @DisplayName("O professor nao pode ter duas sessoes de personal no mesmo horario")
    void naoConflitaComOutroPersonal() {
        AgendamentoPersonal existente = new AgendamentoPersonal();
        existente.setDataHora(amanha.minusMinutes(30));
        existente.setDuracaoMinutos(60);
        given(repository.findByProfessorIdAndStatus(2L, StatusAgendamento.AGENDADO)).willReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(pedido()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("personal marcada");
    }

    @Test
    @DisplayName("O professor nao pode ter uma sessao de personal por cima de uma aula em grupo")
    void naoConflitaComAulaGrupo() {
        AulaGrupo aula = new AulaGrupo();
        aula.setDataHora(amanha.minusMinutes(30));
        aula.setDuracaoMinutos(60);
        given(aulaGrupoRepository.findByProfessorIdAndStatus(2L, StatusAula.ATIVA)).willReturn(List.of(aula));

        assertThatThrownBy(() -> service.criar(pedido()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("aula em grupo");
    }

    @Test
    @DisplayName("Sessao sem conflito e agendada normalmente")
    void agendaSemConflito() {
        given(repository.save(any())).willAnswer(i -> {
            AgendamentoPersonal sessao = i.getArgument(0);
            sessao.setId(50L);
            return sessao;
        });

        AgendamentoPersonalDtos.Response resposta = service.criar(pedido());

        assertThat(resposta.alunoNome()).isEqualTo("Carla Souza");
        assertThat(resposta.professorNome()).isEqualTo("Prof Ana");
        assertThat(resposta.status()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    @DisplayName("Observacoes em branco viram nulo, nao string vazia")
    void observacoesEmBrancoViraNulo() {
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
        var request = new AgendamentoPersonalDtos.Salvar(1L, 2L, 3L, amanha, 60, "   ");

        ArgumentCaptor<AgendamentoPersonal> captor = ArgumentCaptor.forClass(AgendamentoPersonal.class);
        service.criar(request);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getObservacoes()).isNull();
    }

    @Test
    @DisplayName("Cancelar marca o status, sem apagar o historico")
    void cancelarMarcaStatus() {
        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setId(50L);
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(amanha);
        sessao.setDuracaoMinutos(60);
        sessao.setStatus(StatusAgendamento.AGENDADO);
        given(repository.findById(50L)).willReturn(Optional.of(sessao));

        AgendamentoPersonalDtos.Response resposta = service.cancelar(50L);

        assertThat(resposta.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(sessao.getCanceladoEm()).isNotNull();
    }

    @Test
    @DisplayName("Uma sessao ja realizada nao pode ser cancelada")
    void naoCancelaSessaoRealizada() {
        AgendamentoPersonal sessao = sessaoRealizada();
        given(repository.findById(50L)).willReturn(Optional.of(sessao));

        assertThatThrownBy(() -> service.cancelar(50L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("realizada");
    }

    // ---------------------------------------------------------------
    // Marcar como realizada
    // ---------------------------------------------------------------

    @Test
    @DisplayName("So o proprio professor confirma que a sessao aconteceu")
    void marcarRealizadaSoProprioProfessor() {
        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setId(50L);
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(LocalDateTime.now().minusHours(2));
        sessao.setDuracaoMinutos(60);
        sessao.setStatus(StatusAgendamento.AGENDADO);
        given(repository.findById(50L)).willReturn(Optional.of(sessao));

        Usuario outroProfessor = new Usuario();
        outroProfessor.setId(99L);
        outroProfessor.setEmail("outro@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("outro@heracles.com.br"))
                .willReturn(Optional.of(outroProfessor));

        assertThatThrownBy(() -> service.marcarRealizadaEu("outro@heracles.com.br", 50L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O professor confirma a propria sessao ja passada")
    void marcarRealizadaComSucesso() {
        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setId(50L);
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(LocalDateTime.now().minusHours(2));
        sessao.setDuracaoMinutos(60);
        sessao.setStatus(StatusAgendamento.AGENDADO);
        given(repository.findById(50L)).willReturn(Optional.of(sessao));
        given(usuarioRepository.findByEmailIgnoreCase("prof@heracles.com.br")).willReturn(Optional.of(professor));
        professor.setEmail("prof@heracles.com.br");

        AgendamentoPersonalDtos.Response resposta = service.marcarRealizadaEu("prof@heracles.com.br", 50L);

        assertThat(resposta.status()).isEqualTo(StatusAgendamento.REALIZADA);
    }

    @Test
    @DisplayName("Nao da pra confirmar uma sessao que ainda nao aconteceu")
    void naoMarcaRealizadaAntesDoHorario() {
        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setId(50L);
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(amanha);
        sessao.setDuracaoMinutos(60);
        sessao.setStatus(StatusAgendamento.AGENDADO);
        given(repository.findById(50L)).willReturn(Optional.of(sessao));
        professor.setEmail("prof@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("prof@heracles.com.br")).willReturn(Optional.of(professor));

        assertThatThrownBy(() -> service.marcarRealizadaEu("prof@heracles.com.br", 50L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ainda nao aconteceu");
    }

    // ---------------------------------------------------------------
    // Avaliar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("So o proprio aluno avalia a sessao")
    void avaliarSoProprioAluno() {
        AgendamentoPersonal sessao = sessaoRealizada();
        given(repository.findById(50L)).willReturn(Optional.of(sessao));

        Usuario outroAluno = new Usuario();
        outroAluno.setId(77L);
        outroAluno.setEmail("outro@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("outro@heracles.com.br")).willReturn(Optional.of(outroAluno));

        var request = new AgendamentoPersonalDtos.Avaliar(5, "Otima sessao");

        assertThatThrownBy(() -> service.avaliarEu("outro@heracles.com.br", 50L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O aluno avalia a propria sessao ja realizada")
    void avaliarComSucesso() {
        AgendamentoPersonal sessao = sessaoRealizada();
        given(repository.findById(50L)).willReturn(Optional.of(sessao));
        aluno.setEmail("aluno@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("aluno@heracles.com.br")).willReturn(Optional.of(aluno));

        var request = new AgendamentoPersonalDtos.Avaliar(4, "Bom professor");
        AgendamentoPersonalDtos.Response resposta = service.avaliarEu("aluno@heracles.com.br", 50L, request);

        assertThat(resposta.notaAvaliacao()).isEqualTo(4);
        assertThat(resposta.comentarioAvaliacao()).isEqualTo("Bom professor");
    }

    @Test
    @DisplayName("Comentario em branco vira nulo, nao string vazia")
    void avaliarComentarioEmBrancoViraNulo() {
        AgendamentoPersonal sessao = sessaoRealizada();
        given(repository.findById(50L)).willReturn(Optional.of(sessao));
        aluno.setEmail("aluno@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("aluno@heracles.com.br")).willReturn(Optional.of(aluno));

        var request = new AgendamentoPersonalDtos.Avaliar(5, "   ");
        AgendamentoPersonalDtos.Response resposta = service.avaliarEu("aluno@heracles.com.br", 50L, request);

        assertThat(resposta.comentarioAvaliacao()).isNull();
    }

    @Test
    @DisplayName("Nao da pra avaliar uma sessao que ainda nao foi realizada")
    void naoAvaliaSessaoNaoRealizada() {
        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setId(50L);
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(amanha);
        sessao.setDuracaoMinutos(60);
        sessao.setStatus(StatusAgendamento.AGENDADO);
        given(repository.findById(50L)).willReturn(Optional.of(sessao));
        aluno.setEmail("aluno@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("aluno@heracles.com.br")).willReturn(Optional.of(aluno));

        var request = new AgendamentoPersonalDtos.Avaliar(3, null);

        assertThatThrownBy(() -> service.avaliarEu("aluno@heracles.com.br", 50L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ainda nao foi realizada");
    }

    @Test
    @DisplayName("Uma sessao so pode ser avaliada uma vez")
    void naoAvaliaDuasVezes() {
        AgendamentoPersonal sessao = sessaoRealizada();
        sessao.avaliar(5, "Otimo");
        given(repository.findById(50L)).willReturn(Optional.of(sessao));
        aluno.setEmail("aluno@heracles.com.br");
        given(usuarioRepository.findByEmailIgnoreCase("aluno@heracles.com.br")).willReturn(Optional.of(aluno));

        var request = new AgendamentoPersonalDtos.Avaliar(1, "Mudei de ideia");

        assertThatThrownBy(() -> service.avaliarEu("aluno@heracles.com.br", 50L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja foi avaliada");
    }

    // ---------------------------------------------------------------
    // Ranking de avaliacoes
    // ---------------------------------------------------------------

    @Test
    @DisplayName("A media por professor vem pronta do repositorio, com a quantidade minima repassada")
    void mediaAvaliacaoDelegaParaRepositorio() {
        given(repository.mediaAvaliacaoPorProfessor(3L)).willReturn(
                List.of(new LinhaAvaliacaoProfessor(2L, "Prof Ana", 4.5, 10L)));

        List<LinhaAvaliacaoProfessor> media = service.mediaAvaliacaoPorProfessor(3L);

        assertThat(media).hasSize(1);
        assertThat(media.get(0).professorNome()).isEqualTo("Prof Ana");
        assertThat(media.get(0).notaMedia()).isEqualTo(4.5);
    }

    // ---------------------------------------------------------------
    // Taxa de cancelamento
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Cancelamento com menos de 24h de antecedencia conta na taxa")
    void cancelamentoEmCimaDaHoraContaNaTaxa() {
        LocalDateTime dataHoraCancelada = LocalDateTime.of(2026, 3, 10, 18, 0);
        given(repository.sessoesFinalizadasDesde(any())).willReturn(List.of(
                new LinhaSessaoPersonalFinalizada(
                        2L, "Prof Ana", StatusAgendamento.CANCELADO, dataHoraCancelada, dataHoraCancelada.minusHours(2)),
                new LinhaSessaoPersonalFinalizada(
                        2L, "Prof Ana", StatusAgendamento.REALIZADA, dataHoraCancelada.plusDays(1), null)));

        List<LinhaCancelamentoProfessor> resultado = service.taxaCancelamentoPorProfessor(90, 1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).totalSessoes()).isEqualTo(2);
        assertThat(resultado.get(0).cancelamentosEmCimaDaHora()).isEqualTo(1);
        assertThat(resultado.get(0).taxaCancelamento()).isEqualByComparingTo("50.0");
    }

    @Test
    @DisplayName("Cancelamento com mais de 24h de antecedencia nao conta como em cima da hora")
    void cancelamentoComAntecedenciaNaoConta() {
        LocalDateTime dataHoraCancelada = LocalDateTime.of(2026, 3, 10, 18, 0);
        given(repository.sessoesFinalizadasDesde(any())).willReturn(List.of(
                new LinhaSessaoPersonalFinalizada(
                        2L, "Prof Ana", StatusAgendamento.CANCELADO, dataHoraCancelada, dataHoraCancelada.minusHours(48))));

        List<LinhaCancelamentoProfessor> resultado = service.taxaCancelamentoPorProfessor(90, 1L);

        assertThat(resultado.get(0).cancelamentosEmCimaDaHora()).isEqualTo(0);
        assertThat(resultado.get(0).taxaCancelamento()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Cancelamento anterior a coluna existir (sem canceladoEm) nao conta como em cima da hora")
    void cancelamentoSemDataDeCancelamentoNaoConta() {
        given(repository.sessoesFinalizadasDesde(any())).willReturn(List.of(
                new LinhaSessaoPersonalFinalizada(
                        2L, "Prof Ana", StatusAgendamento.CANCELADO, LocalDateTime.of(2026, 3, 10, 18, 0), null)));

        List<LinhaCancelamentoProfessor> resultado = service.taxaCancelamentoPorProfessor(90, 1L);

        assertThat(resultado.get(0).cancelamentosEmCimaDaHora()).isEqualTo(0);
    }

    @Test
    @DisplayName("Quantidade minima filtra quem ainda nao tem amostra suficiente")
    void quantidadeMinimaFiltraAmostraPequena() {
        given(repository.sessoesFinalizadasDesde(any())).willReturn(List.of(
                new LinhaSessaoPersonalFinalizada(
                        2L, "Prof Ana", StatusAgendamento.REALIZADA, LocalDateTime.of(2026, 3, 10, 18, 0), null)));

        List<LinhaCancelamentoProfessor> resultado = service.taxaCancelamentoPorProfessor(90, 4L);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Ordena do pior pro melhor")
    void ordenaDoPiorProMelhor() {
        LocalDateTime data = LocalDateTime.of(2026, 3, 10, 18, 0);
        given(repository.sessoesFinalizadasDesde(any())).willReturn(List.of(
                // Prof Ana: 1 de 2 canceladas em cima da hora = 50%.
                new LinhaSessaoPersonalFinalizada(2L, "Prof Ana", StatusAgendamento.CANCELADO, data, data.minusHours(1)),
                new LinhaSessaoPersonalFinalizada(2L, "Prof Ana", StatusAgendamento.REALIZADA, data, null),
                // Prof Bia: 1 de 1 cancelada em cima da hora = 100%.
                new LinhaSessaoPersonalFinalizada(9L, "Prof Bia", StatusAgendamento.CANCELADO, data, data.minusHours(1))));

        List<LinhaCancelamentoProfessor> resultado = service.taxaCancelamentoPorProfessor(90, 1L);

        assertThat(resultado).extracting(LinhaCancelamentoProfessor::professorNome)
                .containsExactly("Prof Bia", "Prof Ana");
    }

    // ---------------------------------------------------------------
    // Taxa de ocupacao da agenda
    // ---------------------------------------------------------------

    private HorarioProfessor bloco(Usuario prof, DiaSemana dia, int horaInicio, int horaFim) {
        HorarioProfessor horario = new HorarioProfessor();
        horario.setProfessor(prof);
        horario.setDiaSemana(dia);
        horario.setHoraInicio(java.time.LocalTime.of(horaInicio, 0));
        horario.setHoraFim(java.time.LocalTime.of(horaFim, 0));
        return horario;
    }

    @Test
    @DisplayName("Ocupacao compara as horas configuradas com as horas realizadas no periodo")
    void ocupacaoComparaConfiguradoComRealizado() {
        // 4h na segunda + 4h na quarta = 8h semanais.
        given(horarioProfessorRepository.findAll()).willReturn(List.of(
                bloco(professor, DiaSemana.SEGUNDA, 8, 12),
                bloco(professor, DiaSemana.QUARTA, 8, 12)));
        given(repository.minutosOcupadosPorProfessorDesde(any())).willReturn(
                List.of(new LinhaMinutosOcupadosProfessor(2L, 240L)));

        List<LinhaOcupacaoPersonal> resultado = service.ocupacaoPorProfessor(7);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).horasDisponiveis()).isEqualByComparingTo("8.0");
        assertThat(resultado.get(0).horasOcupadas()).isEqualByComparingTo("4.0");
        assertThat(resultado.get(0).taxaOcupacao()).isEqualByComparingTo("50.0");
    }

    @Test
    @DisplayName("Sem sessao realizada no periodo, a ocupacao e zero")
    void ocupacaoZeroSemSessaoRealizada() {
        given(horarioProfessorRepository.findAll()).willReturn(List.of(
                bloco(professor, DiaSemana.SEGUNDA, 8, 12)));
        given(repository.minutosOcupadosPorProfessorDesde(any())).willReturn(List.of());

        List<LinhaOcupacaoPersonal> resultado = service.ocupacaoPorProfessor(7);

        assertThat(resultado.get(0).horasOcupadas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resultado.get(0).taxaOcupacao()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Ordena do menos ocupado pro mais ocupado")
    void ocupacaoOrdenaDoMenosProMaisOcupado() {
        Usuario outroProfessor = new Usuario();
        outroProfessor.setId(9L);
        outroProfessor.setNome("Prof Bia");

        given(horarioProfessorRepository.findAll()).willReturn(List.of(
                bloco(professor, DiaSemana.SEGUNDA, 8, 12),
                bloco(outroProfessor, DiaSemana.SEGUNDA, 8, 12)));
        given(repository.minutosOcupadosPorProfessorDesde(any())).willReturn(List.of(
                // Prof Ana: 4h de 4h = 100%. Prof Bia: 1h de 4h = 25%.
                new LinhaMinutosOcupadosProfessor(2L, 240L),
                new LinhaMinutosOcupadosProfessor(9L, 60L)));

        List<LinhaOcupacaoPersonal> resultado = service.ocupacaoPorProfessor(7);

        assertThat(resultado).extracting(LinhaOcupacaoPersonal::professorNome)
                .containsExactly("Prof Bia", "Prof Ana");
    }

    @Test
    @DisplayName("Ranking de sessoes repassa a contagem por professor tal como a consulta devolve, do mais cheio pro menos cheio")
    void sessoesRealizadasRepassaRankingDaConsulta() {
        given(repository.contarSessoesRealizadasPorProfessorDesde(any())).willReturn(List.of(
                new LinhaSessoesPorProfessor(2L, "Prof Ana", 30L),
                new LinhaSessoesPorProfessor(9L, "Prof Bia", 12L)));

        List<LinhaSessoesPorProfessor> resultado = service.sessoesRealizadasPorProfessor(90);

        assertThat(resultado).extracting(LinhaSessoesPorProfessor::professorNome)
                .containsExactly("Prof Ana", "Prof Bia");
        assertThat(resultado.get(0).quantidadeSessoes()).isEqualTo(30L);
    }

    private AgendamentoPersonal sessaoRealizada() {
        AgendamentoPersonal sessao = new AgendamentoPersonal();
        sessao.setId(50L);
        sessao.setAluno(aluno);
        sessao.setProfessor(professor);
        sessao.setUnidade(unidade);
        sessao.setDataHora(LocalDateTime.now().minusHours(2));
        sessao.setDuracaoMinutos(60);
        sessao.setStatus(StatusAgendamento.REALIZADA);
        return sessao;
    }
}
