package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.LinhaAlunoSemFicha;
import br.com.heracles.heracles_api.core.dto.ResumoAlunosSemFicha;
import br.com.heracles.heracles_api.core.dto.TreinoRequest;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/** Reconciliacao dos exercicios de uma ficha durante a edicao. */
@ExtendWith(MockitoExtension.class)
class TreinoServiceTest {

    @Mock
    private TreinoRepository repository;

    @Mock
    private HistoricoTreinoAlunoRepository historicoTreinoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TreinoService service;

    private Treino fichaExistente;

    @BeforeEach
    void prepararFicha() {
        fichaExistente = new Treino();
        fichaExistente.setId(1L);
        fichaExistente.setNome("Ficha A");
        fichaExistente.setFoco("Hipertrofia");
        fichaExistente.setNivel("Intermediario");
        fichaExistente.adicionarExercicio(exercicio(10L, "Supino Reto", 4, 10, 12));
        fichaExistente.adicionarExercicio(exercicio(11L, "Crucifixo", 3, 12, 12));
    }

    private Exercicio exercicio(Long id, String nome, int series, int min, int max) {
        Exercicio exercicio = new Exercicio();
        exercicio.setId(id);
        exercicio.setNome(nome);
        exercicio.setSeries(series);
        exercicio.setRepeticoesMin(min);
        exercicio.setRepeticoesMax(max);
        return exercicio;
    }

    @Test
    @DisplayName("Exercicio existente e atualizado no lugar, mantendo o mesmo id")
    void atualizaExercicioExistenteSemRecriar() {
        given(repository.findWithExerciciosById(1L)).willReturn(Optional.of(fichaExistente));

        TreinoResponse resposta = service.atualizar(1L, new TreinoRequest(
                "Ficha A", "Hipertrofia", "Avancado",
                List.of(new TreinoRequest.ExercicioRequest(
                        10L, "Supino Reto", 5, 8, 8, "Carga maxima", "Descanso de 2min"))));

        // A versao anterior fazia clear() numa colecao com orphanRemoval e
        // recolocava a instancia com o id original — apagar e persistir a mesma
        // linha no mesmo flush. Aqui a instancia gerenciada e reaproveitada.
        assertThat(resposta.exercicios()).hasSize(1);
        assertThat(resposta.exercicios().get(0).id()).isEqualTo(10L);
        assertThat(resposta.exercicios().get(0).series()).isEqualTo(5);
        assertThat(resposta.exercicios().get(0).repeticoesMin()).isEqualTo(8);
        assertThat(resposta.exercicios().get(0).repeticoesMax()).isEqualTo(8);
        assertThat(resposta.exercicios().get(0).carga()).isEqualTo("Carga maxima");
        assertThat(fichaExistente.getExercicios().get(0))
                .isSameAs(fichaExistente.getExercicios().get(0));
    }

