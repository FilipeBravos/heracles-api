package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.ExecucaoExercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.ExecucaoExercicioDtos;
import br.com.heracles.heracles_api.core.dto.LinhaAdesaoTreino;
import br.com.heracles.heracles_api.core.dto.LinhaExecucaoParaAdesao;
import br.com.heracles.heracles_api.core.repository.ExecucaoExercicioRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * O registro de execucao de exercicio e a "Meu treino" veem do mesmo lugar:
 * quem esta autenticado, nunca um id vindo da requisicao. Um aluno so
 * registra carga para um exercicio que esteja numa das proprias fichas
 * atuais — o mesmo raciocinio de MinhaAreaService.
 */
@Service
public class ExecucaoExercicioService {

    /**
     * Amostra minima de execucoes pra um aluno entrar no relatorio de
     * adesao — uma unica execucao (boa ou ruim) nao sustenta uma taxa.
     */
    public static final int QUANTIDADE_MINIMA_EXECUCOES_ADESAO_PADRAO = 4;

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

    /**
     * Adesao ao treino por aluno, do pior pro melhor: volume prescrito
     * (series vezes repeticoes minimas) contra volume realizado, somado
     * por aluno no periodo. So entram execucoes cujo exercicio ainda
     * existe na ficha de origem — quem foi removido da ficha nao tem
     * mais prescricao pra comparar. So entra aluno com pelo menos
     * `quantidadeMinima` execucoes no periodo.
     */
    @Transactional(readOnly = true)
    public List<LinhaAdesaoTreino> adesaoPorAluno(int dias, int quantidadeMinima) {
        LocalDate desde = LocalDate.now().minusDays(dias);
        List<LinhaExecucaoParaAdesao> linhas = repository.execucoesParaAdesaoDesde(desde);

        Map<Long, List<LinhaExecucaoParaAdesao>> porAluno = linhas.stream()
                .collect(Collectors.groupingBy(LinhaExecucaoParaAdesao::alunoId));

        return porAluno.values().stream()
                .map(this::linhaAdesao)
                .filter(linha -> linha.quantidadeExecucoes() >= quantidadeMinima)
                .sorted(Comparator.comparing(LinhaAdesaoTreino::taxaAdesao))
                .toList();
    }

    private LinhaAdesaoTreino linhaAdesao(List<LinhaExecucaoParaAdesao> linhas) {
        long volumePrescrito = linhas.stream()
                .mapToLong(l -> (long) l.series() * l.repeticoesMin())
                .sum();
        long volumeRealizado = linhas.stream()
                .mapToLong(l -> (long) l.seriesRealizadas() * l.repeticoesRealizadas())
                .sum();
        BigDecimal taxaAdesao = volumePrescrito > 0
                ? BigDecimal.valueOf(volumeRealizado * 100).divide(BigDecimal.valueOf(volumePrescrito), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LinhaExecucaoParaAdesao primeira = linhas.get(0);
        return new LinhaAdesaoTreino(
                primeira.alunoId(), primeira.alunoNome(), linhas.size(), volumePrescrito, volumeRealizado, taxaAdesao);
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
