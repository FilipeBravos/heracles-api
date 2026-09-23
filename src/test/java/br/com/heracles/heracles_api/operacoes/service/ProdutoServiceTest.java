package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.operacoes.domain.Produto;
import br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoParado;
import br.com.heracles.heracles_api.operacoes.dto.LinhaUltimaVendaProduto;
import br.com.heracles.heracles_api.operacoes.dto.ProdutoDtos;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProdutoServiceTest {

    @Mock private ProdutoRepository repository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private VendaRepository vendaRepository;

    private ProdutoService service;
    private Unidade unidade;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(repository, unidadeRepository, vendaRepository);

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");

        given(unidadeRepository.findById(1L)).willReturn(Optional.of(unidade));
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    private Produto produto(Long id, String nome, int quantidadeEstoque, int estoqueMinimo) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setMarca("Marca X");
        p.setPrecoVenda(new BigDecimal("29.90"));
        p.setQuantidadeEstoque(quantidadeEstoque);
        p.setEstoqueMinimo(estoqueMinimo);
        p.setUnidade(unidade);
        p.setAtivo(true);
        return p;
    }

    @Test
    @DisplayName("Criar grava o estoque minimo informado no cadastro")
    void criarGravaEstoqueMinimo() {
        ProdutoDtos.Response criado = service.criar(new ProdutoDtos.Request(
                1L, "Creatina 300g", "Marca X", new BigDecimal("89.90"), 10, 4));

        assertThat(criado.estoqueMinimo()).isEqualTo(4);
    }

    @Test
    @DisplayName("Atualizar sobrescreve o estoque minimo, diferente da quantidade em estoque")
    void atualizarSobrescreveEstoqueMinimo() {
        Produto existente = produto(5L, "Creatina 300g", 10, 4);
        given(repository.findWithUnidadeById(5L)).willReturn(Optional.of(existente));

        ProdutoDtos.Response atualizado = service.atualizar(5L, new ProdutoDtos.Request(
                1L, "Creatina 300g", "Marca X", new BigDecimal("89.90"), 10, 8));

        assertThat(atualizado.estoqueMinimo()).isEqualTo(8);
        // A quantidade em estoque, ao contrario, nao muda pela edicao do cadastro.
        assertThat(atualizado.quantidadeEstoque()).isEqualTo(10);
    }

    @Test
    @DisplayName("Reposicao de estoque mapeia a linha com a quantidade sugerida")
    void reposicaoEstoqueMapeiaLinha() {
        Produto whey = produto(10L, "Whey Protein 900g", 2, 5);
        given(repository.buscarComEstoqueBaixo()).willReturn(List.of(whey));

        List<ProdutoDtos.LinhaReposicao> reposicao = service.reposicaoEstoque();

        assertThat(reposicao).hasSize(1);
        ProdutoDtos.LinhaReposicao linha = reposicao.get(0);
        assertThat(linha.produtoNome()).isEqualTo("Whey Protein 900g");
        assertThat(linha.quantidadeEstoque()).isEqualTo(2);
        assertThat(linha.estoqueMinimo()).isEqualTo(5);
        // 5 - 2 = 3 unidades pra completar o minimo.
        assertThat(linha.quantidadeSugerida()).isEqualTo(3);
    }

    @Test
    @DisplayName("Sem produto abaixo do minimo, a reposicao vem vazia")
    void reposicaoEstoqueVaziaSemDeficit() {
        given(repository.buscarComEstoqueBaixo()).willReturn(List.of());

        assertThat(service.reposicaoEstoque()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Produtos parados
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Produto sem venda ha muito tempo entra na lista, com a data da ultima venda")
    void produtoSemVendaRecenteEntraNaLista() {
        Produto creatina = produto(10L, "Creatina 300g", 20, 5);
        given(repository.findByAtivoTrue()).willReturn(List.of(creatina));
        LocalDateTime ultimaVenda = LocalDateTime.now().minusDays(200);
        given(vendaRepository.ultimaVendaPorProduto()).willReturn(
                List.of(new LinhaUltimaVendaProduto(10L, ultimaVenda)));

        List<LinhaProdutoParado> parados = service.produtosParados(90);

        assertThat(parados).hasSize(1);
        assertThat(parados.get(0).produtoNome()).isEqualTo("Creatina 300g");
        assertThat(parados.get(0).ultimaVenda()).isEqualTo(ultimaVenda.toLocalDate());
        assertThat(parados.get(0).diasParado()).isEqualTo(200);
    }

    @Test
    @DisplayName("Produto que vendeu dentro do limiar nao entra na lista")
    void produtoComVendaRecenteNaoEntra() {
        Produto creatina = produto(10L, "Creatina 300g", 20, 5);
        given(repository.findByAtivoTrue()).willReturn(List.of(creatina));
        given(vendaRepository.ultimaVendaPorProduto()).willReturn(
                List.of(new LinhaUltimaVendaProduto(10L, LocalDateTime.now().minusDays(5))));

        assertThat(service.produtosParados(90)).isEmpty();
    }

    @Test
    @DisplayName("Produto que nunca vendeu conta os dias a partir do proprio cadastro")
    void produtoSemNenhumaVendaContaDoCadastro() {
        Produto novo = produto(11L, "Barra de Proteina", 10, 5);
        novo.setCadastradoEm(LocalDateTime.now().minusDays(200));
        given(repository.findByAtivoTrue()).willReturn(List.of(novo));
        given(vendaRepository.ultimaVendaPorProduto()).willReturn(List.of());

        List<LinhaProdutoParado> parados = service.produtosParados(90);

        assertThat(parados).hasSize(1);
        assertThat(parados.get(0).ultimaVenda()).isNull();
        assertThat(parados.get(0).diasParado()).isEqualTo(200);
    }

    @Test
    @DisplayName("Produto recem-cadastrado e sem venda nao entra, mesmo sem nenhuma venda registrada")
    void produtoRecemCadastradoNaoEntra() {
        Produto novo = produto(11L, "Barra de Proteina", 10, 5);
        novo.setCadastradoEm(LocalDateTime.now().minusDays(5));
        given(repository.findByAtivoTrue()).willReturn(List.of(novo));
        given(vendaRepository.ultimaVendaPorProduto()).willReturn(List.of());

        assertThat(service.produtosParados(90)).isEmpty();
    }

    @Test
    @DisplayName("Ordena do mais parado pro menos parado")
    void ordenaDoMaisParadoProMenos() {
        Produto creatina = produto(10L, "Creatina 300g", 20, 5);
        Produto whey = produto(12L, "Whey Protein 900g", 15, 5);
        given(repository.findByAtivoTrue()).willReturn(List.of(creatina, whey));
        given(vendaRepository.ultimaVendaPorProduto()).willReturn(List.of(
                new LinhaUltimaVendaProduto(10L, LocalDateTime.now().minusDays(100)),
                new LinhaUltimaVendaProduto(12L, LocalDateTime.now().minusDays(300))));

        List<LinhaProdutoParado> parados = service.produtosParados(90);

        assertThat(parados).extracting(LinhaProdutoParado::produtoId).containsExactly(12L, 10L);
    }
}
