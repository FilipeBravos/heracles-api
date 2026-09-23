package br.com.heracles.heracles_api.operacoes.controller;

import br.com.heracles.heracles_api.operacoes.dto.LinhaProdutoParado;
import br.com.heracles.heracles_api.operacoes.dto.ProdutoDtos;
import br.com.heracles.heracles_api.operacoes.service.ProdutoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ProdutoDtos.Response> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(apenasAtivos, pageable);
    }

    @GetMapping("/{id}")
    public ProdutoDtos.Response buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProdutoDtos.Response> criar(@RequestBody @Valid ProdutoDtos.Request request,
                                                      UriComponentsBuilder uriBuilder) {
        ProdutoDtos.Response criado = service.criar(request);
        var uri = uriBuilder.path("/api/produtos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    public ProdutoDtos.Response atualizar(@PathVariable Long id,
                                          @RequestBody @Valid ProdutoDtos.Request request) {
        return service.atualizar(id, request);
    }

    @PostMapping("/{id}/entradas")
    public ProdutoDtos.Response registrarEntrada(@PathVariable Long id,
                                                 @RequestBody @Valid ProdutoDtos.AjusteEstoque ajuste) {
        return service.registrarEntrada(id, ajuste);
    }

    @PutMapping("/{id}/ativo")
    public ProdutoDtos.Response alternarAtivo(@PathVariable Long id) {
        return service.alternarAtivo(id);
    }

    /** A sugestao de reposicao: produtos ativos abaixo do proprio estoque minimo, do maior deficit pro menor. */
    @GetMapping("/reposicao-estoque")
    public List<ProdutoDtos.LinhaReposicao> reposicaoEstoque() {
        return service.reposicaoEstoque();
    }

    /** O oposto da reposicao: produtos ativos sem venda ha pelo menos `dias`, do mais parado pro menos. */
    @GetMapping("/parados")
    public List<LinhaProdutoParado> produtosParados(
            @RequestParam(defaultValue = "" + ProdutoService.DIAS_PARADO_PADRAO) @Min(1) @Max(3650) int dias) {
        return service.produtosParados(dias);
    }
}
