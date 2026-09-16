package br.com.heracles.heracles_api.matriculas.service;

import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.dto.PlanoDtos;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class PlanoService {

    private final PlanoRepository repository;
    private final UnidadeRepository unidadeRepository;

    public PlanoService(PlanoRepository repository, UnidadeRepository unidadeRepository) {
        this.repository = repository;
        this.unidadeRepository = unidadeRepository;
    }

    @Transactional(readOnly = true)
    public Page<PlanoDtos.Response> listar(boolean apenasAtivos, Pageable pageable) {
        return repository.buscarPaginado(apenasAtivos, pageable).map(PlanoDtos.Response::de);
    }

    @Transactional(readOnly = true)
    public PlanoDtos.Response buscarPorId(Long id) {
        return PlanoDtos.Response.de(carregar(id));
    }

    @Transactional
    public PlanoDtos.Response criar(PlanoDtos.Request request) {
        if (repository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new RegraNegocioException("Ja existe um plano com esse nome.");
        }

        Plano plano = new Plano();
        aplicar(request, plano);
        return PlanoDtos.Response.de(repository.save(plano));
    }

    @Transactional
    public PlanoDtos.Response atualizar(Long id, PlanoDtos.Request request) {
        Plano plano = carregar(id);

        if (repository.existsByNomeIgnoreCaseAndIdNot(request.nome().trim(), id)) {
            throw new RegraNegocioException("Ja existe outro plano com esse nome.");
        }

        aplicar(request, plano);
        return PlanoDtos.Response.de(plano);
    }

    /**
     * Plano sai de linha em vez de ser apagado: assinaturas apontam para
     * ele, e o valor cobrado precisa continuar rastreavel. Reversivel.
     *
     * Tirar de linha nao mexe em quem ja esta matriculado — quem esta no
     * plano continua nele ate cancelar. O que muda e que ele deixa de
     * aparecer para novas matriculas.
     */
    @Transactional
    public PlanoDtos.Response alternarAtivo(Long id) {
        Plano plano = carregar(id);
        plano.setAtivo(!plano.isAtivo());
        return PlanoDtos.Response.de(plano);
    }

    private void aplicar(PlanoDtos.Request request, Plano plano) {
        plano.setNome(request.nome().trim());
        plano.setValorMensal(request.valorMensal());
        plano.setTipoCobranca(request.tipoCobranca());
        plano.setUnidades(carregarUnidades(request.unidadeIds()));
    }

    /**
     * Carrega uma a uma de proposito: com findAllById, uma unidade
     * inexistente sumiria da lista em silencio e o plano seria salvo
     * cobrindo menos do que o operador escolheu.
     */
    private Set<Unidade> carregarUnidades(Set<Long> ids) {
        Set<Unidade> unidades = new LinkedHashSet<>();
        for (Long id : ids) {
            unidades.add(unidadeRepository.findById(id)
                    .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", id)));
        }
        return unidades;
    }

    private Plano carregar(Long id) {
        return repository.findWithUnidadesById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", id));
    }
}
