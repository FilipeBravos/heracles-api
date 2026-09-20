package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.InscricaoAula;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.domain.StatusInscricao;
import br.com.heracles.heracles_api.agenda.dto.AulaGrupoDtos;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
import br.com.heracles.heracles_api.agenda.repository.InscricaoAulaRepository;
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

    private AulaGrupoService service;

    private Usuario professor;
    private Usuario aluno;
    private Unidade unidade;
    private final LocalDateTime amanha = LocalDateTime.now().plusDays(1).withHour(18).withMinute(0);

    @BeforeEach
    void preparar() {
        service = new AulaGrupoService(
                repository, inscricaoRepository, agendamentoPersonalRepository, usuarioRepository,
                unidadeRepository);

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
    @DisplayName("Aula lotada recusa nova inscricao")
    void aulaLotadaRecusaInscricao() {
        AulaGrupo aula = aulaSalva(1);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        given(inscricaoRepository.countByAulaIdAndStatus(10L, StatusInscricao.INSCRITA)).willReturn(1L);

        assertThatThrownBy(() -> service.inscrever(10L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("lotada");
    }

    @Test
    @DisplayName("Aluno ja inscrito nao se inscreve de novo")
    void naoInscreveDuasVezes() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.of(new InscricaoAula()));

        assertThatThrownBy(() -> service.inscrever(10L, 2L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta inscrito");
    }

    @Test
    @DisplayName("Marcar vaga em nome do aluno grava a inscricao")
    void inscreverGravaInscricao() {
        AulaGrupo aula = aulaSalva(15);
        given(repository.findById(10L)).willReturn(Optional.of(aula));

        service.inscrever(10L, 2L);

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
        given(inscricaoRepository.findByAulaIdAndAlunoIdAndStatus(10L, 2L, StatusInscricao.INSCRITA))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelarInscricao(10L, 2L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
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
}
