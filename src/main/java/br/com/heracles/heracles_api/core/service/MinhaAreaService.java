package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.dto.HistoricoTreinoResponse;
import br.com.heracles.heracles_api.core.dto.MinhaMatriculaResponse;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * O que o aluno ve de si mesmo.
 *
 * Todo metodo aqui recebe o e-mail do token, nunca um id vindo da
 * requisicao. E a diferenca entre "minhas fichas" e "as fichas de quem
 * eu disser": com um id no caminho, bastaria trocar o numero para ler a
 * ficha de outro aluno.
 */
@Service
public class MinhaAreaService {

    private final UsuarioRepository usuarioRepository;
    private final TreinoRepository treinoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final HistoricoTreinoAlunoRepository historicoTreinoRepository;

    public MinhaAreaService(UsuarioRepository usuarioRepository,
                            TreinoRepository treinoRepository,
                            AssinaturaRepository assinaturaRepository,
                            HistoricoTreinoAlunoRepository historicoTreinoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.treinoRepository = treinoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.historicoTreinoRepository = historicoTreinoRepository;
    }

    @Transactional(readOnly = true)
    public List<TreinoResponse> minhasFichas(String emailAutenticado) {
        return treinoRepository.fichasDoAluno(eu(emailAutenticado).getId()).stream()
                .map(TreinoResponse::de)
                .toList();
    }

    /**
     * Fichas que ja foram do aluno e nao sao mais.
     *
     * A atual nao entra aqui — "Meu treino" ja a mostra, e repeti-la seria
     * a mesma ficha em dois lugares com nomes diferentes. Nome, foco e
     * nivel vem do registro histórico, nao da ficha viva: ela pode ter
     * sido renomeada ou apagada desde a troca.
     */
    @Transactional(readOnly = true)
    public List<HistoricoTreinoResponse> historicoDeTreinos(String emailAutenticado) {
        return historicoTreinoRepository.historicoDoAluno(eu(emailAutenticado).getId()).stream()
                .map(HistoricoTreinoResponse::de)
                .toList();
    }

    /**
     * Minha matricula: plano, vencimento e situacao.
     *
     * Vale a vigente — a que nao foi cancelada —, que e a unica que
     * responde "meu acesso esta em dia?". O historico de planos antigos e
     * outra pergunta, e quem a faz e o balcao.
     *
     * Nao ter matricula vigente nao e erro: o aluno acabou de ser
     * cadastrado e ainda nao passou na recepcao, ou a matricula foi
     * cancelada. Nos dois casos ha uma resposta a dar, e um 404 obrigaria
     * a tela a tratar o normal como falha.
     */
    @Transactional(readOnly = true)
    public MinhaMatriculaResponse minhaMatricula(String emailAutenticado) {
        return assinaturaRepository.buscarVigentePorAluno(eu(emailAutenticado).getId())
                .map(assinatura -> MinhaMatriculaResponse.de(assinatura, LocalDate.now()))
                .orElseGet(MinhaMatriculaResponse::semMatricula);
    }

    private Usuario eu(String emailAutenticado) {
        return usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario autenticado nao encontrado."));
    }
}
