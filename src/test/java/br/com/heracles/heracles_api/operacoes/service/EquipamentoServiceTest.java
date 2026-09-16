package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.*;
import br.com.heracles.heracles_api.operacoes.dto.EquipamentoDtos;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

        service.atualizar(7L, new EquipamentoDtos.Request(1L, "Esteira 03 - Profissional"));

        // Renomear um aparelho não pode devolvê-lo à operação por acidente.
        assertThat(esteira.getNome()).isEqualTo("Esteira 03 - Profissional");
        assertThat(esteira.getStatusAtual()).isEqualTo(StatusEquipamento.EM_MANUTENCAO);
    }
}
