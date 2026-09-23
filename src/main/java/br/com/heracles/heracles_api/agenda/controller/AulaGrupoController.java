package br.com.heracles.heracles_api.agenda.controller;

import br.com.heracles.heracles_api.agenda.dto.AulaGrupoDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaNoShowPorHorario;
import br.com.heracles.heracles_api.agenda.dto.LinhaPresencaPorProfessor;
import br.com.heracles.heracles_api.agenda.service.AulaGrupoService;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/aulas")
public class AulaGrupoController {

    private final AulaGrupoService service;

    public AulaGrupoController(AulaGrupoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AulaGrupoDtos.Response> listar(
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(pageable);
    }

    @PostMapping
    public ResponseEntity<AulaGrupoDtos.Response> criar(@RequestBody @Valid AulaGrupoDtos.Salvar request) {
        return ResponseEntity.status(201).body(service.criar(request));
    }

    @PutMapping("/{id}/cancelamento")
    public AulaGrupoDtos.Response cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    /**
     * A secretaria marca a vaga em nome do aluno — quem liga ou passa no
     * balcao sem usar o app. A resposta diz se entrou direto ou foi para
     * a fila de espera, ja que a turma cheia nao recusa mais.
     */
    @PostMapping("/{id}/inscricoes")
    public AulaGrupoDtos.ResultadoInscricao inscrever(
            @PathVariable Long id, @RequestBody @Valid AulaGrupoDtos.Marcar request) {
        return service.inscrever(id, request.alunoId());
    }

    @DeleteMapping("/{id}/inscricoes/{alunoId}")
    public ResponseEntity<Void> cancelarInscricao(@PathVariable Long id, @PathVariable Long alunoId) {
        service.cancelarInscricao(id, alunoId);
        return ResponseEntity.noContent().build();
    }

    /** Taxa de comparecimento geral e o ranking de quem mais falta em aula em grupo. */
    @GetMapping("/relatorio")
    public AulaGrupoDtos.PainelPresenca relatorio(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias) {
        return service.relatorioPresenca(dias);
    }

    /** Taxa de no-show por horario recorrente, do pior pro melhor — pra decisao de agenda. */
    @GetMapping("/relatorio/no-show")
    public List<LinhaNoShowPorHorario> relatorioNoShowPorHorario(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias,
            @RequestParam(defaultValue = "" + AulaGrupoService.QUANTIDADE_MINIMA_OCORRENCIAS_PADRAO)
            @Min(1) @Max(50) int quantidadeMinima) {
        return service.relatorioNoShowPorHorario(dias, quantidadeMinima);
    }

    /** Taxa de presença em aula em grupo por professor, do pior pro melhor. */
    @GetMapping("/relatorio/presenca-por-professor")
    public List<LinhaPresencaPorProfessor> relatorioPresencaPorProfessor(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias,
            @RequestParam(defaultValue = "" + AulaGrupoService.QUANTIDADE_MINIMA_PRESENCAS_PROFESSOR_PADRAO)
            @Min(1) @Max(200) int quantidadeMinima) {
        return service.relatorioPresencaPorProfessor(dias, quantidadeMinima);
    }
}
