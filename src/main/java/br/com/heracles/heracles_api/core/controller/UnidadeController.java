package br.com.heracles.heracles_api.core.controller;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/unidades")
public class UnidadeController {

    @Autowired
    private UnidadeRepository repository;

    @PostMapping
    public Unidade criar(@RequestBody Unidade unidade) {
        return repository.save(unidade);
    }

    @GetMapping
    public List<Unidade> listarTodas() {
        return repository.findAll();
    }
}