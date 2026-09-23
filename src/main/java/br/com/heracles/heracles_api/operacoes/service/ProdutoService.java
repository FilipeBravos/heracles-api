package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.Produto;
import br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoParado;
import br.com.heracles.heracles_api.operacoes.dto.LinhaUltimaVendaProduto;
import br.com.heracles.heracles_api.operacoes.dto.ProdutoDtos;
import br.com.heracles.heracles_api.operacoes.repository.ProdutoRepository;
import br.com.heracles.heracles_api.operacoes.repository.VendaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    /** Mesmo periodo padrao ja usado nos outros relatorios do sistema. */
    public static final int DIAS_PARADO_PADRAO = 90;

    private final ProdutoRepository repository;
    private final UnidadeRepository unidadeRepository;
    private final VendaRepository vendaRepository;

    public ProdutoService(ProdutoRepository repository, UnidadeRepository unidadeRepository,
                          VendaRepository vendaRepository) {
        this.repository = repository;
        this.unidadeRepository = unidadeRepository;
        this.vendaRepository = vendaRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProdutoDtos.Response> listar(boolean apenasAtivos, Pageable pageable) {
        return repository.buscarPaginado(apenasAtivos, pageable).map(ProdutoDtos.Response::de);
    }

    @Transactional(readOnly = true)
    public ProdutoDtos.Response buscarPorId(Long id) {
        return ProdutoDtos.Response.de(carregar(id));
    }

    @Transactional
    public ProdutoDtos.Response criar(ProdutoDtos.Request request) {
        Unidade unidade = carregarUnidade(request.unidadeId());

        if (repository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), request.nome())) {
            throw new RegraNegocioException("Ja existe um produto com esse nome nesta unidade.");
        }

        Produto produto = new Produto();
        produto.setUnidade(unidade);
        aplicar(request, produto);
        produto.setQuantidadeEstoque(request.quantidadeEstoque());

        return ProdutoDtos.Response.de(repository.save(produto));
    }

    @Transactional
    public ProdutoDtos.Response atualizar(Long id, ProdutoDtos.Request request) {
        Produto produto = carregar(id);
        Unidade unidade = carregarUnidade(request.unidadeId());

        if (repository.existsByUnidadeIdAndNomeIgnoreCaseAndIdNot(unidade.getId(), request.nome(), id)) {
            throw new RegraNegocioException("Ja existe outro produto com esse nome nesta unidade.");
        }

        produto.setUnidade(unidade);
        aplicar(request, produto);
        // O estoque nao e reescrito pela edicao do cadastro: ele se move por
        // entrada e por venda. Sobrescrever aqui apagaria silenciosamente
        // uma venda registrada entre a abertura do formulario e o salvamento.
        return ProdutoDtos.Response.de(produto);
    }

    /** Entrada de mercadoria: soma ao saldo atual. */
    @Transactional
    public ProdutoDtos.Response registrarEntrada(Long id, ProdutoDtos.AjusteEstoque ajuste) {
        Produto produto = carregar(id);
        produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + ajuste.quantidade());
        return ProdutoDtos.Response.de(produto);
    }

    /**
     * Produto sai de linha em vez de ser apagado: o historico de vendas
     * aponta para ele. Reversivel.
     */
    @Transactional
    public ProdutoDtos.Response alternarAtivo(Long id) {
        Produto produto = carregar(id);
        produto.setAtivo(!produto.isAtivo());
        return ProdutoDtos.Response.de(produto);
    }

    /** A sugestao de reposicao: produtos ativos abaixo do proprio estoque minimo, do maior deficit pro menor. */
    @Transactional(readOnly = true)
    public List<ProdutoDtos.LinhaReposicao> reposicaoEstoque() {
        return repository.buscarComEstoqueBaixo().stream().map(ProdutoDtos.LinhaReposicao::de).toList();
    }

    /**
     * O oposto da reposicao de estoque: produtos ativos sem venda ha pelo
     * menos `diasParado`, do mais parado pro menos. Quem nunca vendeu conta
     * a partir do proprio cadastro, nao de uma venda que nunca aconteceu.
     */
    @Transactional(readOnly = true)
    public List<LinhaProdutoParado> produtosParados(int diasParado) {
        List<Produto> produtos = repository.findByAtivoTrue();
        Map<Long, LocalDateTime> ultimasVendas = vendaRepository.ultimaVendaPorProduto().stream()
                .collect(Collectors.toMap(LinhaUltimaVendaProduto::produtoId, LinhaUltimaVendaProduto::ultimaVenda));

        LocalDate hoje = LocalDate.now();

        return produtos.stream()
                .map(produto -> {
                    LocalDateTime ultimaVenda = ultimasVendas.get(produto.getId());
                    LocalDateTime ancora = ultimaVenda != null ? ultimaVenda : produto.getCadastradoEm();
                    long dias = ChronoUnit.DAYS.between(ancora.toLocalDate(), hoje);

                    return new LinhaProdutoParado(
                            produto.getId(), produto.getNome(), produto.getMarca(),
                            produto.getUnidade().getId(), produto.getUnidade().getNome(),
                            ultimaVenda != null ? ultimaVenda.toLocalDate() : null, dias);
                })
                .filter(linha -> linha.diasParado() >= diasParado)
                .sorted(Comparator.comparingLong(LinhaProdutoParado::diasParado).reversed())
                .toList();
    }

    private void aplicar(ProdutoDtos.Request request, Produto produto) {
        produto.setNome(request.nome().trim());
        produto.setMarca(request.marca() != null && !request.marca().isBlank() ? request.marca().trim() : null);
        produto.setPrecoVenda(request.precoVenda());
        produto.setEstoqueMinimo(request.estoqueMinimo());
    }

    private Produto carregar(Long id) {
        return repository.findWithUnidadeById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", id));
    }

    private Unidade carregarUnidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));
    }
}
