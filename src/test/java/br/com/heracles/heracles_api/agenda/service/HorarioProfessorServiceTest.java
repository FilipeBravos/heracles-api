package br.com.heracles.heracles_api.agenda.service;

import br.com.heracles.heracles_api.agenda.domain.DiaSemana;
import br.com.heracles.heracles_api.agenda.domain.HorarioProfessor;
import br.com.heracles.heracles_api.agenda.dto.HorarioProfessorDtos;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
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
class HorarioProfessorServiceTest {

    @Mock private HorarioProfessorRepository repository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;

    private HorarioProfessorService service;

    private Usuario professor;
    private Unidade unidade;

    @BeforeEach
    void preparar() {
        service = new HorarioProfessorService(repository, usuarioRepository, unidadeRepository);

        professor = new Usuario();
        professor.setId(1L);
        professor.setNome("Prof Ana");
        professor.setTipoPerfil(TipoPerfil.PROFESSOR);
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(professor));

        unidade = new Unidade();
        unidade.setId(2L);
        unidade.setNome("Unidade Centro");
        given(unidadeRepository.findById(2L)).willReturn(Optional.of(unidade));

        given(repository.findByProfessorIdOrderByDiaSemanaAscHoraInicioAsc(1L)).willReturn(List.of());
    }

    @Test
    @DisplayName("So um usuario cadastrado como professor tem horario")
    void naoProfessorNaoTemHorario() {
        Usuario aluno = new Usuario();
        aluno.setId(9L);
        aluno.setNome("Carla Souza");
        aluno.setTipoPerfil(TipoPerfil.ALUNO);
        given(usuarioRepository.findById(9L)).willReturn(Optional.of(aluno));

        assertThatThrownBy(() -> service.listar(9L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("professor");
    }

    @Test
    @DisplayName("Professor inexistente devolve 404")
    void professorInexistente() {
        given(usuarioRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(999L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Horario de inicio precisa ser antes do horario de fim")
    void inicioAntesDoFim() {
        var request = new HorarioProfessorDtos.Salvar(
                DiaSemana.SEGUNDA, LocalTime.of(12, 0), LocalTime.of(8, 0), 2L);

        assertThatThrownBy(() -> service.criar(1L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inicio");
    }

    @Test
    @DisplayName("Um bloco novo nao pode cruzar com outro ja cadastrado no mesmo dia")
    void naoSobrepoeBlocoExistente() {
        HorarioProfessor existente = new HorarioProfessor();
        existente.setDiaSemana(DiaSemana.SEGUNDA);
        existente.setHoraInicio(LocalTime.of(8, 0));
        existente.setHoraFim(LocalTime.of(12, 0));
        given(repository.findByProfessorIdOrderByDiaSemanaAscHoraInicioAsc(1L)).willReturn(List.of(existente));

        var request = new HorarioProfessorDtos.Salvar(
                DiaSemana.SEGUNDA, LocalTime.of(10, 0), LocalTime.of(14, 0), 2L);

        assertThatThrownBy(() -> service.criar(1L, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cruza");
    }

    @Test
    @DisplayName("Blocos em dias diferentes nao conflitam, mesmo com o mesmo horario")
    void diasDiferentesNaoConflitam() {
        HorarioProfessor existente = new HorarioProfessor();
        existente.setDiaSemana(DiaSemana.SEGUNDA);
        existente.setHoraInicio(LocalTime.of(8, 0));
        existente.setHoraFim(LocalTime.of(12, 0));
        given(repository.findByProfessorIdOrderByDiaSemanaAscHoraInicioAsc(1L)).willReturn(List.of(existente));
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));

        var request = new HorarioProfessorDtos.Salvar(
                DiaSemana.TERCA, LocalTime.of(8, 0), LocalTime.of(12, 0), 2L);

        assertThat(service.criar(1L, request).diaSemana()).isEqualTo(DiaSemana.TERCA);
    }

    @Test
    @DisplayName("Remover confere que o bloco e do professor da rota, nao de outro")
    void removerConfereDono() {
        Usuario outroProfessor = new Usuario();
        outroProfessor.setId(5L);

        HorarioProfessor deOutro = new HorarioProfessor();
        deOutro.setId(30L);
        deOutro.setProfessor(outroProfessor);
        given(repository.findById(30L)).willReturn(Optional.of(deOutro));

        assertThatThrownBy(() -> service.remover(1L, 30L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Remover o proprio bloco funciona")
    void removerOProprioBloco() {
        HorarioProfessor doProfessor = new HorarioProfessor();
        doProfessor.setId(31L);
        doProfessor.setProfessor(professor);
        given(repository.findById(31L)).willReturn(Optional.of(doProfessor));

        service.remover(1L, 31L);

        verify(repository).delete(doProfessor);
    }
}
