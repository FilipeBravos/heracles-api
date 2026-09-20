package br.com.heracles.heracles_api.agenda.controller;

import br.com.heracles.heracles_api.agenda.dto.AgendamentoPersonalDtos;
import br.com.heracles.heracles_api.agenda.service.AgendamentoPersonalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
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
}
