package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.dto.DashboardResumoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
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

    private final UsuarioRepository usuarioRepository;
    private final TreinoRepository treinoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ProdutoRepository produtoRepository;
    private final VendaRepository vendaRepository;
    private final AssinaturaRepository assinaturaRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                            TreinoRepository treinoRepository,
                            EquipamentoRepository equipamentoRepository,
                            ProdutoRepository produtoRepository,
                            VendaRepository vendaRepository,
                            AssinaturaRepository assinaturaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.treinoRepository = treinoRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.produtoRepository = produtoRepository;
        this.vendaRepository = vendaRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo() {
        LocalDate hoje = LocalDate.now();
        LocalDate primeiroDiaDoMes = hoje.withDayOfMonth(1);
        LocalDateTime inicioDoMes = primeiroDiaDoMes.atStartOfDay();

        return new DashboardResumoResponse(
                usuarioRepository.countByStatus(StatusUsuario.ATIVO),
                usuarioRepository.countByStatus(StatusUsuario.INATIVO),
                treinoRepository.count(),
                usuarioRepository.contarUsuariosComTreino(),
                assinaturaRepository.countByDataInicioGreaterThanEqual(primeiroDiaDoMes),
                assinaturaRepository.countByStatus(StatusAssinatura.INADIMPLENTE),
                assinaturaRepository.contarVencidas(hoje),
                equipamentoRepository.countByStatusAtual(StatusEquipamento.EM_MANUTENCAO),
                produtoRepository.countComEstoqueBaixo(),
                vendaRepository.countByDataVendaAfter(inicioDoMes),
                vendaRepository.faturamentoDesde(inicioDoMes)
        );
    }
}
