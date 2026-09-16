package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.dto.TreinoRequest;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/** Reconciliacao dos exercicios de uma ficha durante a edicao. */
@ExtendWith(MockitoExtension.class)
class TreinoServiceTest {

    @Mock
    private TreinoRepository repository;

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
}
