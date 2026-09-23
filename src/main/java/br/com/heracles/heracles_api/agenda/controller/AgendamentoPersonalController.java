package br.com.heracles.heracles_api.agenda.controller;

import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.dto.LinhaAvaliacaoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaCancelamentoProfessor;
import br.com.heracles.heracles_api.agenda.dto.LinhaOcupacaoPersonal;
import br.com.heracles.heracles_api.agenda.service.AgendamentoPersonalService;
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

@RestController
@Validated
@RequestMapping("/api/sessoes-personal")
public class AgendamentoPersonalController {

    private final AgendamentoPersonalService service;

    public AgendamentoPersonalController(AgendamentoPersonalService service) {
        this.service = service;
    }

    @GetMapping
    public Page<AgendamentoPersonalDtos.Response> listar(
            @RequestParam(required = false) Long professorId,
            @RequestParam(required = false) Long alunoId,
            @PageableDefault(size = 20, sort = "dataHora", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(professorId, alunoId, pageable);
    }

    @PostMapping
    public ResponseEntity<AgendamentoPersonalDtos.Response> criar(
            @RequestBody @Valid AgendamentoPersonalDtos.Salvar request) {
        return ResponseEntity.status(201).body(service.criar(request));
    }

    @PutMapping("/{id}/cancelamento")
    public AgendamentoPersonalDtos.Response cancelar(@PathVariable Long id) {
        return service.cancelar(id);
    }

    /**
     * Nota media por professor, do melhor pro pior — visibilidade de
     * qualidade de atendimento pra gestao. `quantidadeMinima` deixa de
     * fora quem ainda nao tem amostra suficiente pra sustentar a media.
     */
    @GetMapping("/avaliacoes")
    public List<LinhaAvaliacaoProfessor> avaliacoes(
            @RequestParam(defaultValue = "" + AgendamentoPersonalService.QUANTIDADE_MINIMA_AVALIACOES_PADRAO)
            @Min(1) @Max(50) long quantidadeMinima) {
        return service.mediaAvaliacaoPorProfessor(quantidadeMinima);
    }

    /** Taxa de cancelamento em cima da hora por professor, do pior pro melhor. */
    @GetMapping("/cancelamentos")
    public List<LinhaCancelamentoProfessor> cancelamentos(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias,
            @RequestParam(defaultValue = "" + AgendamentoPersonalService.QUANTIDADE_MINIMA_SESSOES_CANCELAMENTO_PADRAO)
            @Min(1) @Max(50) long quantidadeMinima) {
        return service.taxaCancelamentoPorProfessor(dias, quantidadeMinima);
    }

    /** Taxa de ocupacao da agenda de personal por professor, do menos ocupado pro mais ocupado. */
    @GetMapping("/ocupacao")
    public List<LinhaOcupacaoPersonal> ocupacao(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias) {
        return service.ocupacaoPorProfessor(dias);
    }
}