    @Test
    @DisplayName("Exercicio ausente na requisicao e removido da ficha")
    void removeExercicioQueSaiuDaLista() {
        given(repository.findWithExerciciosById(1L)).willReturn(Optional.of(fichaExistente));

        TreinoResponse resposta = service.atualizar(1L, new TreinoRequest(
                "Ficha A", "Hipertrofia", "Intermediario",
                List.of(new TreinoRequest.ExercicioRequest(
                        11L, "Crucifixo", 3, 12, 12, null, null))));

        assertThat(resposta.exercicios()).hasSize(1);
        assertThat(resposta.exercicios().get(0).id()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Exercicio sem id entra como novo e recebe a ordem da posicao")
    void insereExercicioNovoComOrdem() {
        given(repository.findWithExerciciosById(1L)).willReturn(Optional.of(fichaExistente));

        TreinoResponse resposta = service.atualizar(1L, new TreinoRequest(
                "Ficha A", "Hipertrofia", "Intermediario",
                List.of(
                        new TreinoRequest.ExercicioRequest(null, "Desenvolvimento", 4, 10, 10, null, null),
                        new TreinoRequest.ExercicioRequest(10L, "Supino Reto", 4, 10, 12, null, null))));

        assertThat(resposta.exercicios()).extracting(TreinoResponse.ExercicioResponse::nome)
                .containsExactly("Desenvolvimento", "Supino Reto");
        // A ordem segue a posicao enviada, nao o id: a sequencia da prescricao e preservada.
        assertThat(resposta.exercicios()).extracting(TreinoResponse.ExercicioResponse::ordem)
                .containsExactly(0, 1);
    }

    @Test
    @DisplayName("Id de exercicio de outra ficha nao sequestra o registro alheio")
    void idDeOutraFichaEhTratadoComoNovo() {
        given(repository.findWithExerciciosById(1L)).willReturn(Optional.of(fichaExistente));

        TreinoResponse resposta = service.atualizar(1L, new TreinoRequest(
                "Ficha A", "Hipertrofia", "Intermediario",
                List.of(new TreinoRequest.ExercicioRequest(
                        9999L, "Exercicio intruso", 3, 10, 10, null, null))));

        assertThat(resposta.exercicios()).hasSize(1);
        assertThat(resposta.exercicios().get(0).id()).isNull();
    }

    @Test
    @DisplayName("Ficha inexistente lanca RecursoNaoEncontradoException")
    void fichaInexistente() {
        given(repository.findWithExerciciosById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(404L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("Apagar a ficha fecha o periodo de quem ainda estava com ela")
    void apagarFichaFechaHistoricoEmAberto() {
        given(repository.findById(1L)).willReturn(Optional.of(fichaExistente));

        HistoricoTreinoAluno aberto = new HistoricoTreinoAluno();
        aberto.setTreino(fichaExistente);
        aberto.setVinculadoEm(LocalDateTime.now().minusDays(30));
        given(historicoTreinoRepository.buscarAbertosPorTreino(1L)).willReturn(List.of(aberto));

        // A exclusao desfaz o vinculo direto na colecao, sem passar por
        // sincronizarTreinos — se o servico nao fechar o periodo aqui,
        // ninguem mais fecharia.
        service.deletar(1L);

        assertThat(aberto.estaAberto()).isFalse();
        assertThat(aberto.getDesvinculadoEm()).isNotNull();
        // A referencia precisa ser desfeita no mesmo flush em que a ficha e
        // apagada — o Hibernate recusa a transacao se uma entidade que esta
        // sendo atualizada ainda apontar para outra que esta sendo removida.
        assertThat(aberto.getTreino()).isNull();
    }

    // ---------------------------------------------------------------
    // Alerta de alunos sem ficha de treino
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O resumo repassa a contagem do repositorio")
    void resumoAlunosSemFichaRepassaContagem() {
        given(usuarioRepository.countAlunosSemFichaDeTreino()).willReturn(4L);

        ResumoAlunosSemFicha resumo = service.resumoAlunosSemFicha();

        assertThat(resumo.total()).isEqualTo(4L);
    }

    @Test
    @DisplayName("O alerta traz o aluno da consulta, mapeado para a linha")
    void alunosSemFichaMapeiaLinha() {
        Usuario aluno = new Usuario();
        aluno.setId(7L);
        aluno.setNome("Diego Ramos");
        aluno.setEmail("diego@ex.com");
        aluno.setTelefone("11999990000");
        aluno.setDataCadastro(LocalDateTime.now().minusDays(40));
        given(usuarioRepository.buscarAlunosSemFichaDeTreino(any()))
                .willReturn(new PageImpl<>(List.of(aluno)));

        LinhaAlunoSemFicha linha = service.alunosSemFicha(PageRequest.of(0, 20)).getContent().get(0);

        assertThat(linha.alunoId()).isEqualTo(7L);
        assertThat(linha.alunoNome()).isEqualTo("Diego Ramos");
        assertThat(linha.email()).isEqualTo("diego@ex.com");
        assertThat(linha.telefone()).isEqualTo("11999990000");
    }
}
