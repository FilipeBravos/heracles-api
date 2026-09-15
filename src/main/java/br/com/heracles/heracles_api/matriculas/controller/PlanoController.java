package br.com.heracles.heracles_api.matriculas.controller;

import br.com.heracles.heracles_api.matriculas.dto.PlanoDtos;
import br.com.heracles.heracles_api.matriculas.service.PlanoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/planos")
public class PlanoController {

    private final PlanoService service;

    public PlanoController(PlanoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PlanoDtos.Response> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(apenasAtivos, pageable);
    }

    @GetMapping("/{id}")
    public PlanoDtos.Response buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<PlanoDtos.Response> criar(@RequestBody @Valid PlanoDtos.Request request,
                                                    UriComponentsBuilder uriBuilder) {
        PlanoDtos.Response criado = service.criar(request);
        var uri = uriBuilder.path("/api/planos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    public PlanoDtos.Response atualizar(@PathVariable Long id,
                                        @RequestBody @Valid PlanoDtos.Request request) {
        return service.atualizar(id, request);
    }

    @PutMapping("/{id}/ativo")
    public PlanoDtos.Response alternarAtivo(@PathVariable Long id) {
        return service.alternarAtivo(id);
    }
}
