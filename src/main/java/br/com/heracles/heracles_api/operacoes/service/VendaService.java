package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.ItemVenda;
import br.com.heracles.heracles_api.operacoes.domain.Produto;
import br.com.heracles.heracles_api.operacoes.domain.Venda;
import br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoMaisVendido;
import br.com.heracles.heracles_api.operacoes.dto.VendaDtos;
import br.com.heracles.heracles_api.operacoes.repository.ProdutoRepository;
import br.com.heracles.heracles_api.operacoes.repository.VendaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VendaService {

    private final VendaRepository repository;
    private final ProdutoRepository produtoRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;

    public VendaService(VendaRepository repository,
                        ProdutoRepository produtoRepository,
                        UnidadeRepository unidadeRepository,
                        UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.produtoRepository = produtoRepository;
        this.unidadeRepository = unidadeRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<VendaDtos.Response> listar(Pageable pageable) {
        return repository.buscarPaginadoCompleto(pageable).map(VendaDtos.Response::de);
    }

    @Transactional(readOnly = true)
    public VendaDtos.Response buscarPorId(Long id) {
        return repository.findWithItensById(id)
                .map(VendaDtos.Response::de)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Venda", id));
    }

    /**
     * O relatorio de vendas da loja: faturamento e ticket medio do
     * periodo, os produtos mais vendidos e a comparacao entre unidades.
     */
    @Transactional(readOnly = true)
    public VendaDtos.PainelVendas relatorio(int dias) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();

        BigDecimal faturamentoTotal = repository.faturamentoDesde(desde);
        long quantidadeVendas = repository.countByDataVendaAfter(desde);
        BigDecimal ticketMedio = quantidadeVendas > 0
                ? faturamentoTotal.divide(BigDecimal.valueOf(quantidadeVendas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<LinhaProdutoMaisVendido> maisVendidos =
                repository.produtosMaisVendidosDesde(desde, PageRequest.of(0, 10));

        List<VendaDtos.LinhaVendaPorUnidade> porUnidade = repository.faturamentoPorUnidadeDesde(desde).stream()
                .map(VendaDtos.LinhaVendaPorUnidade::de)
                .toList();

        List<VendaDtos.LinhaVendaPorMetodoPagamento> porMetodoPagamento =
                repository.faturamentoPorMetodoPagamentoDesde(desde).stream()
                        .map(VendaDtos.LinhaVendaPorMetodoPagamento::de)
                        .toList();

        return new VendaDtos.PainelVendas(
                dias, faturamentoTotal, quantidadeVendas, ticketMedio, maisVendidos, porUnidade, porMetodoPagamento);
    }

    /**
     * Fecha uma venda.
     *
     * Tudo acontece numa transacao so, e cada produto e carregado com a
     * linha travada (SELECT ... FOR UPDATE). E o que impede duas vendas
     * simultaneas do ultimo item: sem a trava, ambas leem o mesmo saldo,
     * ambas passam na verificacao e ambas gravam.
     *
     * Se qualquer item falhar, a transacao inteira volta atras — nao
     * existe venda parcial com meio estoque baixado.
     */
    @Transactional
    public VendaDtos.Response registrar(VendaDtos.Registrar request, String emailOperador) {
        Unidade unidade = unidadeRepository.findById(request.unidadeId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", request.unidadeId()));

        // O operador vem do token, nao do corpo: quem registrou a venda nao
        // pode ser escolhido por quem a envia.
        Usuario operador = usuarioRepository.findByEmailIgnoreCase(emailOperador)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operador da venda nao encontrado."));

        Venda venda = new Venda();
        venda.setUnidade(unidade);
        venda.setOperador(operador);
        venda.setMetodoPagamento(request.metodoPagamento());

        if (request.alunoId() != null) {
            Usuario aluno = usuarioRepository.findById(request.alunoId())
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", request.alunoId()));
            venda.setAluno(aluno);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> pedido : agruparPorProduto(request).entrySet()) {
            Produto produto = produtoRepository.buscarParaVenda(pedido.getKey())
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", pedido.getKey()));

            if (!produto.getUnidade().getId().equals(unidade.getId())) {
                throw new RegraNegocioException(
                        "O produto \"%s\" pertence a outra unidade.".formatted(produto.getNome()));
            }
            if (!produto.isAtivo()) {
                throw new RegraNegocioException(
                        "O produto \"%s\" esta fora de linha e nao pode ser vendido.".formatted(produto.getNome()));
            }

            produto.baixarEstoque(pedido.getValue());

            ItemVenda item = new ItemVenda();
            item.setProduto(produto);
            item.setQuantidade(pedido.getValue());
            // Preco congelado no momento da venda.
            item.setPrecoUnitario(produto.getPrecoVenda());
            venda.adicionarItem(item);

            total = total.add(item.subtotal());
        }

        // Calculado aqui, a partir da tabela de precos. Nunca vem do cliente.
        venda.setValorTotal(total);

        return VendaDtos.Response.de(repository.save(venda));
    }

    /**
     * Soma quantidades do mesmo produto.
     *
     * O carrinho pode ter o item adicionado duas vezes; somar e o
     * comportamento que o operador espera, e mantem uma linha por produto
     * na venda — que e o que o indice unico da tabela exige.
     *
     * LinkedHashMap para que a ordem de travamento das linhas siga a ordem
     * de envio, e nao a de um hash: ordem estavel reduz a chance de duas
     * vendas concorrentes travarem em sentidos opostos e gerarem deadlock.
     */
    private Map<Long, Integer> agruparPorProduto(VendaDtos.Registrar request) {
        Map<Long, Integer> porProduto = new LinkedHashMap<>();
        for (VendaDtos.Registrar.Item item : request.itens()) {
            porProduto.merge(item.produtoId(), item.quantidade(), Integer::sum);
        }
        return porProduto;
    }
}
