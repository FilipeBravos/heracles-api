package br.com.heracles.heracles_api.matriculas.controller;

import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos;
import br.com.heracles.heracles_api.matriculas.dto.CobrancaDtos;
import br.com.heracles.heracles_api.matriculas.service.AssinaturaService;
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

@RestController
@Validated
@RequestMapping("/api/assinaturas")
public class AssinaturaController {

    private final AssinaturaService service;

    public AssinaturaController(AssinaturaService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AssinaturaDtos.Response> listar(
            @PageableDefault(size = 20, sort = "dataVencimento", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public AssinaturaDtos.Response buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/aluno/{alunoId}")
    public List<AssinaturaDtos.Response> historicoDoAluno(@PathVariable Long alunoId) {
        return service.historicoDoAluno(alunoId);
    }

    /** Serie do grafico de matriculas por mes. */
    @GetMapping("/historico-mensal")
    public AssinaturaDtos.HistoricoMensal historicoMensal(
            @RequestParam(defaultValue = "12") @Min(2) @Max(36) int meses) {
        return service.historicoMensal(meses);
    }

    /**
     * Fila de vencimentos do painel: quem vence nos proximos dias — e quem
     * ja venceu.
     */
    @GetMapping("/vencimentos")
    public AssinaturaDtos.FilaDeVencimentos vencimentos(
            @RequestParam(defaultValue = "15") @Min(1) @Max(180) int dias,
            @RequestParam(defaultValue = "8") @Min(1) @Max(50) int limite) {
        return service.vencimentos(dias, limite);
    }

    /** Veredito da catraca: este aluno pode treinar nesta unidade hoje? */
    @GetMapping("/acesso")
    public AssinaturaDtos.Acesso conferirAcesso(@RequestParam Long alunoId, @RequestParam Long unidadeId) {
        return service.conferirAcesso(alunoId, unidadeId);
    }

    /** Extrato de cobrancas (simuladas) desta assinatura, mais recente primeiro. */
    @GetMapping("/{id}/cobrancas")
    public List<CobrancaDtos.Response> historicoCobrancas(@PathVariable Long id) {
        return service.historicoCobrancas(id);
    }

    /** Cabecalho do relatorio de inadimplencia: quantos em cada etapa da regua. */
    @GetMapping("/inadimplencia/resumo")
    public AssinaturaDtos.ResumoInadimplencia resumoInadimplencia(
            @RequestParam(defaultValue = "" + AssinaturaService.DIAS_VENCE_EM_BREVE_PADRAO)
            @Min(1) @Max(60) int diasParaVencer) {
        return service.resumoInadimplencia(diasParaVencer);
    }

    /** O relatorio de inadimplencia: quem vence em breve, ja venceu ou esta inadimplente. */
    @GetMapping("/inadimplencia")
    public Page<AssinaturaDtos.LinhaInadimplencia> inadimplencia(
            @PageableDefault(size = 20, sort = "dataVencimento", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(defaultValue = "" + AssinaturaService.DIAS_VENCE_EM_BREVE_PADRAO)
            @Min(1) @Max(60) int diasParaVencer) {
        return service.inadimplencia(pageable, diasParaVencer);
    }

    @PostMapping
    public ResponseEntity<AssinaturaDtos.Response> matricular(
            @RequestBody @Valid AssinaturaDtos.Matricular request, UriComponentsBuilder uriBuilder) {
        AssinaturaDtos.Response criada = service.matricular(request);
        var uri = uriBuilder.path("/api/assinaturas/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(uri).body(criada);
    }

    @PutMapping("/{id}/renovacoes")
    public AssinaturaDtos.Response renovar(@PathVariable Long id) {
        return service.renovar(id);
    }

    @PutMapping("/{id}/inadimplencia")
    public AssinaturaDtos.Response marcarInadimplente(@PathVariable Long id) {
        return service.marcarInadimplente(id);
    }

    @DeleteMapping("/{id}")
    public AssinaturaDtos.Response cancelar(@PathVariable Long id) {
        // Cancelar nao apaga: a assinatura vira CANCELADA com data, e o
        // historico do aluno continua completo.
        return service.cancelar(id);
    }
}
