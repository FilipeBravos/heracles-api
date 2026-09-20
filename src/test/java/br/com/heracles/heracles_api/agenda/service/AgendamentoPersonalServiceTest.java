package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.AgendamentoPersonal;
import br.com.heracles.heracles_api.agenda.domain.AulaGrupo;
import br.com.heracles.heracles_api.agenda.domain.StatusAgendamento;
import br.com.heracles.heracles_api.agenda.domain.StatusAula;
import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.repository.AgendamentoPersonalRepository;
import br.com.heracles.heracles_api.agenda.repository.AulaGrupoRepository;
import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoPersonalServiceTest {

    @Mock private AgendamentoPersonalRepository repository;
    @Mock private AulaGrupoRepository aulaGrupoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;

    private AgendamentoPersonalService service;

    private Usuario aluno;
    private Usuario professor;
    private Unidade unidade;
    private final LocalDateTime amanha = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

    @BeforeEach
    void preparar() {
        service = new AgendamentoPersonalService(repository, aulaGrupoRepository, usuarioRepository, unidadeRepository);

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
    }
}
