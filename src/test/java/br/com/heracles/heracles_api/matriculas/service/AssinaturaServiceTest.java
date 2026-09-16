package br.com.heracles.heracles_api.matriculas.service;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.*;
import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos;
import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos.MotivoAcesso;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssinaturaServiceTest {

    @Mock private AssinaturaRepository repository;
    @Mock private PlanoRepository planoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;

    private AssinaturaService service;

    private Usuario aluno;
    private Unidade centro;
    private Unidade zonaSul;
    private Plano mensalCentro;

    @BeforeEach
    void preparar() {
        service = new AssinaturaService(repository, planoRepository, usuarioRepository, unidadeRepository);

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
                10L, 3L, OrigemAssinatura.DIRETO, null, inicio));

        assertThat(mensal.dataVencimento()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(mensal.status()).isEqualTo(StatusAssinatura.ATIVA);

        // Mesmo pedido, plano anual: quem define o periodo e o plano.
        mensalCentro.setTipoCobranca(TipoCobranca.PACOTE_ANUAL);
        AssinaturaDtos.Response anual = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, inicio));

        assertThat(anual.dataVencimento()).isEqualTo(LocalDate.of(2027, 3, 10));
    }

    @Test
    @DisplayName("Sem data de inicio, a matricula comeca hoje")
    void semDataDeInicioComecaHoje() {
        AssinaturaDtos.Response criada = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null));

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
                10L, 3L, OrigemAssinatura.DIRETO, null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja tem matricula vigente");
    }

    @Test
    @DisplayName("So quem esta cadastrado como aluno se matricula")
    void apenasAlunoSeMatricula() {
        aluno.setTipoPerfil(TipoPerfil.PROFESSOR);

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("So alunos se matriculam");
    }

    @Test
    @DisplayName("Plano fora de linha nao aceita novas matriculas")
    void planoForaDeLinhaNaoMatricula() {
        mensalCentro.setAtivo(false);

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, null, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fora de linha");
    }

    @Test
    @DisplayName("Parceiro exige token; matricula direta recusa token")
    void coerenciaDoTokenDeParceiro() {
        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.GYMPASS, "  ", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("exige o codigo do aluno");

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.DIRETO, "GP-123", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("nao tem codigo de parceiro");

        AssinaturaDtos.Response valida = service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.TOTALPASS, " TP-987 ", null));
        assertThat(valida.tokenParceiro()).isEqualTo("TP-987");
    }

    @Test
    @DisplayName("Um codigo de parceiro nao serve a duas matriculas vigentes")
    void tokenDeParceiroNaoSeRepete() {
        given(repository.existsByTokenParceiroAndStatusNot("GP-123", StatusAssinatura.CANCELADA))
                .willReturn(true);

        assertThatThrownBy(() -> service.matricular(new AssinaturaDtos.Matricular(
                10L, 3L, OrigemAssinatura.GYMPASS, "GP-123", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ja esta em uso");
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
    }

    private Assinatura assinaturaDe(StatusAssinatura status, LocalDate vencimento) {
        Assinatura assinatura = new Assinatura();
        assinatura.setId(99L);
        assinatura.setAluno(aluno);
        assinatura.setPlano(mensalCentro);
        assinatura.setOrigem(OrigemAssinatura.DIRETO);
        assinatura.setDataInicio(vencimento.minusMonths(1));
        assinatura.setDataVencimento(vencimento);
        assinatura.setStatus(status);
        if (status == StatusAssinatura.CANCELADA) {
            assinatura.setDataCancelamento(LocalDate.now());
        }
        return assinatura;
    }
}
