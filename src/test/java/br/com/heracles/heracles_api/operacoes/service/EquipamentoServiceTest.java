package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.*;
import br.com.heracles.heracles_api.operacoes.dto.EquipamentoDtos;
import br.com.heracles.heracles_api.operacoes.dto.LinhaEquipamentoProblematico;
import br.com.heracles.heracles_api.operacoes.dto.LinhaManutencaoPorUnidade;
import br.com.heracles.heracles_api.operacoes.dto.LinhaManutencaoPreventiva;
import br.com.heracles.heracles_api.operacoes.dto.LinhaTempoResolucao;
import br.com.heracles.heracles_api.operacoes.dto.LinhaUltimaManutencao;
import br.com.heracles.heracles_api.operacoes.repository.ChamadoManutencaoRepository;
import br.com.heracles.heracles_api.operacoes.repository.EquipamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EquipamentoServiceTest {

    @Mock private EquipamentoRepository repository;
    @Mock private ChamadoManutencaoRepository chamadoRepository;
    @Mock private UnidadeRepository unidadeRepository;

    private EquipamentoService service;
    private Equipamento esteira;

    @BeforeEach
    void preparar() {
        service = new EquipamentoService(repository, chamadoRepository, unidadeRepository);

        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");

        esteira = new Equipamento();
        esteira.setId(7L);
        esteira.setNome("Esteira 03");
        esteira.setUnidade(unidade);
        esteira.setStatusAtual(StatusEquipamento.OK);

        given(repository.findWithUnidadeById(7L)).willReturn(Optional.of(esteira));
        given(chamadoRepository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Abrir chamado tira o equipamento de operacao")
    void abrirChamadoTiraDeOperacao() {
        given(chamadoRepository.findByEquipamentoIdAndStatus(7L, StatusChamado.ABERTO))
                .willReturn(Optional.empty());

        EquipamentoDtos.ChamadoResponse chamado = service.abrirChamado(
                7L, new EquipamentoDtos.AbrirChamado("Lona patinando sob carga"));

        // As duas coisas andam juntas: um aparelho com chamado aberto que
        // continuasse marcado como OK é o que faz alguém subir nele.
        assertThat(chamado.status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(esteira.getStatusAtual()).isEqualTo(StatusEquipamento.EM_MANUTENCAO);
        assertThat(chamado.dataResolucao()).isNull();
        assertThat(chamado.custoReparo()).isNull();
    }

    @Test
    @DisplayName("Nao abre um segundo chamado com um ja aberto")
    void naoAbreChamadoDuplicado() {
        ChamadoManutencao aberto = new ChamadoManutencao();
        aberto.setStatus(StatusChamado.ABERTO);
        given(chamadoRepository.findByEquipamentoIdAndStatus(7L, StatusChamado.ABERTO))
                .willReturn(Optional.of(aberto));

        assertThatThrownBy(() -> service.abrirChamado(
                7L, new EquipamentoDtos.AbrirChamado("Outro problema")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja tem um chamado aberto");
    }

    @Test
    @DisplayName("Resolver devolve o equipamento a operacao e registra o custo")
    void resolverDevolveAOperacao() {
        ChamadoManutencao chamado = new ChamadoManutencao();
        chamado.setId(55L);
        chamado.setEquipamento(esteira);
        chamado.setStatus(StatusChamado.ABERTO);
        esteira.setStatusAtual(StatusEquipamento.EM_MANUTENCAO);
        given(chamadoRepository.findById(55L)).willReturn(Optional.of(chamado));

        EquipamentoDtos.ChamadoResponse resolvido = service.resolverChamado(
                55L, new EquipamentoDtos.ResolverChamado(new BigDecimal("480.00")));

        assertThat(resolvido.status()).isEqualTo(StatusChamado.RESOLVIDO);
        assertThat(resolvido.custoReparo()).isEqualByComparingTo("480.00");
        assertThat(resolvido.dataResolucao()).isNotNull();
        assertThat(esteira.getStatusAtual()).isEqualTo(StatusEquipamento.OK);
    }

    @Test
    @DisplayName("Chamado ja resolvido nao se resolve de novo")
    void naoResolveDuasVezes() {
        ChamadoManutencao chamado = new ChamadoManutencao();
        chamado.setId(55L);
        chamado.setEquipamento(esteira);
        chamado.setStatus(StatusChamado.RESOLVIDO);
        given(chamadoRepository.findById(55L)).willReturn(Optional.of(chamado));

        assertThatThrownBy(() -> service.resolverChamado(
                55L, new EquipamentoDtos.ResolverChamado(BigDecimal.ZERO)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja foi resolvido");
    }

    @Test
    @DisplayName("O status nao e editavel pelo cadastro do equipamento")
    void statusNaoEhEditavelPeloCadastro() {
        given(unidadeRepository.findById(1L)).willReturn(Optional.of(esteira.getUnidade()));
        esteira.setStatusAtual(StatusEquipamento.EM_MANUTENCAO);

        service.atualizar(7L, new EquipamentoDtos.Request(1L, "Esteira 03 - Profissional", null));

        // Renomear um aparelho não pode devolvê-lo à operação por acidente.
        assertThat(esteira.getNome()).isEqualTo("Esteira 03 - Profissional");
        assertThat(esteira.getStatusAtual()).isEqualTo(StatusEquipamento.EM_MANUTENCAO);
    }

    // ---------------------------------------------------------------
    // Relatorio de manutencao
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O tempo medio de resolucao e a media das duracoes entre abertura e resolucao")
    void relatorioCalculaTempoMedioDeResolucao() {
        given(chamadoRepository.countByDataChamadoAfter(any())).willReturn(3L);
        given(chamadoRepository.countByDataChamadoAfterAndStatus(any(), eq(StatusChamado.ABERTO))).willReturn(1L);
        given(chamadoRepository.custoTotalDesde(any())).willReturn(new BigDecimal("980.00"));
        given(chamadoRepository.temposResolucaoDesde(any())).willReturn(List.of(
                new LinhaTempoResolucao(LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 12, 0)),
                new LinhaTempoResolucao(LocalDateTime.of(2026, 1, 2, 8, 0), LocalDateTime.of(2026, 1, 3, 8, 0))));
        given(chamadoRepository.equipamentosProblematicosDesde(any(), any())).willReturn(
                List.of(new LinhaEquipamentoProblematico(7L, "Esteira 03", "Unidade Centro", 2L, new BigDecimal("980.00"))));
        given(chamadoRepository.manutencaoPorUnidadeDesde(any())).willReturn(
                List.of(new LinhaManutencaoPorUnidade(1L, "Unidade Centro", 2L, new BigDecimal("980.00"))));

        EquipamentoDtos.PainelManutencao relatorio = service.relatorio(90);

        assertThat(relatorio.dias()).isEqualTo(90);
        assertThat(relatorio.quantidadeChamados()).isEqualTo(3L);
        assertThat(relatorio.quantidadeAbertos()).isEqualTo(1L);
        assertThat(relatorio.custoTotal()).isEqualByComparingTo("980.00");
        // (4h + 24h) / 2 = 14h
        assertThat(relatorio.tempoMedioResolucaoHoras()).isEqualByComparingTo("14.0");
        assertThat(relatorio.maisProblematicos()).hasSize(1);
        assertThat(relatorio.maisProblematicos().get(0).equipamentoNome()).isEqualTo("Esteira 03");
        assertThat(relatorio.porUnidade()).hasSize(1);
    }

    @Test
    @DisplayName("Sem chamado resolvido no periodo, o tempo medio e zero em vez de dividir por zero")
    void relatorioSemChamadoResolvidoTempoMedioZero() {
        given(chamadoRepository.countByDataChamadoAfter(any())).willReturn(0L);
        given(chamadoRepository.countByDataChamadoAfterAndStatus(any(), eq(StatusChamado.ABERTO))).willReturn(0L);
        given(chamadoRepository.custoTotalDesde(any())).willReturn(BigDecimal.ZERO);
        given(chamadoRepository.temposResolucaoDesde(any())).willReturn(List.of());
        given(chamadoRepository.equipamentosProblematicosDesde(any(), any())).willReturn(List.of());
        given(chamadoRepository.manutencaoPorUnidadeDesde(any())).willReturn(List.of());

        EquipamentoDtos.PainelManutencao relatorio = service.relatorio(90);

        assertThat(relatorio.tempoMedioResolucaoHoras()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(relatorio.maisProblematicos()).isEmpty();
        assertThat(relatorio.porUnidade()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Manutencao preventiva
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Sem chamado resolvido, a ancora e o cadastro do equipamento")
    void relatorioUsaCadastroComoAncoraSemChamadoResolvido() {
        esteira.setIntervaloDiasManutencao(30);
        esteira.setCadastradoEm(LocalDateTime.now().minusDays(40));
        given(repository.findByIntervaloDiasManutencaoIsNotNull()).willReturn(List.of(esteira));
        given(chamadoRepository.ultimaResolucaoPorEquipamento()).willReturn(List.of());

        List<LinhaManutencaoPreventiva> relatorio = service.relatorioManutencaoPreventiva();

        assertThat(relatorio).hasSize(1);
        assertThat(relatorio.get(0).equipamentoId()).isEqualTo(7L);
        // 40 dias desde o cadastro, intervalo de 30: 10 dias de atraso.
        assertThat(relatorio.get(0).diasAtraso()).isEqualTo(10);
    }

    @Test
    @DisplayName("Com chamado resolvido, a ancora e a resolucao mais recente, nao o cadastro")
    void relatorioPrefereUltimaResolucaoAoCadastro() {
        esteira.setIntervaloDiasManutencao(30);
        // Cadastro antigo demais faria parecer vencido; a resolucao recente diz que nao esta.
        esteira.setCadastradoEm(LocalDateTime.now().minusDays(200));
        given(repository.findByIntervaloDiasManutencaoIsNotNull()).willReturn(List.of(esteira));
        given(chamadoRepository.ultimaResolucaoPorEquipamento()).willReturn(
                List.of(new LinhaUltimaManutencao(7L, LocalDateTime.now().minusDays(10))));

        List<LinhaManutencaoPreventiva> relatorio = service.relatorioManutencaoPreventiva();

        // 10 dias desde a resolucao, intervalo de 30: ainda nao venceu.
        assertThat(relatorio).isEmpty();
    }

    @Test
    @DisplayName("Ordena do mais atrasado pro menos atrasado")
    void relatorioOrdenaDoMaisAtrasadoProMenos() {
        Equipamento leg = new Equipamento();
        leg.setId(9L);
        leg.setNome("Leg Press");
        leg.setUnidade(esteira.getUnidade());
        leg.setIntervaloDiasManutencao(30);
        leg.setCadastradoEm(LocalDateTime.now().minusDays(35));

        esteira.setIntervaloDiasManutencao(30);
        esteira.setCadastradoEm(LocalDateTime.now().minusDays(60));

        given(repository.findByIntervaloDiasManutencaoIsNotNull()).willReturn(List.of(leg, esteira));
        given(chamadoRepository.ultimaResolucaoPorEquipamento()).willReturn(List.of());

        List<LinhaManutencaoPreventiva> relatorio = service.relatorioManutencaoPreventiva();

        assertThat(relatorio).extracting(LinhaManutencaoPreventiva::equipamentoId)
                .containsExactly(7L, 9L);
    }
}
