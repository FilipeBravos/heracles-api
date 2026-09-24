package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.LinhaAdesaoTreino;
import br.com.heracles.heracles_api.core.service.ExecucaoExercicioService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Visibilidade de gestao sobre execucoes de exercicio — diferente de
 * /api/eu/execucoes, que e sempre sobre quem esta autenticado.
 */
@Validated
@RestController
@RequestMapping("/api/execucoes-exercicio")
public class ExecucaoExercicioController {

    private final ExecucaoExercicioService service;

    public ExecucaoExercicioController(ExecucaoExercicioService service) {
        this.service = service;
    }

    /** Adesao ao treino por aluno, do pior pro melhor. */
    @GetMapping("/relatorio/adesao")
    public List<LinhaAdesaoTreino> relatorioAdesao(
            @RequestParam(defaultValue = "90") @Min(1) @Max(365) int dias,
            @RequestParam(defaultValue = "" + ExecucaoExercicioService.QUANTIDADE_MINIMA_EXECUCOES_ADESAO_PADRAO)
            @Min(1) @Max(200) int quantidadeMinima) {
        return service.adesaoPorAluno(dias, quantidadeMinima);
    }
}
