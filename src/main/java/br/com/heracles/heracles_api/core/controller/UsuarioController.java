package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisica;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.AnamneseDtos;
import br.com.heracles.heracles_api.core.dto.AvaliacaoFisicaDtos;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

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

    @GetMapping("/{id}/anamnese")
    public AnamneseDtos.Response buscarAnamnese(@PathVariable Long id) {
        return service.buscarAnamnese(id);
    }

    @PutMapping("/{id}/anamnese")
    public AnamneseDtos.Response salvarAnamnese(@PathVariable Long id,
                                                @RequestBody @Valid AnamneseDtos.Salvar request) {
        return service.salvarAnamnese(id, request);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> buscarFoto(@PathVariable Long id) {
        Usuario usuario = service.buscarFoto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(usuario.getFotoContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(usuario.getFoto());
    }

    @GetMapping("/{id}/avaliacoes-fisicas")
    public List<AvaliacaoFisicaDtos.Response> historicoAvaliacoesFisicas(@PathVariable Long id) {
        return service.historicoAvaliacoesFisicas(id);
    }

    @PostMapping("/{id}/avaliacoes-fisicas")
    @ResponseStatus(HttpStatus.CREATED)
    public AvaliacaoFisicaDtos.Response registrarAvaliacaoFisica(
            @PathVariable Long id, @RequestBody @Valid AvaliacaoFisicaDtos.Salvar request) {
        return service.registrarAvaliacaoFisica(id, request);
    }

    @GetMapping("/{id}/avaliacoes-fisicas/{avaliacaoId}/foto")
    public ResponseEntity<byte[]> buscarFotoAvaliacaoFisica(
            @PathVariable Long id, @PathVariable Long avaliacaoId) {
        AvaliacaoFisica avaliacao = service.buscarFotoAvaliacaoFisica(id, avaliacaoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avaliacao.getFotoContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(avaliacao.getFoto());
    }
}
