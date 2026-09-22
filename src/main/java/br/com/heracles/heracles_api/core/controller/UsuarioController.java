package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.domain.AvaliacaoFisicaFoto;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.AnamneseDtos;
import br.com.heracles.heracles_api.core.dto.Aniversariante;
import br.com.heracles.heracles_api.core.dto.AvaliacaoFisicaDtos;
import br.com.heracles.heracles_api.core.dto.ContratoDtos;
import br.com.heracles.heracles_api.core.dto.LinhaReavaliacaoVencida;
import br.com.heracles.heracles_api.core.dto.ResumoReavaliacaoVencida;
import br.com.heracles.heracles_api.core.dto.UsuarioRequests;
import br.com.heracles.heracles_api.core.dto.UsuarioResponse;
import br.com.heracles.heracles_api.core.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@Validated
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
                                                 HttpServletRequest httpRequest,
                                                 UriComponentsBuilder uriBuilder) {
        // Quem cria sai do token, nunca do corpo: o perfil que a pessoa
        // pode cadastrar depende de quem ela e.
        UsuarioResponse criado = service.criar(request, jwt.getSubject(), httpRequest.getRemoteAddr());
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

    /** O contrato assinado no cadastro — so leitura, nao ha PUT: o que foi assinado nao se reescreve. */
    @GetMapping("/{id}/contrato")
    public ContratoDtos.Response buscarContrato(@PathVariable Long id) {
        return service.buscarContrato(id);
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

    /** Uma avaliacao pode ter varias fotos (frente, lado, costas) — cada uma tem seu proprio id. */
    @GetMapping("/{id}/avaliacoes-fisicas/{avaliacaoId}/fotos/{fotoId}")
    public ResponseEntity<byte[]> buscarFotoAvaliacaoFisica(
            @PathVariable Long id, @PathVariable Long avaliacaoId, @PathVariable Long fotoId) {
        AvaliacaoFisicaFoto foto = service.buscarFotoAvaliacaoFisica(id, avaliacaoId, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getFotoContentType()))
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .body(foto.getFoto());
    }

    /**
     * O comparativo entre duas avaliacoes: a primeira e a mais recente por
     * padrao, ou as duas escolhidas via deId/paraId.
     */
    @GetMapping("/{id}/avaliacoes-fisicas/comparativo")
    public AvaliacaoFisicaDtos.Comparativo compararAvaliacoesFisicas(
            @PathVariable Long id,
            @RequestParam(required = false) Long deId,
            @RequestParam(required = false) Long paraId) {
        return service.compararAvaliacoesFisicas(id, deId, paraId);
    }

    /** Aniversariantes do mes corrente, do dia mais proximo pro mais distante. */
    @GetMapping("/aniversariantes")
    public List<Aniversariante> aniversariantes() {
        return service.aniversariantesDoMes();
    }

    /** Cabecalho do alerta: quantos alunos com matricula ativa estao com a reavaliacao fisica vencida. */
    @GetMapping("/reavaliacao-vencida/resumo")
    public ResumoReavaliacaoVencida resumoReavaliacaoVencida(
            @RequestParam(defaultValue = "" + UsuarioService.DIAS_REAVALIACAO_PADRAO)
            @Min(30) @Max(365) int diasSemReavaliacao) {
        return service.resumoReavaliacaoVencida(diasSemReavaliacao);
    }

    /**
     * O alerta de reavaliacao vencida: matricula ativa, mas a ultima
     * avaliacao fisica passou da janela (ou nunca aconteceu).
     */
    @GetMapping("/reavaliacao-vencida")
    public Page<LinhaReavaliacaoVencida> reavaliacaoVencida(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(defaultValue = "" + UsuarioService.DIAS_REAVALIACAO_PADRAO)
            @Min(30) @Max(365) int diasSemReavaliacao) {
        return service.reavaliacaoVencida(pageable, diasSemReavaliacao);
    }
}
