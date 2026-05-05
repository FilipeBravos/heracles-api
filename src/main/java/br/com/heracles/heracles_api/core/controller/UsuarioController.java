package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import br.com.heracles.heracles_api.core.domain.Treino; // Ajuste o pacote do Treino
import br.com.heracles.heracles_api.core.repository.TreinoRepository; // Ajuste o pacote do TreinoRepository

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200") // Permite que seu Angular acesse a API
@RestController
@RequestMapping("/api/usuarios")


public class UsuarioController {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TreinoRepository treinoRepository;

    @PostMapping
    public Usuario criar(@RequestBody Usuario usuario) {
        return repository.save(usuario);
    }

    @GetMapping
    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    @PutMapping("/{idUsuario}/treinos")
    public Usuario sincronizarTreinos(@PathVariable Long idUsuario, @RequestBody List<Long> treinosIds) {
        Usuario usuario = repository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado"));

        // Busca todos os treinos que o Front mandou e atribui ao aluno
        List<Treino> treinosSelecionados = treinoRepository.findAllById(treinosIds);
        usuario.setTreinos(treinosSelecionados);

        return repository.save(usuario);
    }

    @PutMapping("/{id}")
    public Usuario atualizarAluno(@PathVariable Long id, @RequestBody Usuario dadosAtualizados) {
        return repository.findById(id).map(alunoExistente -> {
            alunoExistente.setNome(dadosAtualizados.getNome());
            alunoExistente.setCpf(dadosAtualizados.getCpf());
            alunoExistente.setEmail(dadosAtualizados.getEmail());
            alunoExistente.setTelefone(dadosAtualizados.getTelefone());

            return repository.save(alunoExistente);
        }).orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
    }

    // [NOVO] Rota para alternar o status do aluno (Ativo <-> Inativo)
    @PutMapping("/{id}/status")
    public Usuario alternarStatus(@PathVariable Long id) {
        return repository.findById(id).map(alunoExistente -> {
            // Se está ATIVO, vira INATIVO. Se não, vira ATIVO.
            if (alunoExistente.getStatus() == StatusUsuario.ATIVO) {
                alunoExistente.setStatus(StatusUsuario.INATIVO);
            } else {
                alunoExistente.setStatus(StatusUsuario.ATIVO);
            }
            return repository.save(alunoExistente);
        }).orElseThrow(() -> new RuntimeException("Aluno não encontrado"));
    }

}