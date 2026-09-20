package br.com.heracles.heracles_api.agenda.controller;

import br.com.heracles.heracles_api.agenda.dto.HorarioProfessorDtos;
import br.com.heracles.heracles_api.agenda.service.HorarioProfessorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/professores/{professorId}/horarios")
public class HorarioProfessorController {

    private final HorarioProfessorService service;

    public HorarioProfessorController(HorarioProfessorService service) {
        this.service = service;
    }

    @GetMapping
    public List<HorarioProfessorDtos.Response> listar(@PathVariable Long professorId) {
        return service.listar(professorId);
    }

    @PostMapping
    public ResponseEntity<HorarioProfessorDtos.Response> criar(
            @PathVariable Long professorId,
            @RequestBody @Valid HorarioProfessorDtos.Salvar request) {
        return ResponseEntity.status(201).body(service.criar(professorId, request));
    }

    @DeleteMapping("/{horarioId}")
    public ResponseEntity<Void> remover(@PathVariable Long professorId, @PathVariable Long horarioId) {
        service.remover(professorId, horarioId);
        return ResponseEntity.noContent().build();
    }
}
