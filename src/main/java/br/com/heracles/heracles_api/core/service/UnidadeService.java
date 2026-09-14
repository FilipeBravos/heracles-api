package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.dto.UnidadeDtos;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnidadeService {

    private final UnidadeRepository repository;

    public UnidadeService(UnidadeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UnidadeDtos.Response> listar() {
        return repository.findAll().stream().map(UnidadeDtos.Response::de).toList();
    }

    @Transactional(readOnly = true)
    public UnidadeDtos.Response buscarPorId(Long id) {
        return repository.findById(id)
                .map(UnidadeDtos.Response::de)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));
    }

    @Transactional
    public UnidadeDtos.Response criar(UnidadeDtos.Request request) {
        if (repository.existsByNomeIgnoreCase(request.nome())) {
            throw new RegraNegocioException("Ja existe uma unidade com esse nome.");
        }

        Unidade unidade = new Unidade();
        unidade.setNome(request.nome());
        unidade.setTipo(request.tipo());
        unidade.setEndereco(request.endereco());
        unidade.setTelefone(request.telefone());

        return UnidadeDtos.Response.de(repository.save(unidade));
    }

    @Transactional
    public UnidadeDtos.Response atualizar(Long id, UnidadeDtos.Request request) {
        Unidade unidade = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));

        unidade.setNome(request.nome());
        unidade.setTipo(request.tipo());
        unidade.setEndereco(request.endereco());
        unidade.setTelefone(request.telefone());

        return UnidadeDtos.Response.de(unidade);
    }
}
