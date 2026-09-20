package br.com.heracles.heracles_api.core.service;

import br.com.heracles.heracles_api.core.domain.Exercicio;
import br.com.heracles.heracles_api.core.domain.Treino;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.domain.HistoricoTreinoAluno;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.dto.HistoricoTreinoResponse;
import br.com.heracles.heracles_api.core.dto.MeusDadosDtos;
import br.com.heracles.heracles_api.core.dto.MinhaMatriculaResponse;
import br.com.heracles.heracles_api.core.dto.TreinoResponse;
import br.com.heracles.heracles_api.core.repository.HistoricoTreinoAlunoRepository;
import br.com.heracles.heracles_api.core.repository.TreinoRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.TipoCobranca;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MinhaAreaServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TreinoRepository treinoRepository;
    @Mock private AssinaturaRepository assinaturaRepository;
    @Mock private HistoricoTreinoAlunoRepository historicoTreinoRepository;
    @Mock private UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private MinhaAreaService service;
    private Usuario marina;
    private Usuario bruno;

    @BeforeEach
    void preparar() {
        service = new MinhaAreaService(usuarioRepository, treinoRepository, assinaturaRepository,
                historicoTreinoRepository, passwordEncoder, usuarioService);

        marina = new Usuario();
        marina.setId(10L);
        marina.setNome("Marina Alves");
        marina.setEmail("marina@ex.com");
        marina.setTelefone("11999990000");
        marina.setSenhaHash(passwordEncoder.encode("SenhaAtual123"));

        bruno = new Usuario();
        bruno.setId(20L);
        bruno.setNome("Bruno Dias");
        bruno.setEmail("bruno@ex.com");

        given(usuarioRepository.findByEmailIgnoreCase("marina@ex.com")).willReturn(Optional.of(marina));
        given(usuarioRepository.findByEmailIgnoreCase("bruno@ex.com")).willReturn(Optional.of(bruno));
    }

    @Test
    @DisplayName("Consulta as fichas do id de quem o e-mail do token identifica")
    void consultaPeloDonoDoToken() {
        ArgumentCaptor<Long> alunoId = ArgumentCaptor.forClass(Long.class);
        given(treinoRepository.fichasDoAluno(alunoId.capture())).willReturn(List.of());

        service.minhasFichas("marina@ex.com");

        // O id nunca vem da requisicao: e resolvido a partir do token. Sem
        // isso, bastaria trocar um numero para ler a ficha de outro aluno.
        assertThat(alunoId.getValue()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Dois alunos autenticados recebem cada um as suas fichas")
    void cadaUmVeAsSuas() {
        Treino dela = fichaCom("Ficha A - Peito e Triceps");
        Treino dele = fichaCom("Circuito Funcional");
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of(dela));
        given(treinoRepository.fichasDoAluno(20L)).willReturn(List.of(dele));

        assertThat(service.minhasFichas("marina@ex.com"))
                .extracting(TreinoResponse::nome).containsExactly("Ficha A - Peito e Triceps");
        assertThat(service.minhasFichas("bruno@ex.com"))
                .extracting(TreinoResponse::nome).containsExactly("Circuito Funcional");
    }

    @Test
    @DisplayName("Aluno sem ficha vinculada recebe lista vazia, nao erro")
    void semFichaNaoEhErro() {
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of());

        // Nao ter ficha e um estado normal — o aluno acabou de se matricular
        // e o professor ainda nao montou. A tela diz isso; a API nao estoura.
        assertThat(service.minhasFichas("marina@ex.com")).isEmpty();
    }

    @Test
    @DisplayName("A ficha vem com a prescricao completa dos exercicios")
    void fichaVemComPrescricao() {
        given(treinoRepository.fichasDoAluno(10L)).willReturn(List.of(fichaCom("Ficha A")));

        TreinoResponse ficha = service.minhasFichas("marina@ex.com").get(0);

        assertThat(ficha.exercicios()).hasSize(1);
        assertThat(ficha.exercicios().get(0).nome()).isEqualTo("Supino reto");
        assertThat(ficha.exercicios().get(0).series()).isEqualTo(4);
        assertThat(ficha.exercicios().get(0).repeticoesMin()).isEqualTo(10);
        assertThat(ficha.exercicios().get(0).repeticoesMax()).isEqualTo(12);
        // 4 series x 10 repeticoes minimas.
        assertThat(ficha.volumePrescritoMinimo()).isEqualTo(40);
    }

    @Test
    @DisplayName("Token de usuario que sumiu da base nao vira 500")
    void tokenOrfaoNaoEstoura() {
        assertThatThrownBy(() -> service.minhasFichas("fantasma@ex.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Consulta a matricula do id de quem o e-mail do token identifica")
    void matriculaSaiDoToken() {
        ArgumentCaptor<Long> alunoId = ArgumentCaptor.forClass(Long.class);
        given(assinaturaRepository.buscarVigentePorAluno(alunoId.capture())).willReturn(Optional.empty());

        service.minhaMatricula("marina@ex.com");

        assertThat(alunoId.getValue()).isEqualTo(10L);
    }

    @Test
    @DisplayName("A matricula vem com plano, vencimento e as unidades cobertas")
    void matriculaVemCompleta() {
        given(assinaturaRepository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(matriculaAte(LocalDate.now().plusDays(20))));

        MinhaMatriculaResponse minha = service.minhaMatricula("marina@ex.com");

        assertThat(minha.temMatricula()).isTrue();
        assertThat(minha.planoNome()).isEqualTo("Rede Total");
        assertThat(minha.valorMensal()).isEqualByComparingTo("149.90");
        assertThat(minha.tipoCobranca()).isEqualTo(TipoCobranca.RECORRENTE);
        assertThat(minha.origem()).isEqualTo(OrigemAssinatura.DIRETO);
        assertThat(minha.status()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(minha.vencida()).isFalse();
        // A unidade e a pergunta pratica do aluno: "meu plano cobre onde eu treino?".
        assertThat(minha.unidades()).containsExactly("Centro", "Zona Sul");
    }

    @Test
    @DisplayName("Aluno sem matricula vigente recebe resposta, nao erro")
    void semMatriculaNaoEhErro() {
        given(assinaturaRepository.buscarVigentePorAluno(10L)).willReturn(Optional.empty());

        MinhaMatriculaResponse minha = service.minhaMatricula("marina@ex.com");

        // Recem-cadastrado que ainda nao passou na recepcao, ou matricula
        // cancelada. A tela tem o que dizer; um 404 faria o normal parecer falha.
        assertThat(minha.temMatricula()).isFalse();
        assertThat(minha.planoNome()).isNull();
        assertThat(minha.unidades()).isEmpty();
    }

    @Test
    @DisplayName("Quem conta os dias que faltam e o servidor, com o sinal certo")
    void diasContadosNoServidor() {
        given(assinaturaRepository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(matriculaAte(LocalDate.now().plusDays(5))));
        assertThat(service.minhaMatricula("marina@ex.com").diasParaVencer()).isEqualTo(5);

        given(assinaturaRepository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(matriculaAte(LocalDate.now().minusDays(3))));
        MinhaMatriculaResponse vencida = service.minhaMatricula("marina@ex.com");

        // Negativo quando ja venceu — e a mesma conta que a catraca faz.
        assertThat(vencida.diasParaVencer()).isEqualTo(-3);
        assertThat(vencida.vencida()).isTrue();
    }

    @Test
    @DisplayName("No proprio dia do vencimento a matricula ainda nao esta vencida")
    void noDiaDoVencimentoAindaVale() {
        given(assinaturaRepository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(matriculaAte(LocalDate.now())));

        MinhaMatriculaResponse minha = service.minhaMatricula("marina@ex.com");

        // O aluno pagou por este dia. A tela do aluno e a catraca precisam
        // concordar nesta borda.
        assertThat(minha.vencida()).isFalse();
        assertThat(minha.diasParaVencer()).isZero();
    }

    @Test
    @DisplayName("Inadimplente chega como tal, sem virar vencida")
    void inadimplenteChegaComoTal() {
        Assinatura assinatura = matriculaAte(LocalDate.now().plusDays(10));
        assinatura.setStatus(StatusAssinatura.INADIMPLENTE);
        given(assinaturaRepository.buscarVigentePorAluno(10L)).willReturn(Optional.of(assinatura));

        MinhaMatriculaResponse minha = service.minhaMatricula("marina@ex.com");

        // Atraso de pagamento e vencimento sao coisas diferentes, e levam a
        // conversas diferentes na recepcao.
        assertThat(minha.status()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(minha.vencida()).isFalse();
    }

    @Test
    @DisplayName("Token de usuario que sumiu da base nao vira 500 tambem na matricula")
    void tokenOrfaoNaoEstouraNaMatricula() {
        assertThatThrownBy(() -> service.minhaMatricula("fantasma@ex.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("O historico consulta pelo id de quem o e-mail do token identifica")
    void historicoSaiDoToken() {
        ArgumentCaptor<Long> alunoId = ArgumentCaptor.forClass(Long.class);
        given(historicoTreinoRepository.historicoDoAluno(alunoId.capture())).willReturn(List.of());

        service.historicoDeTreinos("marina@ex.com");

        assertThat(alunoId.getValue()).isEqualTo(10L);
    }

    @Test
    @DisplayName("O historico vem com nome, foco e nivel do registro, nao da ficha viva")
    void historicoVemComOsDadosDoRegistro() {
        HistoricoTreinoAluno periodo = new HistoricoTreinoAluno();
        periodo.setTreinoNome("Ficha A - Peito e Triceps");
        periodo.setTreinoFoco("Hipertrofia");
        periodo.setTreinoNivel("Intermediario");
        periodo.setVinculadoEm(LocalDateTime.of(2026, 3, 1, 8, 0));
        periodo.setDesvinculadoEm(LocalDateTime.of(2026, 6, 15, 8, 0));
        given(historicoTreinoRepository.historicoDoAluno(10L)).willReturn(List.of(periodo));

        HistoricoTreinoResponse resposta = service.historicoDeTreinos("marina@ex.com").get(0);

        assertThat(resposta.nome()).isEqualTo("Ficha A - Peito e Triceps");
        assertThat(resposta.foco()).isEqualTo("Hipertrofia");
        assertThat(resposta.vinculadoEm()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(resposta.desvinculadoEm()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    @DisplayName("Sem ficha anterior o historico vem vazio, nao erro")
    void semHistoricoNaoEhErro() {
        given(historicoTreinoRepository.historicoDoAluno(10L)).willReturn(List.of());

        assertThat(service.historicoDeTreinos("marina@ex.com")).isEmpty();
    }

    @Test
    @DisplayName("Token de usuario que sumiu da base nao vira 500 tambem no historico")
    void tokenOrfaoNaoEstouraNoHistorico() {
        assertThatThrownBy(() -> service.historicoDeTreinos("fantasma@ex.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Assinatura matriculaAte(LocalDate vencimento) {
        Unidade centro = new Unidade();
        centro.setId(1L);
        centro.setNome("Centro");
        Unidade zonaSul = new Unidade();
        zonaSul.setId(2L);
        zonaSul.setNome("Zona Sul");

        Plano plano = new Plano();
        plano.setId(7L);
        plano.setNome("Rede Total");
        plano.setValorMensal(new BigDecimal("149.90"));
        plano.setTipoCobranca(TipoCobranca.RECORRENTE);
        plano.setUnidades(new LinkedHashSet<>(List.of(centro, zonaSul)));

        Assinatura assinatura = new Assinatura();
        assinatura.setId(99L);
        assinatura.setAluno(marina);
        assinatura.setPlano(plano);
        assinatura.setOrigem(OrigemAssinatura.DIRETO);
        assinatura.setDataInicio(vencimento.minusMonths(1));
        assinatura.setDataVencimento(vencimento);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        return assinatura;
    }

    @Test
    @DisplayName("Meus dados sao os de quem o token identifica")
    void meusDadosSaoOsDoToken() {
        MeusDadosDtos.Response resposta = service.meusDados("marina@ex.com");

        assertThat(resposta.nome()).isEqualTo("Marina Alves");
        assertThat(resposta.email()).isEqualTo("marina@ex.com");
        assertThat(resposta.telefone()).isEqualTo("11999990000");
    }

    @Test
    @DisplayName("Atualizar meus dados muda nome e telefone, nunca e-mail ou CPF")
    void atualizarMeusDadosMudaNomeETelefone() {
        MeusDadosDtos.Response resposta = service.atualizarMeusDados(
                "marina@ex.com", new MeusDadosDtos.Atualizar("Marina Alves Souza", "11988887777"));

        assertThat(resposta.nome()).isEqualTo("Marina Alves Souza");
        assertThat(resposta.telefone()).isEqualTo("11988887777");
        // O contrato de entrada nem tem campo de e-mail ou CPF: não há o
        // que sequestrar mudando um dos dois por engano ou de proposito.
        assertThat(resposta.email()).isEqualTo("marina@ex.com");
        assertThat(marina.getEmail()).isEqualTo("marina@ex.com");
    }

    @Test
    @DisplayName("Trocar a senha exige acertar a atual")
    void trocarSenhaExigeAAtual() {
        assertThatThrownBy(() -> service.trocarSenha(
                "marina@ex.com", new MeusDadosDtos.TrocarSenha("SenhaErrada", "NovaSenha123")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("incorreta");

        // A senha antiga continua valendo: a tentativa nao teve efeito.
        assertThat(passwordEncoder.matches("SenhaAtual123", marina.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("Acertando a senha atual, a nova senha passa a valer")
    void trocarSenhaComAAtualCorreta() {
        service.trocarSenha("marina@ex.com", new MeusDadosDtos.TrocarSenha("SenhaAtual123", "NovaSenha123"));

        assertThat(passwordEncoder.matches("NovaSenha123", marina.getSenhaHash())).isTrue();
        assertThat(passwordEncoder.matches("SenhaAtual123", marina.getSenhaHash())).isFalse();
    }

    @Test
    @DisplayName("Token de usuario que sumiu da base nao vira 500 tambem em meus dados")
    void tokenOrfaoNaoEstouraEmMeusDados() {
        assertThatThrownBy(() -> service.meusDados("fantasma@ex.com"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Treino fichaCom(String nome) {
        Treino treino = new Treino();
        treino.setId(1L);
        treino.setNome(nome);
        treino.setFoco("Hipertrofia");
        treino.setNivel("Intermediario");

        Exercicio exercicio = new Exercicio();
        exercicio.setId(1L);
        exercicio.setNome("Supino reto");
        exercicio.setSeries(4);
        exercicio.setRepeticoesMin(10);
        exercicio.setRepeticoesMax(12);
        exercicio.setCarga("70% 1RM");
        exercicio.setOrdem(0);
        treino.adicionarExercicio(exercicio);

        return treino;
    }

    // ---------------------------------------------------------------
    // Avaliacao fisica: so delega, resolvendo o id pelo token
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Meu historico de avaliacoes fisicas usa o id de quem o token identifica")
    void minhasAvaliacoesFisicasUsaIdDoToken() {
        service.minhasAvaliacoesFisicas("marina@ex.com");

        verify(usuarioService).historicoAvaliacoesFisicas(10L);
    }

    @Test
    @DisplayName("Meu comparativo fisico usa o id de quem o token identifica")
    void meuComparativoFisicoUsaIdDoToken() {
        service.meuComparativoFisico("bruno@ex.com", 1L, 2L);

        verify(usuarioService).compararAvaliacoesFisicas(20L, 1L, 2L);
    }

    @Test
    @DisplayName("Minha foto de avaliacao fisica usa o id de quem o token identifica")
    void minhaFotoAvaliacaoFisicaUsaIdDoToken() {
        service.minhaFotoAvaliacaoFisica("marina@ex.com", 5L, 50L);

        verify(usuarioService).buscarFotoAvaliacaoFisica(10L, 5L, 50L);
    }
}
