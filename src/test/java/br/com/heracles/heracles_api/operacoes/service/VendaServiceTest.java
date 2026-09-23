package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.MetodoPagamento;
import br.com.heracles.heracles_api.operacoes.domain.Produto;
import br.com.heracles.heracles_api.operacoes.domain.Venda;
import br.com.heracles.heracles_api.operacoes.dto.LinhaFaturamentoPorMetodoPagamento;
import br.com.heracles.heracles_api.operacoes.dto.LinhaFaturamentoPorUnidade;
import br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoMaisVendido;
import br.com.heracles.heracles_api.operacoes.dto.VendaDtos;
import br.com.heracles.heracles_api.operacoes.repository.ProdutoRepository;
import br.com.heracles.heracles_api.operacoes.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
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
class VendaServiceTest {

    @Mock private VendaRepository vendaRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private VendaService service;
    private Unidade unidade;
    private Produto whey;

    @BeforeEach
    void preparar() {
        service = new VendaService(vendaRepository, produtoRepository, unidadeRepository, usuarioRepository);

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");

        whey = produto(10L, "Whey Protein 900g", new BigDecimal("149.90"), 3, unidade);

        Usuario operador = new Usuario();
        operador.setId(99L);
        operador.setNome("Carla Recepcao");
        operador.setEmail("carla@heracles.com.br");
        operador.setTipoPerfil(TipoPerfil.SECRETARIA);

        given(unidadeRepository.findById(1L)).willReturn(Optional.of(unidade));
        given(usuarioRepository.findByEmailIgnoreCase("carla@heracles.com.br")).willReturn(Optional.of(operador));
        given(produtoRepository.buscarParaVenda(10L)).willReturn(Optional.of(whey));
        given(vendaRepository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    private Produto produto(Long id, String nome, BigDecimal preco, int estoque, Unidade unidade) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPrecoVenda(preco);
        p.setQuantidadeEstoque(estoque);
        p.setUnidade(unidade);
        p.setAtivo(true);
        return p;
    }

    private VendaDtos.Registrar pedido(Long produtoId, int quantidade) {
        return new VendaDtos.Registrar(1L, null, MetodoPagamento.PIX,
                List.of(new VendaDtos.Registrar.Item(produtoId, quantidade)));
    }

    @Test
    @DisplayName("O total e calculado no servidor, a partir da tabela de precos")
    void totalCalculadoNoServidor() {
        VendaDtos.Response resposta = service.registrar(pedido(10L, 2), "carla@heracles.com.br");

        // 2 x 149,90 — o cliente nao envia preco nem total.
        assertThat(resposta.valorTotal()).isEqualByComparingTo("299.80");
        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.itens().get(0).precoUnitario()).isEqualByComparingTo("149.90");
    }

    @Test
    @DisplayName("A venda baixa o estoque do produto")
    void vendaBaixaEstoque() {
        service.registrar(pedido(10L, 2), "carla@heracles.com.br");
        assertThat(whey.getQuantidadeEstoque()).isEqualTo(1);
    }

