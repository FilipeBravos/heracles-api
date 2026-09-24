package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.dto.LinhaAlunoSemFicha;
import br.com.heracles.heracles_api.core.dto.LinhaPermanenciaPorNivel;
import br.com.heracles.heracles_api.core.dto.ResumoAlunosSemFicha;
import br.com.heracles.heracles_api.core.dto.TreinoRequest;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.service.TreinoService;
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

// Sem @CrossOrigin: a politica de CORS e unica e vive em SecurityConfig.
@Validated
@RestController
@RequestMapping("/api/treinos")
public class TreinoController {

    private final TreinoService service;

    public TreinoController(TreinoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<TreinoResponse> listar(
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public TreinoResponse buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    /**
     * O @PostMapping que faltava: o metodo existia e estava correto, mas sem
     * anotacao o Spring nunca registrou a rota, e criar ficha respondia 405.
     */
    @PostMapping
    public ResponseEntity<TreinoResponse> criar(@RequestBody @Valid TreinoRequest request,
                                                UriComponentsBuilder uriBuilder) {
        TreinoResponse criado = service.criar(request);
        var uri = uriBuilder.path("/api/treinos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(uri).body(criado);
    }

    @PutMapping("/{id}")
    public TreinoResponse atualizar(@PathVariable Long id, @RequestBody @Valid TreinoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    /** Cabecalho do alerta: quantos alunos com matricula ativa nunca receberam ficha de treino. */
    @GetMapping("/alunos-sem-ficha/resumo")
    public ResumoAlunosSemFicha resumoAlunosSemFicha() {
        return service.resumoAlunosSemFicha();
    }

    /**
     * O alerta de cobertura: alunos com matricula ativa que nunca
     * receberam uma ficha de treino.
     */
    @GetMapping("/alunos-sem-ficha")
    public Page<LinhaAlunoSemFicha> alunosSemFicha(
            @PageableDefault(size = 20, sort = "dataCadastro", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.alunosSemFicha(pageable);
    }

    /** Tempo medio de permanencia numa ficha antes da troca, por nivel, do mais tempo pro menos. */
    @GetMapping("/relatorio/permanencia")
    public List<LinhaPermanenciaPorNivel> relatorioPermanencia(
            @RequestParam(defaultValue = "365") @Min(1) @Max(1095) int dias) {
        return service.permanenciaPorNivel(dias);
    }
}
