package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public Page<UsuarioResponse> listar(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequests.Criar request,
                                                 @AuthenticationPrincipal Jwt jwt,
                                                 UriComponentsBuilder uriBuilder) {
        // Quem cria sai do token, nunca do corpo: o perfil que a pessoa
        // pode cadastrar depende de quem ela e.
        UsuarioResponse criado = service.criar(request, jwt.getSubject());
        var uri = uriBuilder.path("/api/usuarios/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable Long id,
                                     @RequestBody @Valid UsuarioRequests.Atualizar request) {
        return service.atualizar(id, request);
    }

    @PutMapping("/{id}/treinos")
    public UsuarioResponse sincronizarTreinos(@PathVariable Long id,
                                              @RequestBody @Valid UsuarioRequests.VincularTreinos request) {
        return service.sincronizarTreinos(id, request.treinosIds());
    }

    @PutMapping("/{id}/status")
    public UsuarioResponse alternarStatus(@PathVariable Long id) {
        return service.alternarStatus(id);
    }
}
