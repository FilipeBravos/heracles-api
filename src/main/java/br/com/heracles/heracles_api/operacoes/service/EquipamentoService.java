package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.*;
import br.com.heracles.heracles_api.operacoes.dto.EquipamentoDtos;
import br.com.heracles.heracles_api.operacoes.dto.LinhaEquipamentoProblematico;
import br.com.heracles.heracles_api.operacoes.dto.LinhaManutencaoPorUnidade;
import br.com.heracles.heracles_api.operacoes.dto.LinhaManutencaoPreventiva;
import br.com.heracles.heracles_api.operacoes.dto.LinhaTempoResolucao;
import br.com.heracles.heracles_api.operacoes.dto.LinhaUltimaManutencao;
import br.com.heracles.heracles_api.operacoes.repository.ChamadoManutencaoRepository;
import br.com.heracles.heracles_api.operacoes.repository.EquipamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EquipamentoService {

    private final EquipamentoRepository repository;
    private final ChamadoManutencaoRepository chamadoRepository;
    private final UnidadeRepository unidadeRepository;

    public EquipamentoService(EquipamentoRepository repository,
                              ChamadoManutencaoRepository chamadoRepository,
                              UnidadeRepository unidadeRepository) {
        this.repository = repository;
        this.chamadoRepository = chamadoRepository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public Page<EquipamentoDtos.Response> listar(Pageable pageable) {
        return repository.buscarPaginado(pageable).map(EquipamentoDtos.Response::de);
    }

    @Transactional(readOnly = true)
    public List<EquipamentoDtos.ChamadoResponse> historico(Long equipamentoId) {
        carregar(equipamentoId); // 404 antes de devolver lista vazia de id inexistente
        return chamadoRepository.findByEquipamentoIdOrderByDataChamadoDesc(equipamentoId).stream()
                .map(EquipamentoDtos.ChamadoResponse::de)
                .toList();
    }

    @Transactional
    public EquipamentoDtos.Response criar(EquipamentoDtos.Request request) {
        Unidade unidade = carregarUnidade(request.unidadeId());

        if (repository.existsByUnidadeIdAndNomeIgnoreCase(unidade.getId(), request.nome())) {
            throw new RegraNegocioException("Ja existe um equipamento com esse nome nesta unidade.");
        }

        Equipamento equipamento = new Equipamento();
        equipamento.setUnidade(unidade);
        equipamento.setNome(request.nome().trim());
        equipamento.setStatusAtual(StatusEquipamento.OK);
        equipamento.setIntervaloDiasManutencao(request.intervaloDiasManutencao());

        return EquipamentoDtos.Response.de(repository.save(equipamento));
    }

    @Transactional
    public EquipamentoDtos.Response atualizar(Long id, EquipamentoDtos.Request request) {
        Equipamento equipamento = carregar(id);
        equipamento.setUnidade(carregarUnidade(request.unidadeId()));
        equipamento.setNome(request.nome().trim());
        equipamento.setIntervaloDiasManutencao(request.intervaloDiasManutencao());
        // O status nao se edita aqui: ele e consequencia dos chamados.
        return EquipamentoDtos.Response.de(equipamento);
    }

    /**
     * Abre um chamado e tira o equipamento de operacao.
     *
     * As duas coisas acontecem juntas de proposito: um equipamento com
     * chamado aberto que continuasse marcado como OK e exatamente o
     * estado que faz um aluno subir num aparelho quebrado.
     */
    @Transactional
    public EquipamentoDtos.ChamadoResponse abrirChamado(Long equipamentoId, EquipamentoDtos.AbrirChamado request) {
        Equipamento equipamento = carregar(equipamentoId);

        if (chamadoRepository.findByEquipamentoIdAndStatus(equipamentoId, StatusChamado.ABERTO).isPresent()) {
            throw new RegraNegocioException(
                    "Este equipamento ja tem um chamado aberto. Resolva o atual antes de abrir outro.");
        }

        ChamadoManutencao chamado = new ChamadoManutencao();
        chamado.setEquipamento(equipamento);
        chamado.setDescricaoProblema(request.descricaoProblema().trim());
        chamado.setStatus(StatusChamado.ABERTO);

        equipamento.setStatusAtual(StatusEquipamento.EM_MANUTENCAO);

        return EquipamentoDtos.ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    /** Resolve o chamado, registra o custo e devolve o equipamento a operacao. */
    @Transactional
    public EquipamentoDtos.ChamadoResponse resolverChamado(Long chamadoId, EquipamentoDtos.ResolverChamado request) {
        ChamadoManutencao chamado = chamadoRepository.findById(chamadoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Chamado", chamadoId));

        if (!chamado.estaAberto()) {
            throw new RegraNegocioException("Este chamado ja foi resolvido.");
        }

        chamado.resolver(request.custoReparo());
        chamado.getEquipamento().setStatusAtual(StatusEquipamento.OK);

        return EquipamentoDtos.ChamadoResponse.de(chamado);
    }

    /**
     * O painel de manutencao: custo, tempo medio de resolucao, os
     * equipamentos mais problematicos e a comparacao entre unidades no
     * periodo.
     */
    @Transactional(readOnly = true)
    public EquipamentoDtos.PainelManutencao relatorio(int dias) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();

        long quantidadeChamados = chamadoRepository.countByDataChamadoAfter(desde);
        long quantidadeAbertos = chamadoRepository.countByDataChamadoAfterAndStatus(desde, StatusChamado.ABERTO);
        BigDecimal custoTotal = chamadoRepository.custoTotalDesde(desde);

        List<LinhaTempoResolucao> tempos = chamadoRepository.temposResolucaoDesde(desde);
        BigDecimal tempoMedioResolucaoHoras = tempos.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(tempos.stream()
                                .mapToLong(t -> Duration.between(t.dataChamado(), t.dataResolucao()).toMinutes())
                                .average().orElse(0) / 60.0)
                        .setScale(1, RoundingMode.HALF_UP);

        List<LinhaEquipamentoProblematico> maisProblematicos =
                chamadoRepository.equipamentosProblematicosDesde(desde, PageRequest.of(0, 10));
        List<LinhaManutencaoPorUnidade> porUnidade = chamadoRepository.manutencaoPorUnidadeDesde(desde);

        return new EquipamentoDtos.PainelManutencao(
                dias, quantidadeChamados, quantidadeAbertos, custoTotal, tempoMedioResolucaoHoras,
                maisProblematicos, porUnidade);
    }

    /**
     * Manutencao preventiva vencida ou vencendo hoje: equipamentos com
     * intervalo configurado cuja proxima revisao — ultimo chamado
     * resolvido, ou o cadastro se nunca teve nenhum — ja chegou.
     */
    @Transactional(readOnly = true)
    public List<LinhaManutencaoPreventiva> relatorioManutencaoPreventiva() {
        List<Equipamento> equipamentos = repository.findByIntervaloDiasManutencaoIsNotNull();
        Map<Long, LocalDateTime> ultimasManutencoes = chamadoRepository.ultimaResolucaoPorEquipamento().stream()
                .collect(Collectors.toMap(LinhaUltimaManutencao::equipamentoId, LinhaUltimaManutencao::ultimaResolucao));

        LocalDate hoje = LocalDate.now();

        return equipamentos.stream()
                .map(equipamento -> {
                    LocalDateTime ultima = ultimasManutencoes.getOrDefault(
                            equipamento.getId(), equipamento.getCadastradoEm());
                    LocalDate ultimaData = ultima.toLocalDate();
                    LocalDate proxima = ultimaData.plusDays(equipamento.getIntervaloDiasManutencao());
                    long diasAtraso = ChronoUnit.DAYS.between(proxima, hoje);

                    return new LinhaManutencaoPreventiva(
                            equipamento.getId(), equipamento.getNome(),
                            equipamento.getUnidade().getId(), equipamento.getUnidade().getNome(),
                            equipamento.getIntervaloDiasManutencao(), ultimaData, proxima, diasAtraso);
                })
                .filter(linha -> linha.diasAtraso() >= 0)
                .sorted(Comparator.comparingLong(LinhaManutencaoPreventiva::diasAtraso).reversed())
                .toList();
    }

    private Equipamento carregar(Long id) {
        return repository.findWithUnidadeById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Equipamento", id));
    }

    private Unidade carregarUnidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));
    }
}
