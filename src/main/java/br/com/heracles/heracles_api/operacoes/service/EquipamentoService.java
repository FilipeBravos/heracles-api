package br.com.heracles.heracles_api.operacoes.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.operacoes.domain.*;
import br.com.heracles.heracles_api.operacoes.dto.EquipamentoDtos;
import br.com.heracles.heracles_api.operacoes.repository.ChamadoManutencaoRepository;
import br.com.heracles.heracles_api.operacoes.repository.EquipamentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        return EquipamentoDtos.Response.de(repository.save(equipamento));
    }

    @Transactional
    public EquipamentoDtos.Response atualizar(Long id, EquipamentoDtos.Request request) {
        Equipamento equipamento = carregar(id);
        equipamento.setUnidade(carregarUnidade(request.unidadeId()));
        equipamento.setNome(request.nome().trim());
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

    private Equipamento carregar(Long id) {
        return repository.findWithUnidadeById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Equipamento", id));
    }

    private Unidade carregarUnidade(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id));
    }
}
