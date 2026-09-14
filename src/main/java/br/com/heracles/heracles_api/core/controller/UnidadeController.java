package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.UnidadeDtos;
import br.com.heracles.heracles_api.core.service.UnidadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    private final UnidadeService service;

    public UnidadeController(UnidadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<UnidadeDtos.Response> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public UnidadeDtos.Response buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<UnidadeDtos.Response> criar(@RequestBody @Valid UnidadeDtos.Request request,
                                                      UriComponentsBuilder uriBuilder) {
        UnidadeDtos.Response criada = service.criar(request);
        var uri = uriBuilder.path("/api/unidades/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @PutMapping("/{id}")
    public UnidadeDtos.Response atualizar(@PathVariable Long id,
                                          @RequestBody @Valid UnidadeDtos.Request request) {
        return service.atualizar(id, request);
    }
}
