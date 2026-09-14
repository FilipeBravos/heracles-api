package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.dto.TreinoRequest;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TreinoService {

    private final TreinoRepository repository;

    public TreinoService(TreinoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<TreinoResponse> listar(Pageable pageable) {
        return repository.buscarPaginadoComExercicios(pageable).map(TreinoResponse::de);
    }

    @Transactional(readOnly = true)
    public TreinoResponse buscarPorId(Long id) {
        return TreinoResponse.de(carregar(id));
    }

    @Transactional
    public TreinoResponse criar(TreinoRequest request) {
        Treino treino = new Treino();
        treino.setNome(request.nome());
        treino.setFoco(request.foco());
        treino.setNivel(request.nivel());

        int ordem = 0;
        for (TreinoRequest.ExercicioRequest exercicioRequest : request.exercicios()) {
            Exercicio exercicio = new Exercicio();
            aplicar(exercicioRequest, exercicio, ordem++);
            exercicio.setTreino(treino);
            treino.getExercicios().add(exercicio);
        }

        return TreinoResponse.de(repository.save(treino));
    }

    /**
     * Atualiza a ficha reconciliando os exercicios por id.
     *
     * A versao anterior chamava clear() numa colecao com orphanRemoval e
     * recolocava instancias destacadas que ainda carregavam o id original,
     * pedindo ao Hibernate que apagasse e persistisse as mesmas linhas no
     * mesmo flush. Aqui cada exercicio enviado e classificado: atualiza o
     * que ja existe, insere o que e novo, remove o que saiu da lista.
     */
    @Transactional
    public TreinoResponse atualizar(Long id, TreinoRequest request) {
        Treino treino = carregar(id);
        treino.setNome(request.nome());
        treino.setFoco(request.foco());
        treino.setNivel(request.nivel());

        Map<Long, Exercicio> existentes = treino.getExercicios().stream()
                .collect(Collectors.toMap(Exercicio::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<Exercicio> resultado = new java.util.ArrayList<>();
        int ordem = 0;

        for (TreinoRequest.ExercicioRequest exercicioRequest : request.exercicios()) {
            Exercicio exercicio;

            if (exercicioRequest.id() != null) {
                exercicio = existentes.remove(exercicioRequest.id());
                if (exercicio == null) {
                    // Id que nao pertence a esta ficha: tratamos como novo em vez de
                    // deixar o cliente sequestrar um exercicio de outro treino.
                    exercicio = new Exercicio();
                    exercicio.setTreino(treino);
                }
            } else {
                exercicio = new Exercicio();
                exercicio.setTreino(treino);
            }

            aplicar(exercicioRequest, exercicio, ordem++);
            resultado.add(exercicio);
        }

        // O que sobrou em "existentes" nao veio na requisicao: orphanRemoval apaga.
        treino.getExercicios().clear();
        treino.getExercicios().addAll(resultado);

        return TreinoResponse.de(treino);
    }

    @Transactional
    public void deletar(Long id) {
        Treino treino = repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Treino", id));

        // Desfaz os vinculos N:N antes de apagar, para que nenhum aluno fique
        // apontando para uma ficha inexistente.
        treino.getUsuarios().forEach(usuario -> usuario.getTreinos().remove(treino));
        treino.getUsuarios().clear();

        repository.delete(treino);
    }

    private void aplicar(TreinoRequest.ExercicioRequest request, Exercicio exercicio, int ordem) {
        exercicio.setNome(request.nome());
        exercicio.setSeries(request.series());
        exercicio.setRepeticoesMin(request.repeticoesMin());
        exercicio.setRepeticoesMax(request.repeticoesMax());
        exercicio.setCarga(vazioComoNulo(request.carga()));
        exercicio.setObservacoes(vazioComoNulo(request.observacoes()));
        exercicio.setOrdem(ordem);
    }

    /** Campo opcional em branco vira nulo, para nao guardar string vazia. */
    private String vazioComoNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private Treino carregar(Long id) {
        return repository.findWithExerciciosById(Objects.requireNonNull(id))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Treino", id));
    }
}
