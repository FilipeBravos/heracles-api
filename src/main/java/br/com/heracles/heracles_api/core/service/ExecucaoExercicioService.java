package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.ExecucaoExercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.ExecucaoExercicioDtos;
import br.com.heracles.heracles_api.core.repository.ExecucaoExercicioRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * O registro de execucao de exercicio e a "Meu treino" veem do mesmo lugar:
 * quem esta autenticado, nunca um id vindo da requisicao. Um aluno so
 * registra carga para um exercicio que esteja numa das proprias fichas
 * atuais — o mesmo raciocinio de MinhaAreaService.
 */
@Service
public class ExecucaoExercicioService {

    private final ExecucaoExercicioRepository repository;
    private final TreinoRepository treinoRepository;
    private final UsuarioRepository usuarioRepository;

    public ExecucaoExercicioService(ExecucaoExercicioRepository repository,
                                    TreinoRepository treinoRepository,
                                    UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.treinoRepository = treinoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ExecucaoExercicioDtos.Response registrar(String emailAutenticado, ExecucaoExercicioDtos.Request request) {
        Usuario aluno = eu(emailAutenticado);
        Exercicio exercicio = exercicioDaFichaAtual(aluno.getId(), request.exercicioId());

        ExecucaoExercicio execucao = new ExecucaoExercicio();
        execucao.setAluno(aluno);
        execucao.setExercicio(exercicio);
        execucao.setExercicioNome(exercicio.getNome());
        execucao.setDataExecucao(request.dataExecucao());
        execucao.setSeriesRealizadas(request.seriesRealizadas());
        execucao.setRepeticoesRealizadas(request.repeticoesRealizadas());
        execucao.setCargaRealizada(request.cargaRealizada());
        execucao.setObservacao(vazioComoNulo(request.observacao()));

        return ExecucaoExercicioDtos.Response.de(repository.save(execucao));
    }

    /** A evolucao de um exercicio especifico, do mais recente pro mais antigo. */
    @Transactional(readOnly = true)
    public List<ExecucaoExercicioDtos.Response> minhasExecucoes(String emailAutenticado, Long exercicioId) {
        Long alunoId = eu(emailAutenticado).getId();
        return repository.findByAlunoIdAndExercicioIdOrderByDataExecucaoDesc(alunoId, exercicioId).stream()
                .map(ExecucaoExercicioDtos.Response::de)
                .toList();
    }

    /**
     * So aceita um exercicio que esteja numa ficha vinculada ao aluno agora
     * — sem isso, bastaria trocar o id na requisicao para registrar carga
     * num exercicio de qualquer outro aluno.
     */
    private Exercicio exercicioDaFichaAtual(Long alunoId, Long exercicioId) {
        List<Treino> fichas = treinoRepository.fichasDoAluno(alunoId);
        return fichas.stream()
                .flatMap(treino -> treino.getExercicios().stream())
                .filter(exercicio -> exercicio.getId().equals(exercicioId))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException(
                        "Este exercicio nao esta em nenhuma das suas fichas atuais."));
    }

    private String vazioComoNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private Usuario eu(String emailAutenticado) {
        return usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario autenticado nao encontrado."));
    }
}
