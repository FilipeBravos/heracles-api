package br.com.heracles.heracles_api.operacoes.controller;

import br.com.heracles.heracles_api.operacoes.dto.EquipamentoDtos;
import br.com.heracles.heracles_api.operacoes.service.EquipamentoService;
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
@RequestMapping("/api/equipamentos")
public class EquipamentoController {

    private final EquipamentoService service;

    public EquipamentoController(EquipamentoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<EquipamentoDtos.Response> listar(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}/chamados")
    public List<EquipamentoDtos.ChamadoResponse> historico(@PathVariable Long id) {
        return service.historico(id);
    }

    /** Painel de manutencao: custo, tempo medio de resolucao, equipamentos mais problematicos e comparacao por unidade. */
    @GetMapping("/relatorio")
    public EquipamentoDtos.PainelManutencao relatorio(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias) {
        return service.relatorio(dias);
    }

    @PostMapping
    public ResponseEntity<EquipamentoDtos.Response> criar(@RequestBody @Valid EquipamentoDtos.Request request,
                                                          UriComponentsBuilder uriBuilder) {
        EquipamentoDtos.Response criado = service.criar(request);
        var uri = uriBuilder.path("/api/equipamentos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    public EquipamentoDtos.Response atualizar(@PathVariable Long id,
                                              @RequestBody @Valid EquipamentoDtos.Request request) {
        return service.atualizar(id, request);
    }

    @PostMapping("/{id}/chamados")
    public ResponseEntity<EquipamentoDtos.ChamadoResponse> abrirChamado(
            @PathVariable Long id, @RequestBody @Valid EquipamentoDtos.AbrirChamado request) {
        return ResponseEntity.ok(service.abrirChamado(id, request));
    }

    @PutMapping("/chamados/{chamadoId}/resolver")
    public EquipamentoDtos.ChamadoResponse resolverChamado(
            @PathVariable Long chamadoId, @RequestBody @Valid EquipamentoDtos.ResolverChamado request) {
        return service.resolverChamado(chamadoId, request);
    }
}
