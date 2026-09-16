package br.com.heracles.heracles_api.operacoes.controller;

import br.com.heracles.heracles_api.operacoes.dto.VendaDtos;
import br.com.heracles.heracles_api.operacoes.service.VendaService;
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
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService service;

    public VendaController(VendaService service) {
        this.service = service;
    }

    @GetMapping
    public Page<VendaDtos.Response> listar(
            @PageableDefault(size = 20, sort = "dataVenda", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public VendaDtos.Response buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<VendaDtos.Response> registrar(@RequestBody @Valid VendaDtos.Registrar request,
                                                        @AuthenticationPrincipal Jwt jwt,
                                                        UriComponentsBuilder uriBuilder) {
        // O operador sai do token (subject = e-mail), nao do corpo: quem
        // fechou o caixa nao e escolha de quem envia a requisicao.
        VendaDtos.Response registrada = service.registrar(request, jwt.getSubject());
        var uri = uriBuilder.path("/api/vendas/{id}").buildAndExpand(registrada.id()).toUri();
        return ResponseEntity.created(uri).body(registrada);
    }
}
