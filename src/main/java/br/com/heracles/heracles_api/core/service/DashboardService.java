package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.StatusUsuario;
import br.com.heracles.heracles_api.core.dto.DashboardResumoResponse;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Alimenta os cartoes da tela inicial com contagens reais. */
@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final TreinoRepository treinoRepository;

    public DashboardService(UsuarioRepository usuarioRepository, TreinoRepository treinoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.treinoRepository = treinoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo() {
        LocalDateTime inicioDoMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return new DashboardResumoResponse(
                usuarioRepository.countByStatus(StatusUsuario.ATIVO),
                usuarioRepository.countByStatus(StatusUsuario.INATIVO),
                treinoRepository.count(),
                usuarioRepository.contarUsuariosComTreino(),
                usuarioRepository.countByDataCadastroAfter(inicioDoMes)
        );
    }
}
