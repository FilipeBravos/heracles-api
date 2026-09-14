package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.dto.DashboardResumoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.operacoes.domain.StatusEquipamento;
import br.com.heracles.heracles_api.operacoes.repository.EquipamentoRepository;
import br.com.heracles.heracles_api.operacoes.repository.ProdutoRepository;
import br.com.heracles.heracles_api.operacoes.repository.VendaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Alimenta os cartoes da tela inicial com contagens reais. */
@Service
public class DashboardService {

    /** Estoque igual ou abaixo disso conta como baixo na visao geral. */
    private static final int LIMITE_ESTOQUE_BAIXO = 5;

    private final UsuarioRepository usuarioRepository;
    private final TreinoRepository treinoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                            TreinoRepository treinoRepository,
                            EquipamentoRepository equipamentoRepository,
                            ProdutoRepository produtoRepository,
                            VendaRepository vendaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.treinoRepository = treinoRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo() {
        LocalDateTime inicioDoMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return new DashboardResumoResponse(
                usuarioRepository.countByStatus(StatusUsuario.ATIVO),
                usuarioRepository.countByStatus(StatusUsuario.INATIVO),
                treinoRepository.count(),
                usuarioRepository.contarUsuariosComTreino(),
                usuarioRepository.countByDataCadastroAfter(inicioDoMes),
                equipamentoRepository.countByStatusAtual(StatusEquipamento.EM_MANUTENCAO),
                produtoRepository.countByAtivoTrueAndQuantidadeEstoqueLessThanEqual(LIMITE_ESTOQUE_BAIXO),
                vendaRepository.countByDataVendaAfter(inicioDoMes),
                vendaRepository.faturamentoDesde(inicioDoMes)
        );
    }
}