    @Test
    @DisplayName("O produto e carregado com a linha travada, nao por findById")
    void usaTravaPessimista() {
        service.registrar(pedido(10L, 1), "carla@heracles.com.br");

        // É o que impede duas vendas simultâneas do último item: sem o
        // SELECT ... FOR UPDATE, ambas leem o mesmo saldo e ambas gravam.
        verify(produtoRepository).buscarParaVenda(10L);
        verify(produtoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Estoque insuficiente recusa a venda inteira")
    void estoqueInsuficiente() {
        assertThatThrownBy(() -> service.registrar(pedido(10L, 4), "carla@heracles.com.br"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        // Nada foi gravado e o estoque nao se moveu: nao existe venda parcial.
        verify(vendaRepository, never()).save(any());
        assertThat(whey.getQuantidadeEstoque()).isEqualTo(3);
    }

    @Test
    @DisplayName("O mesmo produto repetido no carrinho e somado, nao duplicado")
    void produtoRepetidoEhSomado() {
        VendaDtos.Registrar comRepeticao = new VendaDtos.Registrar(1L, null, MetodoPagamento.DINHEIRO,
                List.of(new VendaDtos.Registrar.Item(10L, 1), new VendaDtos.Registrar.Item(10L, 2)));

        VendaDtos.Response resposta = service.registrar(comRepeticao, "carla@heracles.com.br");

        // Uma linha por produto — que é o que o índice único da tabela exige.
        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.itens().get(0).quantidade()).isEqualTo(3);
        assertThat(whey.getQuantidadeEstoque()).isZero();
    }

    @Test
    @DisplayName("Produto fora de linha nao pode ser vendido")
    void produtoInativoNaoVende() {
        whey.setAtivo(false);

        assertThatThrownBy(() -> service.registrar(pedido(10L, 1), "carla@heracles.com.br"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fora de linha");
    }

    @Test
    @DisplayName("Produto de outra unidade nao entra na venda")
    void produtoDeOutraUnidade() {
        Unidade outra = new Unidade();
        outra.setId(2L);
        outra.setNome("Unidade Zona Sul");
        whey.setUnidade(outra);

        assertThatThrownBy(() -> service.registrar(pedido(10L, 1), "carla@heracles.com.br"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("outra unidade");
    }

    @Test
    @DisplayName("O operador vem do token, nao do corpo da requisicao")
    void operadorVemDoToken() {
        service.registrar(pedido(10L, 1), "carla@heracles.com.br");

        org.mockito.ArgumentCaptor<Venda> captor = org.mockito.ArgumentCaptor.forClass(Venda.class);
        verify(vendaRepository).save(captor.capture());
        assertThat(captor.getValue().getOperador().getEmail()).isEqualTo("carla@heracles.com.br");
    }

    // ---------------------------------------------------------------
    // Relatorio de vendas
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O ticket medio e o faturamento dividido pelo numero de vendas")
    void relatorioCalculaTicketMedio() {
        given(vendaRepository.faturamentoDesde(any())).willReturn(new BigDecimal("1000.00"));
        given(vendaRepository.countByDataVendaAfter(any())).willReturn(4L);
        given(vendaRepository.produtosMaisVendidosDesde(any(), any())).willReturn(
                List.of(new LinhaProdutoMaisVendido(10L, "Whey Protein 900g", 8L, new BigDecimal("1199.20"))));
        given(vendaRepository.faturamentoPorUnidadeDesde(any())).willReturn(
                List.of(new LinhaFaturamentoPorUnidade(1L, "Unidade Centro", new BigDecimal("1000.00"), 4L)));

        VendaDtos.PainelVendas relatorio = service.relatorio(30);

        assertThat(relatorio.dias()).isEqualTo(30);
        assertThat(relatorio.faturamentoTotal()).isEqualByComparingTo("1000.00");
        assertThat(relatorio.quantidadeVendas()).isEqualTo(4L);
        assertThat(relatorio.ticketMedio()).isEqualByComparingTo("250.00");
        assertThat(relatorio.maisVendidos()).hasSize(1);
        assertThat(relatorio.maisVendidos().get(0).produtoNome()).isEqualTo("Whey Protein 900g");
        assertThat(relatorio.porUnidade()).hasSize(1);
        assertThat(relatorio.porUnidade().get(0).ticketMedio()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Sem venda no periodo, o ticket medio e zero em vez de dividir por zero")
    void relatorioSemVendasTicketMedioZero() {
        given(vendaRepository.faturamentoDesde(any())).willReturn(BigDecimal.ZERO);
        given(vendaRepository.countByDataVendaAfter(any())).willReturn(0L);
        given(vendaRepository.produtosMaisVendidosDesde(any(), any())).willReturn(List.of());
        given(vendaRepository.faturamentoPorUnidadeDesde(any())).willReturn(List.of());
        given(vendaRepository.faturamentoPorMetodoPagamentoDesde(any())).willReturn(List.of());

        VendaDtos.PainelVendas relatorio = service.relatorio(30);

        assertThat(relatorio.ticketMedio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(relatorio.maisVendidos()).isEmpty();
        assertThat(relatorio.porUnidade()).isEmpty();
        assertThat(relatorio.porMetodoPagamento()).isEmpty();
    }

    @Test
    @DisplayName("O relatorio por forma de pagamento cruza unidade e metodo, com ticket medio proprio")
    void relatorioCalculaPorFormaDePagamento() {
        given(vendaRepository.faturamentoDesde(any())).willReturn(new BigDecimal("1000.00"));
        given(vendaRepository.countByDataVendaAfter(any())).willReturn(4L);
        given(vendaRepository.produtosMaisVendidosDesde(any(), any())).willReturn(List.of());
        given(vendaRepository.faturamentoPorUnidadeDesde(any())).willReturn(List.of());
        given(vendaRepository.faturamentoPorMetodoPagamentoDesde(any())).willReturn(List.of(
                new LinhaFaturamentoPorMetodoPagamento(
                        1L, "Unidade Centro", MetodoPagamento.PIX, new BigDecimal("600.00"), 3L),
                new LinhaFaturamentoPorMetodoPagamento(
                        1L, "Unidade Centro", MetodoPagamento.DINHEIRO, new BigDecimal("400.00"), 1L)));

        VendaDtos.PainelVendas relatorio = service.relatorio(30);

        assertThat(relatorio.porMetodoPagamento()).hasSize(2);
        VendaDtos.LinhaVendaPorMetodoPagamento pix = relatorio.porMetodoPagamento().get(0);
        assertThat(pix.metodoPagamento()).isEqualTo(MetodoPagamento.PIX);
        assertThat(pix.ticketMedio()).isEqualByComparingTo("200.00");
        VendaDtos.LinhaVendaPorMetodoPagamento dinheiro = relatorio.porMetodoPagamento().get(1);
        assertThat(dinheiro.ticketMedio()).isEqualByComparingTo("400.00");
    }
}
