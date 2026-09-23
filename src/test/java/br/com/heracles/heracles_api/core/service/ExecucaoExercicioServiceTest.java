package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.ExecucaoExercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.ExecucaoExercicioDtos;
import br.com.heracles.heracles_api.core.repository.ExecucaoExercicioRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExecucaoExercicioServiceTest {

    @Mock private ExecucaoExercicioRepository repository;
    @Mock private TreinoRepository treinoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private ExecucaoExercicioService service;
    private Usuario marina;
    private Exercicio supino;

    @BeforeEach
    void preparar() {
        service = new ExecucaoExercicioService(repository, treinoRepository, usuarioRepository);

        marina = new Usuario();
        marina.setId(10L);
        marina.setEmail("marina@ex.com");

        supino = new Exercicio();
        supino.setId(5L);
        supino.setNome("Supino reto");

        Treino treino = new Treino();
        treino.setId(1L);
        treino.getExercicios().add(supino);

        given(usuarioRepository.findByEmailIgnoreCase("marina@ex.com")).willReturn(Optional.of(marina));
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of(treino));
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Registra a execucao de um exercicio da ficha atual do aluno")
    void registrarSalvaExecucaoDaFichaAtual() {
        ExecucaoExercicioDtos.Request request = new ExecucaoExercicioDtos.Request(
                5L, LocalDate.of(2026, 3, 1), 4, 10, new BigDecimal("60.00"), "Boa execucao");

        ExecucaoExercicioDtos.Response resposta = service.registrar("marina@ex.com", request);

        assertThat(resposta.exercicioId()).isEqualTo(5L);
        assertThat(resposta.exercicioNome()).isEqualTo("Supino reto");
        assertThat(resposta.seriesRealizadas()).isEqualTo(4);
        assertThat(resposta.repeticoesRealizadas()).isEqualTo(10);
        assertThat(resposta.cargaRealizada()).isEqualByComparingTo("60.00");

        ArgumentCaptor<ExecucaoExercicio> captor = ArgumentCaptor.forClass(ExecucaoExercicio.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAluno()).isEqualTo(marina);
        assertThat(captor.getValue().getExercicio()).isEqualTo(supino);
        // O nome fica copiado no registro, independente do exercicio sair da ficha depois.
        assertThat(captor.getValue().getExercicioNome()).isEqualTo("Supino reto");
    }

    @Test
    @DisplayName("Observacao em branco vira nulo, para nao guardar string vazia")
    void observacaoEmBrancoViraNulo() {
        ExecucaoExercicioDtos.Request request = new ExecucaoExercicioDtos.Request(
                5L, LocalDate.of(2026, 3, 1), 4, 10, null, "   ");

        ExecucaoExercicioDtos.Response resposta = service.registrar("marina@ex.com", request);

        assertThat(resposta.observacao()).isNull();
        assertThat(resposta.cargaRealizada()).isNull();
    }

    @Test
    @DisplayName("Rejeita exercicio que nao esta em nenhuma ficha atual do aluno")
    void rejeitaExercicioForaDasFichasAtuais() {
        ExecucaoExercicioDtos.Request request = new ExecucaoExercicioDtos.Request(
                999L, LocalDate.of(2026, 3, 1), 4, 10, null, null);

        assertThatThrownBy(() -> service.registrar("marina@ex.com", request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao esta em nenhuma das suas fichas atuais");
    }

    @Test
    @DisplayName("A evolucao vem do mais recente pro mais antigo, como o repositorio devolve")
    void minhasExecucoesMapeiaNaOrdemDoRepositorio() {
        ExecucaoExercicio execucao = new ExecucaoExercicio();
        execucao.setId(1L);
        execucao.setAluno(marina);
        execucao.setExercicio(supino);
        execucao.setExercicioNome("Supino reto");
        execucao.setDataExecucao(LocalDate.of(2026, 3, 8));
        execucao.setSeriesRealizadas(4);
        execucao.setRepeticoesRealizadas(8);
        execucao.setCargaRealizada(new BigDecimal("65.00"));

        given(repository.findByAlunoIdAndExercicioIdOrderByDataExecucaoDesc(10L, 5L))
                .willReturn(List.of(execucao));

        List<ExecucaoExercicioDtos.Response> resultado = service.minhasExecucoes("marina@ex.com", 5L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).cargaRealizada()).isEqualByComparingTo("65.00");
    }
}
