package br.com.heracles.heracles_api.agenda.controller;

import br.com.heracles.heracles_api.agenda.dto.AulaGrupoDtos;
import br.com.heracles.heracles_api.agenda.service.AulaGrupoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
