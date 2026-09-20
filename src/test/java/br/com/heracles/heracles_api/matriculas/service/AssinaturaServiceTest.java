package br.com.heracles.heracles_api.matriculas.service;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.*;
import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos;
import br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada;
import br.com.heracles.heracles_api.matriculas.dto.ContagemMensal;
import br.com.heracles.heracles_api.matriculas.dto.LembreteDtos;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import br.com.heracles.heracles_api.matriculas.repository.CheckinRepository;
import br.com.heracles.heracles_api.matriculas.repository.CobrancaRepository;
import br.com.heracles.heracles_api.matriculas.repository.LembreteEnviadoRepository;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssinaturaServiceTest {

    @Mock private AssinaturaRepository repository;
    @Mock private PlanoRepository planoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private CobrancaRepository cobrancaRepository;
    @Mock private CheckinRepository checkinRepository;
    @Mock private LembreteEnviadoRepository lembreteRepository;

    private AssinaturaService service;

    private Usuario aluno;
    private Unidade centro;
    private Unidade zonaSul;
    private Plano mensalCentro;

    @BeforeEach
    void preparar() {
        service = new AssinaturaService(
                repository, planoRepository, usuarioRepository, unidadeRepository, cobrancaRepository,
                checkinRepository, lembreteRepository);

        centro = new Unidade();
        centro.setId(1L);
        centro.setNome("Unidade Centro");

        zonaSul = new Unidade();
        zonaSul.setId(2L);
        zonaSul.setNome("Unidade Zona Sul");

        aluno = new Usuario();
        aluno.setId(10L);
        aluno.setNome("Marina Alves");
        aluno.setTipoPerfil(TipoPerfil.ALUNO);

        mensalCentro = new Plano();
        mensalCentro.setId(3L);
        mensalCentro.setNome("Mensal Centro");
        mensalCentro.setValorMensal(new BigDecimal("129.90"));
        mensalCentro.setTipoCobranca(TipoCobranca.RECORRENTE);
        mensalCentro.setAtivo(true);
        mensalCentro.setUnidades(Set.of(centro));

        given(usuarioRepository.findById(10L)).willReturn(Optional.of(aluno));
        given(unidadeRepository.findById(1L)).willReturn(Optional.of(centro));
        given(unidadeRepository.findById(2L)).willReturn(Optional.of(zonaSul));
        given(planoRepository.findWithUnidadesById(3L)).willReturn(Optional.of(mensalCentro));
        given(repository.buscarVigentePorAluno(10L)).willReturn(Optional.empty());
        given(repository.save(any())).willAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------
    // Matricula
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O vencimento sai do periodo do plano, nao do corpo da requisicao")
    void vencimentoVemDoPlano() {
        LocalDate inicio = LocalDate.of(2026, 3, 10);

        AssinaturaDtos.Response mensal = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, inicio, FormaPagamento.PIX, null));

        assertThat(mensal.dataVencimento()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(mensal.status()).isEqualTo(StatusAssinatura.ATIVA);

        // Mesmo pedido, plano anual: quem define o periodo e o plano.
        mensalCentro.setTipoCobranca(TipoCobranca.PACOTE_ANUAL);
        AssinaturaDtos.Response anual = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, inicio, FormaPagamento.PIX, null));

        assertThat(anual.dataVencimento()).isEqualTo(LocalDate.of(2027, 3, 10));
    }

    @Test
    @DisplayName("Sem data de inicio, a matricula comeca hoje")
    void semDataDeInicioComecaHoje() {
        AssinaturaDtos.Response criada = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null, FormaPagamento.PIX, null));

        assertThat(criada.dataInicio()).isEqualTo(LocalDate.now());
        assertThat(criada.dataVencimento()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    @DisplayName("Aluno com matricula vigente nao se matricula de novo")
    void naoMatriculaDuasVezes() {
        Assinatura vigente = assinaturaDe(StatusAssinatura.INADIMPLENTE, LocalDate.now().plusDays(5));
        given(repository.buscarVigentePorAluno(10L)).willReturn(Optional.of(vigente));

        // INADIMPLENTE tambem barra: a matricula continua sendo do aluno,
        // ele so esta em atraso. Se nao barrasse, bastaria atrasar o
        // pagamento para abrir uma segunda e deixar a primeira para tras.
        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null, FormaPagamento.PIX, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja tem matricula vigente");
    }

    @Test
    @DisplayName("So quem esta cadastrado como aluno se matricula")
    void apenasAlunoSeMatricula() {
        aluno.setTipoPerfil(TipoPerfil.PROFESSOR);

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null, FormaPagamento.PIX, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("So alunos se matriculam");
    }

    @Test
    @DisplayName("Plano fora de linha nao aceita novas matriculas")
    void planoForaDeLinhaNaoMatricula() {
        mensalCentro.setAtivo(false);

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null, FormaPagamento.PIX, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fora de linha");
    }

    @Test
    @DisplayName("Parceiro exige token; matricula direta recusa token")
    void coerenciaDoTokenDeParceiro() {
        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.GYMPASS, "  ", null, FormaPagamento.PIX, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("exige o codigo do aluno");

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, "GP-123", null, FormaPagamento.PIX, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao tem codigo de parceiro");

        AssinaturaDtos.Response valida = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.TOTALPASS, " TP-987 ", null, FormaPagamento.PIX, null));
        assertThat(valida.tokenParceiro()).isEqualTo("TP-987");
    }

    @Test
    @DisplayName("Um codigo de parceiro nao serve a duas matriculas vigentes")
    void tokenDeParceiroNaoSeRepete() {
        given(repository.existsByTokenParceiroAndStatusNot("GP-123", StatusAssinatura.CANCELADA))
                .willReturn(true);

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.GYMPASS, "GP-123", null, FormaPagamento.PIX, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta em uso");
    }

    // ---------------------------------------------------------------
    // Programa de indicacao
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Matricula por indicacao grava quem indicou")
    void matriculaPorIndicacaoGravaIndicador() {
        Usuario indicadora = new Usuario();
        indicadora.setId(20L);
        indicadora.setNome("Bruna Lima");
        indicadora.setTipoPerfil(TipoPerfil.ALUNO);
        given(usuarioRepository.findById(20L)).willReturn(Optional.of(indicadora));

        AssinaturaDtos.Response criada = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.INDICACAO, null, null, FormaPagamento.PIX, 20L));

        assertThat(criada.indicadoPorAlunoId()).isEqualTo(20L);
        assertThat(criada.indicadoPorNome()).isEqualTo("Bruna Lima");
    }

    @Test
    @DisplayName("Indicacao sem indicador e erro de validacao do DTO")
    void indicacaoSemIndicadorEhInvalida() {
        AssinaturaDtos.Matricular request = new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.INDICACAO, null, null, FormaPagamento.PIX, null);

        assertThat(request.isIndicadorCoerente()).isFalse();
    }

    @Test
    @DisplayName("Indicador em matricula que nao e indicacao e erro de validacao do DTO")
    void indicadorForaDeIndicacaoEhInvalido() {
        AssinaturaDtos.Matricular request = new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null, FormaPagamento.PIX, 20L);

        assertThat(request.isIndicadorCoerente()).isFalse();
    }

    @Test
    @DisplayName("Aluno nao pode se indicar a si mesmo")
    void alunoNaoSeIndicaASiMesmo() {
        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.INDICACAO, null, null, FormaPagamento.PIX, 10L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao pode se indicar a si mesmo");
    }

    @Test
    @DisplayName("Indicador inexistente e 404")
    void indicadorInexistenteE404() {
        given(usuarioRepository.findById(777L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.INDICACAO, null, null, FormaPagamento.PIX, 777L)))
                .isInstanceOf(br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Quem nao e aluno nao pode ter indicado ninguem")
    void indicadorPrecisaSerAluno() {
        Usuario professor = new Usuario();
        professor.setId(30L);
        professor.setNome("Prof. Ana");
        professor.setTipoPerfil(TipoPerfil.PROFESSOR);
        given(usuarioRepository.findById(30L)).willReturn(Optional.of(professor));

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.INDICACAO, null, null, FormaPagamento.PIX, 30L)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao esta cadastrado como aluno");
    }

    @Test
    @DisplayName("Ranking de indicacoes vem pronto do repositorio")
    void indicacoesDelegaParaRepositorio() {
        given(repository.contarIndicacoesPorAluno()).willReturn(
                List.of(new ContagemAgrupada(20L, "Bruna Lima", 3L)));

        List<ContagemAgrupada> ranking = service.indicacoes();

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).nome()).isEqualTo("Bruna Lima");
        assertThat(ranking.get(0).quantidade()).isEqualTo(3L);
    }

    // ---------------------------------------------------------------
    // Renovacao e ciclo de vida
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Renovar adiantado soma ao vencimento que ainda valia")
    void renovarAdiantadoNaoPerdeDias() {
        LocalDate vencimentoFuturo = LocalDate.now().plusDays(12);
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, vencimentoFuturo);
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        AssinaturaDtos.Response renovada = service.renovar(99L);

        // Quem paga antes nao perde os dias que ainda tinha.
        assertThat(renovada.dataVencimento()).isEqualTo(vencimentoFuturo.plusMonths(1));
    }

    @Test
    @DisplayName("Renovar assinatura vencida conta a partir de hoje, nao do passado")
    void renovarVencidaContaDeHoje() {
        Assinatura assinatura = assinaturaDe(StatusAssinatura.INADIMPLENTE, LocalDate.now().minusMonths(3));
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        AssinaturaDtos.Response renovada = service.renovar(99L);

        // Somar ao vencimento antigo devolveria uma assinatura ja vencida
        // — o aluno pagaria e continuaria barrado na catraca.
        assertThat(renovada.dataVencimento()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(renovada.status()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(renovada.vencida()).isFalse();
    }

    @Test
    @DisplayName("Assinatura cancelada nao se renova")
    void canceladaNaoRenova() {
        Assinatura assinatura = assinaturaDe(StatusAssinatura.CANCELADA, LocalDate.now().plusDays(5));
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.renovar(99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao se renova");
    }

    @Test
    @DisplayName("Cancelar registra a data e nao apaga a assinatura")
    void cancelarRegistraData() {
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(20));
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        AssinaturaDtos.Response cancelada = service.cancelar(99L);

        assertThat(cancelada.status()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(cancelada.dataCancelamento()).isEqualTo(LocalDate.now());

        assertThatThrownBy(() -> service.cancelar(99L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta cancelada");
    }

    // ---------------------------------------------------------------
    // Cobranca simulada
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Matricular gera a primeira cobranca, pendente, no vencimento da assinatura")
    void matricularCriaCobrancaPendente() {
        AssinaturaDtos.Response criada = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, LocalDate.of(2026, 3, 10), FormaPagamento.PIX, null));

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(cobrancaRepository).save(captor.capture());
        Cobranca cobranca = captor.getValue();

        assertThat(cobranca.getValor()).isEqualByComparingTo("129.90");
        assertThat(cobranca.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(cobranca.getDataVencimento()).isEqualTo(criada.dataVencimento());
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PENDENTE);
        // "SIMULADO" no meio do codigo: ninguem pode confundir isto com um
        // boleto/PIX de verdade, ja que nao ha gateway integrado.
        assertThat(cobranca.getCodigoSimulado()).startsWith("PIX-SIMULADO-");
    }

    @Test
    @DisplayName("Cobranca por cartao nao tem codigo de copia e cola")
    void cobrancaPorCartaoNaoTemCodigo() {
        service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null, FormaPagamento.CARTAO, null));

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(cobrancaRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigoSimulado()).isNull();
    }

    @Test
    @DisplayName("Renovar quita a cobranca pendente e gera a do proximo ciclo")
    void renovarQuitaCobrancaEGeraProxima() {
        LocalDate vencimentoAtual = LocalDate.now().plusDays(5);
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, vencimentoAtual);
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        Cobranca pendente = new Cobranca();
        pendente.setAssinatura(assinatura);
        pendente.setStatus(StatusCobranca.PENDENTE);
        given(cobrancaRepository.findByAssinaturaIdAndStatus(99L, StatusCobranca.PENDENTE))
                .willReturn(Optional.of(pendente));

        AssinaturaDtos.Response renovada = service.renovar(99L);

        assertThat(pendente.getStatus()).isEqualTo(StatusCobranca.PAGA);
        assertThat(pendente.getDataPagamento()).isEqualTo(LocalDate.now());

        ArgumentCaptor<Cobranca> captor = ArgumentCaptor.forClass(Cobranca.class);
        verify(cobrancaRepository).save(captor.capture());
        // A proxima cobranca vence no novo vencimento da assinatura, nao no antigo.
        assertThat(captor.getValue().getDataVencimento()).isEqualTo(renovada.dataVencimento());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusCobranca.PENDENTE);

        // Sem este flush, o UPDATE que quita a cobranca antiga so vai ao
        // banco no commit — depois do INSERT da nova, que usa IDENTITY e
        // insere na hora. As duas cairiam juntas no indice parcial de "uma
        // pendente por assinatura" (bug real, pego so em teste manual).
        verify(cobrancaRepository).flush();
    }

    @Test
    @DisplayName("Renovar sem cobranca pendente ainda gera a do proximo ciclo")
    void renovarSemCobrancaPendenteGeraProximaAssimMesmo() {
        // Cobre a assinatura que ja existia antes da cobranca nascer:
        // repository.findByAssinaturaIdAndStatus nao foi estubado, entao
        // devolve Optional.empty() por padrao.
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(5));
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        service.renovar(99L);

        verify(cobrancaRepository).save(any());
    }

    @Test
    @DisplayName("Cancelar cancela a cobranca pendente da assinatura")
    void cancelarCancelaCobrancaPendente() {
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(20));
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        Cobranca pendente = new Cobranca();
        pendente.setStatus(StatusCobranca.PENDENTE);
        given(cobrancaRepository.findByAssinaturaIdAndStatus(99L, StatusCobranca.PENDENTE))
                .willReturn(Optional.of(pendente));

        service.cancelar(99L);

        assertThat(pendente.getStatus()).isEqualTo(StatusCobranca.CANCELADA);
    }

    // ---------------------------------------------------------------
    // Fila de vencimentos
    // ---------------------------------------------------------------

    @Test
    @DisplayName("A fila alcanca as ja vencidas, nao so as que estao por vencer")
    void filaIncluiVencidas() {
        LocalDate hoje = LocalDate.now();
        ArgumentCaptor<LocalDate> limite = ArgumentCaptor.forClass(LocalDate.class);
        given(repository.vencendoAte(limite.capture(), any())).willReturn(List.of());
        given(repository.contarVencendoAte(any())).willReturn(0L);

        service.vencimentos(15, 8);

        // A janela vai ate hoje+15; a consulta usa <=, entao tudo que ficou
        // para tras entra junto. Uma vencida ha uma semana e mais urgente
        // que uma que vence amanha.
        assertThat(limite.getValue()).isEqualTo(hoje.plusDays(15));
    }

    @Test
    @DisplayName("Devolve a contagem completa junto com o pedaco da lista")
    void filaDevolveTotalEPedaco() {
        Assinatura vencida = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(3));
        given(repository.vencendoAte(any(), any())).willReturn(List.of(vencida));
        given(repository.contarVencendoAte(any())).willReturn(22L);

        AssinaturaDtos.FilaDeVencimentos fila = service.vencimentos(15, 8);

        // Sem o total, uma lista truncada em oito parece a fila inteira.
        assertThat(fila.total()).isEqualTo(22L);
        assertThat(fila.itens()).hasSize(1);
        assertThat(fila.dias()).isEqualTo(15);
    }

    @Test
    @DisplayName("Dias para vencer sai negativo no que ja venceu")
    void diasParaVencerNegativoNoAtraso() {
        Assinatura vencida = assinaturaDe(StatusAssinatura.INADIMPLENTE, LocalDate.now().minusDays(3));
        Assinatura aVencer = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(4));
        given(repository.vencendoAte(any(), any())).willReturn(List.of(vencida, aVencer));
        given(repository.contarVencendoAte(any())).willReturn(2L);

        AssinaturaDtos.FilaDeVencimentos fila = service.vencimentos(15, 8);

        assertThat(fila.itens()).extracting(AssinaturaDtos.Vencimento::diasParaVencer)
                .containsExactly(-3L, 4L);
        assertThat(fila.itens().get(0).alunoNome()).isEqualTo("Marina Alves");
        assertThat(fila.itens().get(0).status()).isEqualTo(StatusAssinatura.INADIMPLENTE);
    }

    // ---------------------------------------------------------------
    // Regua de cobranca / relatorio de inadimplencia
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O resumo conta cada etapa da regua separadamente")
    void resumoContaCadaEtapa() {
        given(repository.countAtivasVencendoEntre(any(), any())).willReturn(4L);
        given(repository.countAtivasVencidas(any())).willReturn(2L);
        given(repository.countByStatus(StatusAssinatura.INADIMPLENTE)).willReturn(3L);

        AssinaturaDtos.ResumoInadimplencia resumo = service.resumoInadimplencia(7);

        assertThat(resumo.venceEmBreve()).isEqualTo(4L);
        assertThat(resumo.vencidas()).isEqualTo(2L);
        assertThat(resumo.inadimplentes()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Cada linha do relatorio traz a cobranca pendente daquela assinatura")
    void inadimplenciaAnexaCobrancaPendente() {
        Assinatura vencida = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(3));
        given(repository.buscarEmAtencao(any(), any()))
                .willReturn(new PageImpl<>(List.of(vencida)));

        Cobranca pendente = new Cobranca();
        pendente.setId(55L);
        pendente.setAssinatura(vencida);
        pendente.setFormaPagamento(FormaPagamento.BOLETO);
        pendente.setCodigoSimulado("BOLETO-SIMULADO-ABC123");
        given(cobrancaRepository.findByAssinaturaIdInAndStatus(List.of(99L), StatusCobranca.PENDENTE))
                .willReturn(List.of(pendente));

        Page<AssinaturaDtos.LinhaInadimplencia> pagina =
                service.inadimplencia(PageRequest.of(0, 20), 7);

        AssinaturaDtos.LinhaInadimplencia linha = pagina.getContent().get(0);
        assertThat(linha.assinaturaId()).isEqualTo(99L);
        assertThat(linha.vencida()).isTrue();
        assertThat(linha.diasParaVencer()).isEqualTo(-3L);
        assertThat(linha.cobrancaPendenteId()).isEqualTo(55L);
        assertThat(linha.formaPagamento()).isEqualTo(FormaPagamento.BOLETO);
        assertThat(linha.codigoSimulado()).isEqualTo("BOLETO-SIMULADO-ABC123");
    }

    @Test
    @DisplayName("Sem cobranca pendente, a linha do relatorio nao oferece o que confirmar")
    void inadimplenciaSemCobrancaPendenteDeixaCamposNulos() {
        Assinatura semCobranca = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(2));
        given(repository.buscarEmAtencao(any(), any()))
                .willReturn(new PageImpl<>(List.of(semCobranca)));
        given(cobrancaRepository.findByAssinaturaIdInAndStatus(any(), any())).willReturn(List.of());

        AssinaturaDtos.LinhaInadimplencia linha =
                service.inadimplencia(PageRequest.of(0, 20), 7).getContent().get(0);

        assertThat(linha.cobrancaPendenteId()).isNull();
        assertThat(linha.formaPagamento()).isNull();
        assertThat(linha.codigoSimulado()).isNull();
    }

    @Test
    @DisplayName("O job diario marca inadimplente cada assinatura vencida alem da tolerancia")
    void autoBloquearMarcaTodasAsRetornadas() {
        Assinatura primeira = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(10));
        Assinatura segunda = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(20));
        ArgumentCaptor<LocalDate> limite = ArgumentCaptor.forClass(LocalDate.class);
        given(repository.buscarAtivasVencidasAntesDe(limite.capture())).willReturn(List.of(primeira, segunda));

        int quantidade = service.autoBloquearVencidas(5);

        assertThat(quantidade).isEqualTo(2);
        assertThat(primeira.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(segunda.getStatus()).isEqualTo(StatusAssinatura.INADIMPLENTE);
        assertThat(limite.getValue()).isEqualTo(LocalDate.now().minusDays(5));
    }

    @Test
    @DisplayName("Sem ninguem alem da tolerancia, o job nao marca nada")
    void autoBloquearSemVencidasNaoFazNada() {
        given(repository.buscarAtivasVencidasAntesDe(any())).willReturn(List.of());

        assertThat(service.autoBloquearVencidas(5)).isZero();
    }

    // ---------------------------------------------------------------
    // Lembretes automaticos
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Gerar lembretes registra um por estagio da regua")
    void gerarLembretesRegistraUmPorEstagio() {
        Assinatura venceEmBreve = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(3));
        Assinatura vencida = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(2));
        Assinatura inadimplente = assinaturaDe(StatusAssinatura.INADIMPLENTE, LocalDate.now().minusDays(40));

        given(repository.buscarAtivasVencendoEntre(any(), any())).willReturn(List.of(venceEmBreve));
        given(repository.buscarAtivasVencidasAntesDe(any())).willReturn(List.of(vencida));
        given(repository.findByStatus(StatusAssinatura.INADIMPLENTE)).willReturn(List.of(inadimplente));

        int total = service.gerarLembretes(7);

        assertThat(total).isEqualTo(3);
        verify(lembreteRepository, times(3)).save(any());
    }

    @Test
    @DisplayName("Lembrete ja enviado para o estagio nao se repete")
    void gerarLembretesNaoRepeteEstagioJaEnviado() {
        Assinatura vencida = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(2));
        given(repository.buscarAtivasVencendoEntre(any(), any())).willReturn(List.of());
        given(repository.buscarAtivasVencidasAntesDe(any())).willReturn(List.of(vencida));
        given(repository.findByStatus(StatusAssinatura.INADIMPLENTE)).willReturn(List.of());
        given(lembreteRepository.existsByAssinaturaIdAndEstagio(99L, EstagioLembrete.VENCIDA)).willReturn(true);

        assertThat(service.gerarLembretes(7)).isZero();
        verify(lembreteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Canal e WhatsApp quando o aluno tem telefone, senao e-mail")
    void canalDoLembreteDependeDoTelefone() {
        aluno.setTelefone("11999990000");
        aluno.setEmail("marina@ex.com");
        Assinatura vencida = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(2));
        given(repository.buscarAtivasVencendoEntre(any(), any())).willReturn(List.of());
        given(repository.buscarAtivasVencidasAntesDe(any())).willReturn(List.of(vencida));
        given(repository.findByStatus(StatusAssinatura.INADIMPLENTE)).willReturn(List.of());

        ArgumentCaptor<LembreteEnviado> captor = ArgumentCaptor.forClass(LembreteEnviado.class);
        service.gerarLembretes(7);
        verify(lembreteRepository).save(captor.capture());

        assertThat(captor.getValue().getCanal()).isEqualTo(CanalLembrete.WHATSAPP);
        assertThat(captor.getValue().getDestinatario()).isEqualTo("11999990000");

        // Sem telefone, cai para e-mail.
        aluno.setTelefone(null);
        service.gerarLembretes(7);
        verify(lembreteRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getCanal()).isEqualTo(CanalLembrete.EMAIL);
        assertThat(captor.getValue().getDestinatario()).isEqualTo("marina@ex.com");
    }

    @Test
    @DisplayName("Historico de lembretes vem do mais recente pro mais antigo")
    void historicoLembretesOrdenaDoMaisRecente() {
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(5));
        given(repository.findWithAlunoAndPlanoById(99L)).willReturn(Optional.of(assinatura));

        LembreteEnviado lembrete = new LembreteEnviado();
        lembrete.setId(1L);
        lembrete.setEstagio(EstagioLembrete.VENCE_EM_BREVE);
        lembrete.setCanal(CanalLembrete.EMAIL);
        lembrete.setDestinatario("marina@ex.com");
        given(lembreteRepository.findByAssinaturaIdOrderByDataEnvioDesc(99L)).willReturn(List.of(lembrete));

        List<LembreteDtos.Response> historico = service.historicoLembretes(99L);

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).canal()).isEqualTo(CanalLembrete.EMAIL);
    }

    @Test
    @DisplayName("Historico de lembretes de assinatura inexistente e 404")
    void historicoLembretesAssinaturaInexistenteE404() {
        given(repository.findWithAlunoAndPlanoById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.historicoLembretes(999L))
                .isInstanceOf(br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException.class);
    }

    // ---------------------------------------------------------------
    // Grafico de matriculas por mes
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Mes sem matricula vira zero em vez de sumir da serie")
    void mesVazioViraZero() {
        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        // So o mes atual e o de tres meses atras tiveram matricula.
        given(repository.contarPorMesDesde(any())).willReturn(List.of(
                new ContagemMensal(mesAtual.minusMonths(3).getYear(), mesAtual.minusMonths(3).getMonthValue(), 4L),
                new ContagemMensal(mesAtual.getYear(), mesAtual.getMonthValue(), 7L)));

        AssinaturaDtos.HistoricoMensal historico = service.historicoMensal(6);

        // Seis pontos, nao dois: se os meses vazios sumissem, o eixo do
        // tempo comprimiria e o grafico mostraria uma sequencia de meses
        // bons que nunca existiu.
        assertThat(historico.pontos()).hasSize(6);
        // Serie de seis meses terminando no atual: [m-5 .. m]. O mes de
        // tres meses atras cai no indice 2.
        assertThat(historico.pontos()).extracting(AssinaturaDtos.PontoMensal::quantidade)
                .containsExactly(0L, 0L, 4L, 0L, 0L, 7L);
        assertThat(historico.total()).isEqualTo(11L);
        assertThat(historico.meses()).isEqualTo(6);
    }

    @Test
    @DisplayName("A serie vai do mes mais antigo ao atual, em ordem, como yyyy-MM")
    void serieEmOrdemAteOMesAtual() {
        given(repository.contarPorMesDesde(any())).willReturn(List.of());
        YearMonth mesAtual = YearMonth.from(LocalDate.now());

        AssinaturaDtos.HistoricoMensal historico = service.historicoMensal(12);

        assertThat(historico.pontos()).hasSize(12);
        assertThat(historico.pontos().get(0).mes()).isEqualTo(mesAtual.minusMonths(11).toString());
        assertThat(historico.pontos().get(11).mes()).isEqualTo(mesAtual.toString());
        assertThat(historico.total()).isZero();
    }

    @Test
    @DisplayName("A janela consultada comeca no primeiro dia do mes mais antigo")
    void janelaComecaNoPrimeiroDiaDoMes() {
        ArgumentCaptor<LocalDate> desde = ArgumentCaptor.forClass(LocalDate.class);
        given(repository.contarPorMesDesde(desde.capture())).willReturn(List.of());

        service.historicoMensal(12);

        // Comecar no dia de hoje de 11 meses atras deixaria de fora as
        // matriculas do inicio daquele mes, e a primeira barra sairia baixa.
        assertThat(desde.getValue())
                .isEqualTo(YearMonth.from(LocalDate.now()).minusMonths(11).atDay(1));
    }

    // ---------------------------------------------------------------
    // Painel de retencao
    // ---------------------------------------------------------------

    @Test
    @DisplayName("A taxa de churn de cada mes vem calculada: cancelados sobre ativos no inicio")
    void taxaDeChurnCalculada() {
        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of(
                new ContagemMensal(mesAtual.getYear(), mesAtual.getMonthValue(), 5L)));
        given(repository.contarAtivasEm(mesAtual.atDay(1))).willReturn(100L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorPlano(any(), any())).willReturn(List.of());
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of());

        AssinaturaDtos.Retencao retencao = service.retencao(6);

        AssinaturaDtos.PontoChurn ultimoPonto = retencao.historico().pontos().get(5);
        assertThat(ultimoPonto.ativosNoInicio()).isEqualTo(100L);
        assertThat(ultimoPonto.cancelados()).isEqualTo(5L);
        assertThat(ultimoPonto.taxaChurn()).isEqualTo(0.05);
    }

    @Test
    @DisplayName("Mes sem nenhuma assinatura ativa no inicio nao divide por zero")
    void mesSemBaseNaoDivide() {
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of());
        given(repository.contarAtivasEm(any())).willReturn(0L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorPlano(any(), any())).willReturn(List.of());
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of());

        AssinaturaDtos.Retencao retencao = service.retencao(3);

        assertThat(retencao.historico().pontos()).allSatisfy(ponto -> assertThat(ponto.taxaChurn()).isZero());
    }

    @Test
    @DisplayName("O mes de referencia do detalhamento e o ultimo mes fechado, nao o corrente")
    void mesDeReferenciaEOAnterior() {
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of());
        given(repository.contarAtivasEm(any())).willReturn(0L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorPlano(any(), any())).willReturn(List.of());
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of());

        AssinaturaDtos.Retencao retencao = service.retencao(3);

        assertThat(retencao.mesReferencia())
                .isEqualTo(YearMonth.from(LocalDate.now()).minusMonths(1).toString());
    }

    @Test
    @DisplayName("O detalhamento por plano junta ativos e cancelados por id, e ordena pela maior taxa primeiro")
    void detalhamentoPorPlanoOrdenaPorTaxa() {
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of());
        given(repository.contarAtivasEm(any())).willReturn(0L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of(
                new ContagemAgrupada(1L, "Mensal Centro", 50L),
                new ContagemAgrupada(2L, "Rede Anual", 20L)));
        given(repository.contarCancelamentosPorPlano(any(), any())).willReturn(List.of(
                new ContagemAgrupada(1L, "Mensal Centro", 2L),
                new ContagemAgrupada(2L, "Rede Anual", 4L)));
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of());

        AssinaturaDtos.Retencao retencao = service.retencao(3);

        // Rede Anual: 4/20 = 20% de churn; Mensal Centro: 2/50 = 4%. A
        // maior taxa e quem a gestao precisa ver primeiro.
        assertThat(retencao.porPlano()).extracting(AssinaturaDtos.LinhaChurn::nome)
                .containsExactly("Rede Anual", "Mensal Centro");
        assertThat(retencao.porPlano().get(0).taxaChurn()).isEqualTo(0.2);
        assertThat(retencao.porPlano().get(1).taxaChurn()).isEqualTo(0.04);
    }

    @Test
    @DisplayName("Um plano sem cancelamento no mes fechado entra com zero, nao fica de fora")
    void planoSemCancelamentoEntraComZero() {
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of());
        given(repository.contarAtivasEm(any())).willReturn(0L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of(
                new ContagemAgrupada(1L, "Mensal Centro", 50L)));
        given(repository.contarCancelamentosPorPlano(any(), any())).willReturn(List.of());
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of());

        AssinaturaDtos.Retencao retencao = service.retencao(3);

        assertThat(retencao.porPlano()).hasSize(1);
        assertThat(retencao.porPlano().get(0).cancelados()).isZero();
        assertThat(retencao.porPlano().get(0).taxaChurn()).isZero();
    }

    @Test
    @DisplayName("O detalhamento por unidade usa a mesma base espalhada pelo plano.unidades")
    void detalhamentoPorUnidade() {
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of());
        given(repository.contarAtivasEm(any())).willReturn(0L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorPlano(any(), any())).willReturn(List.of());
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of(
                new ContagemAgrupada(1L, "Unidade Centro", 80L)));
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of(
                new ContagemAgrupada(1L, "Unidade Centro", 8L)));

        AssinaturaDtos.Retencao retencao = service.retencao(3);

        assertThat(retencao.porUnidade()).hasSize(1);
        assertThat(retencao.porUnidade().get(0).nome()).isEqualTo("Unidade Centro");
        assertThat(retencao.porUnidade().get(0).taxaChurn()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("O intervalo de cancelamentos do detalhamento e o mes fechado inteiro, semiaberto")
    void intervaloDoMesFechado() {
        given(repository.contarCancelamentosPorMesDesde(any())).willReturn(List.of());
        given(repository.contarAtivasEm(any())).willReturn(0L);
        given(repository.contarAtivasPorPlanoEm(any())).willReturn(List.of());
        given(repository.contarAtivasPorUnidadeEm(any())).willReturn(List.of());
        given(repository.contarCancelamentosPorUnidade(any(), any())).willReturn(List.of());

        ArgumentCaptor<LocalDate> inicio = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> fim = ArgumentCaptor.forClass(LocalDate.class);
        given(repository.contarCancelamentosPorPlano(inicio.capture(), fim.capture())).willReturn(List.of());

        service.retencao(3);

        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        assertThat(inicio.getValue()).isEqualTo(mesAtual.minusMonths(1).atDay(1));
        assertThat(fim.getValue()).isEqualTo(mesAtual.atDay(1));
    }

    // ---------------------------------------------------------------
    // Catraca
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Acesso liberado quando o plano cobre a unidade e a matricula esta em dia")
    void acessoLiberado() {
        LocalDate vencimento = LocalDate.of(2027, 9, 15);
        Assinatura assinatura = assinaturaDe(StatusAssinatura.ATIVA, vencimento);
        given(repository.buscarVigentePorAluno(10L)).willReturn(Optional.of(assinatura));

        AssinaturaDtos.Acesso acesso = service.conferirAcesso(10L, 1L);

        assertThat(acesso.liberado()).isTrue();
        assertThat(acesso.motivo()).isEqualTo(MotivoAcesso.LIBERADO);
        // A mensagem e lida no balcao: data no formato do pais, nao ISO.
        assertThat(acesso.mensagem()).contains("15/09/2027");

        // A mesma pergunta que a catraca faz vira historico de frequencia.
        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinRepository).save(captor.capture());
        assertThat(captor.getValue().isLiberado()).isTrue();
        assertThat(captor.getValue().getMotivo()).isEqualTo(MotivoAcesso.LIBERADO);
        assertThat(captor.getValue().getAssinatura()).isEqualTo(assinatura);
        assertThat(captor.getValue().getAluno()).isEqualTo(aluno);
    }

    @Test
    @DisplayName("No dia do vencimento o aluno ainda treina")
    void vencimentoNoDiaAindaLibera() {
        given(repository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now())));

        // Ele pagou por este dia. Barrar aqui seria cobrar um mes e
        // entregar um mes menos um dia.
        assertThat(service.conferirAcesso(10L, 1L).liberado()).isTrue();
    }

    @Test
    @DisplayName("Inadimplente e vencida barram, cada uma com seu motivo")
    void motivosDeBarrar() {
        given(repository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(assinaturaDe(StatusAssinatura.INADIMPLENTE, LocalDate.now().plusDays(10))));
        AssinaturaDtos.Acesso emAtraso = service.conferirAcesso(10L, 1L);
        assertThat(emAtraso.liberado()).isFalse();
        assertThat(emAtraso.motivo()).isEqualTo(MotivoAcesso.INADIMPLENTE);

        given(repository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().minusDays(1))));
        AssinaturaDtos.Acesso vencida = service.conferirAcesso(10L, 1L);
        assertThat(vencida.liberado()).isFalse();
        assertThat(vencida.motivo()).isEqualTo(MotivoAcesso.VENCIDA);
    }

    @Test
    @DisplayName("Plano que nao cobre a unidade barra o acesso naquela unidade")
    void planoNaoCobreUnidade() {
        given(repository.buscarVigentePorAluno(10L))
                .willReturn(Optional.of(assinaturaDe(StatusAssinatura.ATIVA, LocalDate.now().plusDays(10))));

        AssinaturaDtos.Acesso acesso = service.conferirAcesso(10L, 2L);

        assertThat(acesso.liberado()).isFalse();
        assertThat(acesso.motivo()).isEqualTo(MotivoAcesso.UNIDADE_NAO_COBERTA);
        assertThat(acesso.mensagem()).contains("Unidade Zona Sul");
        // O veredito ainda devolve a matricula: a recepcao precisa saber
        // em que plano o aluno esta para propor a troca.
        assertThat(acesso.assinatura()).isNotNull();
    }

    @Test
    @DisplayName("Aluno sem matricula vigente e barrado sem assinatura no veredito")
    void semMatricula() {
        given(repository.buscarVigentePorAluno(10L)).willReturn(Optional.empty());

        AssinaturaDtos.Acesso acesso = service.conferirAcesso(10L, 1L);

        assertThat(acesso.liberado()).isFalse();
        assertThat(acesso.motivo()).isEqualTo(MotivoAcesso.SEM_MATRICULA);
        assertThat(acesso.assinatura()).isNull();

        // Sem matricula, o check-in ainda e gravado — so sem assinatura pra apontar.
        ArgumentCaptor<Checkin> captor = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinRepository).save(captor.capture());
        assertThat(captor.getValue().isLiberado()).isFalse();
        assertThat(captor.getValue().getAssinatura()).isNull();
    }

    // ---------------------------------------------------------------
    // Historico de frequencia
    // ---------------------------------------------------------------

    @Test
    @DisplayName("O historico de frequencia devolve os check-ins do aluno")
    void historicoDeFrequenciaDoAluno() {
        Checkin checkin = new Checkin();
        checkin.setAluno(aluno);
        checkin.setUnidade(centro);
        checkin.setLiberado(true);
        checkin.setMotivo(MotivoAcesso.LIBERADO);
        given(checkinRepository.findByAlunoId(eq(10L), any()))
                .willReturn(new PageImpl<>(List.of(checkin)));

        Page<br.com.heracles.heracles_api.matriculas.dto.CheckinDtos.Response> pagina =
                service.historicoCheckins(10L, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).unidadeNome()).isEqualTo("Unidade Centro");
        assertThat(pagina.getContent().get(0).liberado()).isTrue();
    }

    @Test
    @DisplayName("Historico de aluno inexistente e 404, nao lista vazia")
    void historicoDeFrequenciaAlunoInexistente() {
        given(usuarioRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.historicoCheckins(999L, PageRequest.of(0, 20)))
                .isInstanceOf(br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException.class);
    }

    private Assinatura assinaturaDe(StatusAssinatura status, LocalDate vencimento) {
        Assinatura assinatura = new Assinatura();
        assinatura.setId(99L);
        assinatura.setAluno(aluno);
        assinatura.setPlano(mensalCentro);
        assinatura.setOrigem(OrigemAssinatura.DIRETO);
        assinatura.setFormaPagamento(FormaPagamento.PIX);
        assinatura.setDataInicio(vencimento.minusMonths(1));
        assinatura.setDataVencimento(vencimento);
        assinatura.setStatus(status);
        if (status == StatusAssinatura.CANCELADA) {
            assinatura.setDataCancelamento(LocalDate.now());
        }
        return assinatura;
    }
}
