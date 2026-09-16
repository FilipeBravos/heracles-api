package br.com.heracles.heracles_api.matriculas.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.domain.TipoCobranca;
import br.com.heracles.heracles_api.matriculas.dto.PlanoDtos;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanoServiceTest {

    @Mock private PlanoRepository repository;
    @Mock private UnidadeRepository unidadeRepository;

    private PlanoService service;
    private Unidade centro;
    private Unidade zonaSul;

    @BeforeEach
    void preparar() {
        service = new PlanoService(repository, unidadeRepository);

        centro = new Unidade();
        centro.setId(1L);
        centro.setNome("Unidade Centro");

        zonaSul = new Unidade();
        zonaSul.setId(2L);
        zonaSul.setNome("Unidade Zona Sul");

        given(unidadeRepository.findById(1L)).willReturn(Optional.of(centro));
        given(unidadeRepository.findById(2L)).willReturn(Optional.of(zonaSul));
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Plano de rede guarda todas as unidades escolhidas")
    void planoDeRedeGuardaUnidades() {
        PlanoDtos.Response criado = service.criar(new PlanoDtos.Request(
                "  Rede Total  ", new BigDecimal("199.90"), TipoCobranca.PACOTE_ANUAL, ordenadas(1L, 2L)));

        assertThat(criado.nome()).isEqualTo("Rede Total");
        assertThat(criado.unidades()).extracting(PlanoDtos.UnidadeResumo::nome)
                .containsExactlyInAnyOrder("Unidade Centro", "Unidade Zona Sul");
        assertThat(criado.ativo()).isTrue();
    }

    @Test
    @DisplayName("Unidade inexistente derruba o cadastro em vez de sumir da lista")
    void unidadeInexistenteNaoEhIgnorada() {
        given(unidadeRepository.findById(404L)).willReturn(Optional.empty());

        // Com findAllById, o plano seria salvo cobrindo menos unidades do
        // que o operador escolheu, sem aviso nenhum.
        assertThatThrownBy(() -> service.criar(new PlanoDtos.Request(
                "Rede Total", new BigDecimal("199.90"), TipoCobranca.RECORRENTE, ordenadas(1L, 404L))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Unidade");
    }

    @Test
    @DisplayName("Dois planos com o mesmo nome sao recusados")
    void nomeDuplicadoEhRecusado() {
        given(repository.existsByNomeIgnoreCase("Mensal Centro")).willReturn(true);

        assertThatThrownBy(() -> service.criar(new PlanoDtos.Request(
                "Mensal Centro", new BigDecimal("129.90"), TipoCobranca.RECORRENTE, ordenadas(1L))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Ja existe um plano");
    }

    @Test
    @DisplayName("Tirar de linha e reversivel e nao apaga o plano")
    void tirarDeLinhaEhReversivel() {
        Plano plano = new Plano();
        plano.setId(3L);
        plano.setNome("Mensal Centro");
        plano.setValorMensal(new BigDecimal("129.90"));
        plano.setTipoCobranca(TipoCobranca.RECORRENTE);
        plano.setUnidades(new LinkedHashSet<>(Set.of(centro)));
        given(repository.findWithUnidadesById(3L)).willReturn(Optional.of(plano));

        assertThat(service.alternarAtivo(3L).ativo()).isFalse();
        assertThat(service.alternarAtivo(3L).ativo()).isTrue();
    }

    private Set<Long> ordenadas(Long... ids) {
        return new LinkedHashSet<>(java.util.Arrays.asList(ids));
    }
}
