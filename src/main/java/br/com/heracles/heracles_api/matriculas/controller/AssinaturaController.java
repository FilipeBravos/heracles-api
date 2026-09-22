package br.com.heracles.heracles_api.matriculas.controller;

import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos;
import br.com.heracles.heracles_api.matriculas.dto.CheckinDtos;
import br.com.heracles.heracles_api.matriculas.dto.CobrancaDtos;
import br.com.heracles.heracles_api.matriculas.dto.ComissaoIndicacaoDtos;
import br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada;
import br.com.heracles.heracles_api.matriculas.dto.LembreteDtos;
import br.com.heracles.heracles_api.matriculas.dto.LinhaMotivoCancelamento;
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

    /** Painel de retencao: tendencia de churn mensal, e o detalhamento do ultimo mes por plano e por unidade. */
    @GetMapping("/retencao")
    public AssinaturaDtos.Retencao retencao(
            @RequestParam(defaultValue = "12") @Min(2) @Max(36) int meses) {
        return service.retencao(meses);
    }

    /** Painel financeiro: MRR, ticket medio, inadimplencia em R$ e a projecao de caixa do mes. */
    @GetMapping("/financeiro")
    public AssinaturaDtos.PainelFinanceiro financeiro() {
        return service.financeiro();
    }

    /** Ocupacao por hora do dia, por unidade, nos ultimos dias informados. */
    @GetMapping("/ocupacao")
    public AssinaturaDtos.PainelOcupacao ocupacao(
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int dias) {
        return service.ocupacao(dias);
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

    /**
     * Veredito da catraca: este aluno pode treinar nesta unidade hoje?
     *
     * Cada chamada tambem grava um check-in — e a mesma pergunta que a
     * recepcao faz na porta, entao a resposta e o historico de frequencia
     * sao o mesmo evento.
     */
    @GetMapping("/acesso")
    public AssinaturaDtos.Acesso conferirAcesso(@RequestParam Long alunoId, @RequestParam Long unidadeId) {
        return service.conferirAcesso(alunoId, unidadeId);
    }

    /** Historico de frequencia do aluno: cada check-in, liberado ou barrado. */
    @GetMapping("/checkins/aluno/{alunoId}")
    public Page<CheckinDtos.Response> historicoCheckins(
            @PathVariable Long alunoId,
            @PageableDefault(size = 20, sort = "momento", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.historicoCheckins(alunoId, pageable);
    }

    /** Extrato de cobrancas (simuladas) desta assinatura, mais recente primeiro. */
    @GetMapping("/{id}/cobrancas")
    public List<CobrancaDtos.Response> historicoCobrancas(@PathVariable Long id) {
        return service.historicoCobrancas(id);
    }

    /** Extrato de lembretes (simulados) desta assinatura, mais recente primeiro. */
    @GetMapping("/{id}/lembretes")
    public List<LembreteDtos.Response> historicoLembretes(@PathVariable Long id) {
        return service.historicoLembretes(id);
    }

    /** Ranking do programa de indicacao: quantas matriculas cada aluno trouxe. */
    @GetMapping("/indicacoes")
    public List<ContagemAgrupada> indicacoes() {
        return service.indicacoes();
    }

    /** Cabecalho do alerta: quantas comissoes de indicacao esperam aprovacao da secretaria. */
    @GetMapping("/comissoes-indicacao/resumo")
    public ComissaoIndicacaoDtos.Resumo resumoComissoesIndicacao() {
        return service.resumoComissoesIndicacao();
    }

    /** A fila de comissoes de indicacao pendentes de aprovacao. */
    @GetMapping("/comissoes-indicacao")
    public Page<ComissaoIndicacaoDtos.Response> comissoesIndicacaoPendentes(
            @PageableDefault(size = 20) Pageable pageable) {
        return service.comissoesIndicacaoPendentes(pageable);
    }

    /** Aprova a comissao: aplica o desconto na cobranca pendente da matricula vigente do indicador. */
    @PutMapping("/comissoes-indicacao/{id}/aplicar")
    public ComissaoIndicacaoDtos.Response aplicarComissaoIndicacao(@PathVariable Long id) {
        return service.aplicarComissaoIndicacao(id);
    }

    /** Quantos cancelamentos por motivo, do mais comum para o menos comum. */
    @GetMapping("/motivos-cancelamento")
    public List<LinhaMotivoCancelamento> motivosCancelamento() {
        return service.motivosCancelamento();
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

    /** Cabecalho do alerta de inatividade: quantos alunos com matricula ativa pararam de aparecer. */
    @GetMapping("/inatividade/resumo")
    public AssinaturaDtos.ResumoAlunosInativos resumoAlunosInativos(
            @RequestParam(defaultValue = "" + AssinaturaService.DIAS_INATIVIDADE_PADRAO)
            @Min(1) @Max(180) int diasSemCheckin) {
        return service.resumoAlunosInativos(diasSemCheckin);
    }

    /**
     * O alerta de inatividade: matricula ativa, mas o aluno parou de
     * fazer check-in — indicador antecedente pra secretaria agir antes
     * do cancelamento, nao um retrato do que ja aconteceu.
     */
    @GetMapping("/inatividade")
    public Page<AssinaturaDtos.LinhaAlunoInativo> alunosInativos(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(defaultValue = "" + AssinaturaService.DIAS_INATIVIDADE_PADRAO)
            @Min(1) @Max(180) int diasSemCheckin) {
        return service.alunosInativos(pageable, diasSemCheckin);
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
    public AssinaturaDtos.Response cancelar(@PathVariable Long id, @RequestBody @Valid AssinaturaDtos.Cancelar request) {
        // Cancelar nao apaga: a assinatura vira CANCELADA com data, e o
        // historico do aluno continua completo.
        return service.cancelar(id, request);
    }
}
