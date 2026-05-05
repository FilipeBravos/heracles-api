package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/treinos")
public class TreinoController {

    @Autowired
    private TreinoRepository repository;

    @GetMapping
    public List<Treino> listarTodos() {
        return repository.findAll();
    }

    public Treino salvarTreino(@RequestBody Treino novoTreino) {
        // Antes de salvar, avisa para cada exercício quem é o "Pai" (Treino) dele
        if (novoTreino.getExercicios() != null) {
            novoTreino.getExercicios().forEach(ex -> ex.setTreino(novoTreino));
        }
        return repository.save(novoTreino);
    }

    @PutMapping("/{id}")
    public Treino atualizar(@PathVariable Long id, @RequestBody Treino dadosAtualizados) {
        return repository.findById(id).map(treinoExistente -> {
            treinoExistente.setNome(dadosAtualizados.getNome());
            treinoExistente.setFoco(dadosAtualizados.getFoco());
            treinoExistente.setNivel(dadosAtualizados.getNivel());

            // Limpa a lista de exercícios antiga e coloca a nova que veio do Front
            treinoExistente.getExercicios().clear();
            if (dadosAtualizados.getExercicios() != null) {
                dadosAtualizados.getExercicios().forEach(ex -> {
                    ex.setTreino(treinoExistente);
                    treinoExistente.getExercicios().add(ex);
                });
            }
            return repository.save(treinoExistente);
        }).orElseThrow(() -> new RuntimeException("Treino não encontrado"));
    }

    // [NOVO] Excluir a ficha
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build(); // Retorna 204 (Sucesso sem conteúdo) para o Front
    }
}
