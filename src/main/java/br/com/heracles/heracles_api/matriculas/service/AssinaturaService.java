package br.com.heracles.heracles_api.matriculas.service;

import br.com.heracles.heracles_api.core.domain.TipoPerfil;
import br.com.heracles.heracles_api.core.domain.Unidade;
import br.com.heracles.heracles_api.core.domain.Usuario;
import br.com.heracles.heracles_api.core.repository.UnidadeRepository;
import br.com.heracles.heracles_api.core.repository.UsuarioRepository;
import br.com.heracles.heracles_api.exception.RecursoNaoEncontradoException;
import br.com.heracles.heracles_api.exception.RegraNegocioException;
import br.com.heracles.heracles_api.matriculas.domain.Assinatura;
import br.com.heracles.heracles_api.matriculas.domain.CanalLembrete;
import br.com.heracles.heracles_api.matriculas.domain.Checkin;
import br.com.heracles.heracles_api.matriculas.domain.Cobranca;
import br.com.heracles.heracles_api.matriculas.domain.EstagioLembrete;
import br.com.heracles.heracles_api.matriculas.domain.FormaPagamento;
import br.com.heracles.heracles_api.matriculas.domain.LembreteEnviado;
import br.com.heracles.heracles_api.matriculas.domain.MotivoAcesso;
import br.com.heracles.heracles_api.matriculas.domain.OrigemAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.Plano;
import br.com.heracles.heracles_api.matriculas.domain.StatusAssinatura;
import br.com.heracles.heracles_api.matriculas.domain.StatusCobranca;
import br.com.heracles.heracles_api.matriculas.dto.AssinaturaDtos;
import br.com.heracles.heracles_api.matriculas.dto.CheckinDtos;
import br.com.heracles.heracles_api.matriculas.dto.CobrancaDtos;
import br.com.heracles.heracles_api.matriculas.dto.ContagemAgrupada;
import br.com.heracles.heracles_api.matriculas.dto.ContagemMensal;
import br.com.heracles.heracles_api.matriculas.dto.LembreteDtos;
import br.com.heracles.heracles_api.matriculas.dto.LinhaMotivoCancelamento;
import br.com.heracles.heracles_api.matriculas.dto.LinhaOcupacao;
import br.com.heracles.heracles_api.matriculas.repository.AssinaturaRepository;
import br.com.heracles.heracles_api.matriculas.repository.CheckinRepository;
import br.com.heracles.heracles_api.matriculas.repository.CobrancaRepository;
import br.com.heracles.heracles_api.matriculas.repository.LembreteEnviadoRepository;
import br.com.heracles.heracles_api.matriculas.repository.PlanoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AssinaturaService {

    /**
     * As mensagens de veredito sao lidas no balcao, entao a data sai no
     * formato do pais. LocalDate.toString() daria ISO (2027-09-15) no meio
     * de uma frase em portugues.
     */
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Janela padrao de "vence em breve" do relatorio de inadimplencia —
     * mesma contagem que a tela de Matriculas ja usa no cliente
     * (`DIAS_PARA_VENCER`), para as duas telas concordarem sobre quem
     * esta "quase vencendo".
     */
    public static final int DIAS_VENCE_EM_BREVE_PADRAO = 7;

    /**
     * Janela padrao do alerta de inatividade — tempo o bastante pra nao
     * confundir "vai pouco essa semana" com "parou de vir", curto o
     * bastante pra secretaria agir antes do cancelamento.
     */
    public static final int DIAS_INATIVIDADE_PADRAO = 14;

    private final AssinaturaRepository repository;
    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final CobrancaRepository cobrancaRepository;
    private final CheckinRepository checkinRepository;
    private final LembreteEnviadoRepository lembreteRepository;

    public AssinaturaService(AssinaturaRepository repository,
                             PlanoRepository planoRepository,
                             UsuarioRepository usuarioRepository,
                             UnidadeRepository unidadeRepository,
                             CobrancaRepository cobrancaRepository,
                             CheckinRepository checkinRepository,
                             LembreteEnviadoRepository lembreteRepository) {
        this.repository = repository;
        this.planoRepository = planoRepository;
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.cobrancaRepository = cobrancaRepository;
        this.checkinRepository = checkinRepository;
        this.lembreteRepository = lembreteRepository;
    }

    @Transactional(readOnly = true)
    public Page<AssinaturaDtos.Response> listar(Pageable pageable) {
        LocalDate hoje = LocalDate.now();
        return repository.buscarPaginadoCompleto(pageable)
                .map(assinatura -> AssinaturaDtos.Response.de(assinatura, hoje));
    }

    @Transactional(readOnly = true)
    public AssinaturaDtos.Response buscarPorId(Long id) {
        return AssinaturaDtos.Response.de(carregar(id));
    }

    /** Historico completo do aluno, canceladas inclusive. */
    @Transactional(readOnly = true)
    public List<AssinaturaDtos.Response> historicoDoAluno(Long alunoId) {
        carregarAluno(alunoId); // 404 antes de devolver lista vazia de id inexistente
        LocalDate hoje = LocalDate.now();
        return repository.historicoDoAluno(alunoId).stream()
                .map(assinatura -> AssinaturaDtos.Response.de(assinatura, hoje))
                .toList();
    }

    /**
     * Matriculas por mes, dos ultimos `meses` meses ate o atual.
     *
     * A consulta agregada devolve so os meses que tiveram matricula. Os
     * demais sao preenchidos com zero aqui: um mes sem matricula
     * precisa aparecer como uma barra vazia, e nao sumir — se sumisse, o
     * eixo do tempo comprimiria e o grafico mostraria uma sequencia de
     * meses bons que nunca existiu.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.HistoricoMensal historicoMensal(int meses) {
        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        YearMonth primeiro = mesAtual.minusMonths(meses - 1L);

        Map<YearMonth, Long> porMes = repository.contarPorMesDesde(primeiro.atDay(1)).stream()
                .collect(Collectors.toMap(
                        c -> YearMonth.of(c.ano(), c.mes()),
                        ContagemMensal::quantidade));

        List<AssinaturaDtos.PontoMensal> pontos = new ArrayList<>(meses);
        long total = 0;
        for (int i = 0; i < meses; i++) {
            YearMonth mes = primeiro.plusMonths(i);
            long quantidade = porMes.getOrDefault(mes, 0L);
            total += quantidade;
            pontos.add(new AssinaturaDtos.PontoMensal(mes.toString(), quantidade));
        }

        return new AssinaturaDtos.HistoricoMensal(meses, total, pontos);
    }

    /**
     * O painel de retencao: taxa de churn mes a mes, e o detalhamento por
     * plano e por unidade do ultimo mes fechado.
     *
     * O mes corrente fica de fora do detalhamento por estar incompleto —
     * so a tendencia mensal o inclui, como o grafico de matriculas ja
     * faz, porque ali a barra "em andamento" e uma convencao conhecida.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.Retencao retencao(int meses) {
        YearMonth mesAtual = YearMonth.from(LocalDate.now());
        YearMonth primeiro = mesAtual.minusMonths(meses - 1L);

        Map<YearMonth, Long> canceladosPorMes = repository.contarCancelamentosPorMesDesde(primeiro.atDay(1))
                .stream()
                .collect(Collectors.toMap(c -> YearMonth.of(c.ano(), c.mes()), ContagemMensal::quantidade));

        List<AssinaturaDtos.PontoChurn> pontos = new ArrayList<>(meses);
        for (int i = 0; i < meses; i++) {
            YearMonth mes = primeiro.plusMonths(i);
            long ativosNoInicio = repository.contarAtivasEm(mes.atDay(1));
            long cancelados = canceladosPorMes.getOrDefault(mes, 0L);
            pontos.add(new AssinaturaDtos.PontoChurn(mes.toString(), ativosNoInicio, cancelados, taxa(cancelados, ativosNoInicio)));
        }

        YearMonth mesFechado = mesAtual.minusMonths(1);
        LocalDate inicioMesFechado = mesFechado.atDay(1);
        LocalDate inicioMesAtual = mesAtual.atDay(1);

        return new AssinaturaDtos.Retencao(
                new AssinaturaDtos.HistoricoChurn(meses, pontos),
                mesFechado.toString(),
                linhasDeChurn(
                        repository.contarAtivasPorPlanoEm(inicioMesFechado),
                        repository.contarCancelamentosPorPlano(inicioMesFechado, inicioMesAtual)),
                linhasDeChurn(
                        repository.contarAtivasPorUnidadeEm(inicioMesFechado),
                        repository.contarCancelamentosPorUnidade(inicioMesFechado, inicioMesAtual)));
    }

    /**
     * O painel financeiro: o dinheiro, onde o painel de retencao mede
     * alunos.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.PainelFinanceiro financeiro() {
        YearMonth mesAtual = YearMonth.from(LocalDate.now());

        BigDecimal mrr = repository.somarMrr();
        long assinaturasAtivas = repository.countByStatus(StatusAssinatura.ATIVA);
        BigDecimal ticketMedio = assinaturasAtivas > 0
                ? mrr.divide(BigDecimal.valueOf(assinaturasAtivas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal inadimplenciaEmReais = cobrancaRepository.somarInadimplenciaEmAberto(LocalDate.now());
        BigDecimal projecaoDoMes = cobrancaRepository
                .somarCobrancasNoPeriodo(mesAtual.atDay(1), mesAtual.atEndOfMonth());

        return new AssinaturaDtos.PainelFinanceiro(
                mesAtual.toString(), mrr, assinaturasAtivas, ticketMedio, inadimplenciaEmReais, projecaoDoMes);
    }

    /**
     * Ocupacao por hora do dia, por unidade: em que horario a casa costuma
     * lotar, pra dimensionar equipamento e horario de aula em grupo.
     *
     * Toda unidade cadastrada entra, mesmo sem nenhum check-in no
     * periodo — a serie zerada e o dado, nao um erro a esconder.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.PainelOcupacao ocupacao(int dias) {
        LocalDateTime desde = LocalDate.now().minusDays(dias).atStartOfDay();

        Map<Long, List<LinhaOcupacao>> linhasPorUnidade = checkinRepository
                .contarOcupacaoPorUnidadeEHora(desde).stream()
                .collect(Collectors.groupingBy(LinhaOcupacao::unidadeId));

        List<AssinaturaDtos.OcupacaoPorUnidade> unidades = unidadeRepository.findAll().stream()
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .map(unidade -> {
                    Map<Integer, Long> porHora = linhasPorUnidade
                            .getOrDefault(unidade.getId(), List.of()).stream()
                            .collect(Collectors.toMap(LinhaOcupacao::hora, LinhaOcupacao::quantidade));

                    List<AssinaturaDtos.PontoOcupacao> pontos = new ArrayList<>(24);
                    for (int hora = 0; hora < 24; hora++) {
                        pontos.add(new AssinaturaDtos.PontoOcupacao(hora, porHora.getOrDefault(hora, 0L)));
                    }

                    return new AssinaturaDtos.OcupacaoPorUnidade(unidade.getId(), unidade.getNome(), pontos);
                })
                .toList();

        return new AssinaturaDtos.PainelOcupacao(dias, unidades);
    }

    /**
     * Junta as duas contagens (ativos e cancelados) por id de grupo numa
     * linha so, ordenada da maior taxa de churn para a menor — e a maior
     * taxa que a gestao precisa ver primeiro.
     */
    private List<AssinaturaDtos.LinhaChurn> linhasDeChurn(List<ContagemAgrupada> ativos,
                                                           List<ContagemAgrupada> cancelados) {
        Map<Long, Long> canceladosPorId = cancelados.stream()
                .collect(Collectors.toMap(ContagemAgrupada::id, ContagemAgrupada::quantidade));

        return ativos.stream()
                .map(grupo -> {
                    long qtdCancelados = canceladosPorId.getOrDefault(grupo.id(), 0L);
                    return new AssinaturaDtos.LinhaChurn(
                            grupo.id(), grupo.nome(), grupo.quantidade(), qtdCancelados,
                            taxa(qtdCancelados, grupo.quantidade()));
                })
                .sorted((a, b) -> Double.compare(b.taxaChurn(), a.taxaChurn()))
                .toList();
    }

    /** 0 quando a base e zero — sem isso, todo chamador teria que tratar a divisao por zero. */
    private double taxa(long parte, long base) {
        return base == 0 ? 0d : (double) parte / base;
    }

    /**
     * O ranking do programa de indicacao: quantas matriculas cada aluno
     * trouxe, do maior para o menor. So aparece quem ja indicou alguem —
     * um ranking com todo mundo em zero nao ajuda a reconhecer ninguem.
     */
    @Transactional(readOnly = true)
    public List<ContagemAgrupada> indicacoes() {
        return repository.contarIndicacoesPorAluno();
    }

    /**
     * A fila de vencimentos dos proximos `dias`.
     *
     * Traz junto as que ja venceram: uma matricula vencida ha uma semana
     * e mais urgente que uma que vence amanha, e some-la a fila e o que
     * impede o caso mais grave de desaparecer da tela justamente por ser
     * grave demais.
     *
     * Devolve a contagem completa e so um pedaco da lista — o painel usa
     * o numero para dizer quantas ficaram de fora.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.FilaDeVencimentos vencimentos(int dias, int limite) {
        LocalDate hoje = LocalDate.now();
        LocalDate limiteDaJanela = hoje.plusDays(dias);

        List<AssinaturaDtos.Vencimento> itens =
                repository.vencendoAte(limiteDaJanela, PageRequest.of(0, limite)).stream()
                        .map(assinatura -> AssinaturaDtos.Vencimento.de(assinatura, hoje))
                        .toList();

        return new AssinaturaDtos.FilaDeVencimentos(
                dias, repository.contarVencendoAte(limiteDaJanela), itens);
    }

    /**
     * Matricula o aluno.
     *
     * O vencimento e calculado aqui, a partir do periodo do plano — nunca
     * vem do cliente. Fosse um campo do corpo, bastaria editar a
     * requisicao para se dar um ano de academia por um mes pago.
     */
    @Transactional
    public AssinaturaDtos.Response matricular(AssinaturaDtos.Matricular request) {
        Usuario aluno = carregarAluno(request.alunoId());

        if (aluno.getTipoPerfil() != TipoPerfil.ALUNO) {
            throw new RegraNegocioException(
                    "So alunos se matriculam. \"%s\" esta cadastrado como %s."
                            .formatted(aluno.getNome(), aluno.getTipoPerfil()));
        }

        repository.buscarVigentePorAluno(aluno.getId()).ifPresent(vigente -> {
            throw new RegraNegocioException(
                    "%s ja tem matricula vigente no plano \"%s\". Cancele a atual antes de matricular de novo."
                            .formatted(aluno.getNome(), vigente.getPlano().getNome()));
        });

        Plano plano = planoRepository.findWithUnidadesById(request.planoId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", request.planoId()));

        if (!plano.isAtivo()) {
            throw new RegraNegocioException(
                    "O plano \"%s\" esta fora de linha e nao aceita novas matriculas.".formatted(plano.getNome()));
        }

        String token = normalizarToken(request.origem(), request.tokenParceiro());
        Usuario indicadoPor = normalizarIndicador(request.origem(), request.indicadoPorAlunoId(), aluno);

        Assinatura assinatura = new Assinatura();
        assinatura.setAluno(aluno);
        assinatura.setPlano(plano);
        assinatura.setOrigem(request.origem());
        assinatura.setTokenParceiro(token);
        assinatura.setIndicadoPor(indicadoPor);
        assinatura.setFormaPagamento(request.formaPagamento());
        assinatura.setStatus(StatusAssinatura.ATIVA);

        LocalDate inicio = request.dataInicio() != null ? request.dataInicio() : LocalDate.now();
        assinatura.setDataInicio(inicio);
        assinatura.setDataVencimento(inicio.plusMonths(plano.getTipoCobranca().getMesesDeVigencia()));

        Assinatura salva = repository.save(assinatura);
        criarCobranca(salva);
        return AssinaturaDtos.Response.de(salva);
    }

    /**
     * Pagamento entrou: empurra o vencimento, devolve a assinatura a ATIVA,
     * quita a cobranca em aberto (se houver) e gera a do proximo ciclo.
     *
     * "Se houver" cobre a assinatura que ja existia antes desta cobranca
     * nascer — sem cobranca pendente, so ha o que renovar e gerar a
     * proxima. Depois desta migracao toda assinatura nova ja nasce com
     * uma, entao o caminho sem cobranca tende a desaparecer com o tempo.
     */
    @Transactional
    public AssinaturaDtos.Response renovar(Long id) {
        Assinatura assinatura = carregar(id);
        renovarAssinatura(assinatura, LocalDate.now());
        return AssinaturaDtos.Response.de(assinatura);
    }

    /**
     * Paga a cobranca em aberto (se houver) e gera a do proximo ciclo —
     * a mesma sequencia tanto para quem a secretaria renova na tela
     * quanto para quem o cartao renova sozinho.
     */
    private void renovarAssinatura(Assinatura assinatura, LocalDate hoje) {
        cobrancaRepository.findByAssinaturaIdAndStatus(assinatura.getId(), StatusCobranca.PENDENTE)
                .ifPresent(cobranca -> cobranca.confirmarPagamento(hoje));
        // A cobranca usa GenerationType.IDENTITY: o INSERT da proxima (logo
        // abaixo) executa na hora, mas o UPDATE desta so seria mandado ao
        // banco no commit. Sem o flush aqui, as duas cairiam juntas no
        // indice parcial de "uma pendente por assinatura" e o banco veria
        // duas pendentes ao mesmo tempo.
        cobrancaRepository.flush();

        assinatura.renovar(hoje);
        criarCobranca(assinatura);
    }

    /**
     * O job diario de renovacao automatica: quem paga no cartao e venceu
     * (ou ja esta inadimplente, se o job ficou algum dia sem rodar) e
     * cobrado e renovado sozinho, sem a secretaria precisar confirmar.
     *
     * So cartao — boleto e PIX nao tem "cobranca automatica" de verdade,
     * exigem uma acao real de pagamento de quem paga.
     */
    @Transactional
    public int renovarAutomaticamente() {
        LocalDate hoje = LocalDate.now();
        List<Assinatura> candidatas = repository.buscarParaRenovacaoAutomatica(hoje);
        candidatas.forEach(assinatura -> renovarAssinatura(assinatura, hoje));
        return candidatas.size();
    }

    /** Pagamento nao entrou: interrompe o acesso sem apagar a matricula. A cobranca continua pendente — a divida nao some. */
    @Transactional
    public AssinaturaDtos.Response marcarInadimplente(Long id) {
        Assinatura assinatura = carregar(id);
        assinatura.marcarInadimplente();
        return AssinaturaDtos.Response.de(assinatura);
    }

    @Transactional
    public AssinaturaDtos.Response cancelar(Long id, AssinaturaDtos.Cancelar request) {
        Assinatura assinatura = carregar(id);
        assinatura.cancelar(LocalDate.now(), request.motivo(), vazioComoNulo(request.comentario()));
        cobrancaRepository.findByAssinaturaIdAndStatus(assinatura.getId(), StatusCobranca.PENDENTE)
                .ifPresent(Cobranca::cancelar);
        return AssinaturaDtos.Response.de(assinatura);
    }

    /** Quantos cancelamentos por motivo, do mais comum para o menos comum. */
    @Transactional(readOnly = true)
    public List<LinhaMotivoCancelamento> motivosCancelamento() {
        return repository.contarCancelamentosPorMotivo();
    }

    /** Extrato de cobrancas da assinatura, mais recente primeiro. */
    @Transactional(readOnly = true)
    public List<CobrancaDtos.Response> historicoCobrancas(Long assinaturaId) {
        carregar(assinaturaId); // 404 antes de devolver historico vazio de id inexistente
        return cobrancaRepository.findByAssinaturaIdOrderByDataVencimentoDesc(assinaturaId).stream()
                .map(CobrancaDtos.Response::de)
                .toList();
    }

    /** Contagem por etapa da regua, para o cabecalho do relatorio de inadimplencia. */
    @Transactional(readOnly = true)
    public AssinaturaDtos.ResumoInadimplencia resumoInadimplencia(int diasParaVencer) {
        LocalDate hoje = LocalDate.now();
        LocalDate limiteDaJanela = hoje.plusDays(diasParaVencer);
        return new AssinaturaDtos.ResumoInadimplencia(
                repository.countAtivasVencendoEntre(hoje, limiteDaJanela),
                repository.countAtivasVencidas(hoje),
                repository.countByStatus(StatusAssinatura.INADIMPLENTE));
    }

    /**
     * O relatorio de inadimplencia em si: quem vence em breve, ja venceu ou
     * foi marcado inadimplente, com a cobranca em aberto de cada um para a
     * tela oferecer "confirmar pagamento" na hora.
     */
    @Transactional(readOnly = true)
    public Page<AssinaturaDtos.LinhaInadimplencia> inadimplencia(Pageable pageable, int diasParaVencer) {
        LocalDate hoje = LocalDate.now();
        LocalDate limiteDaJanela = hoje.plusDays(diasParaVencer);

        Page<Assinatura> pagina = repository.buscarEmAtencao(limiteDaJanela, pageable);

        List<Long> assinaturaIds = pagina.getContent().stream().map(Assinatura::getId).toList();
        Map<Long, Cobranca> cobrancasPendentes = cobrancaRepository
                .findByAssinaturaIdInAndStatus(assinaturaIds, StatusCobranca.PENDENTE).stream()
                .collect(Collectors.toMap(cobranca -> cobranca.getAssinatura().getId(), cobranca -> cobranca));

        Map<Long, LembreteEnviado> ultimoLembretePorAssinatura = lembreteRepository
                .findByAssinaturaIdIn(assinaturaIds).stream()
                .collect(Collectors.toMap(
                        lembrete -> lembrete.getAssinatura().getId(),
                        lembrete -> lembrete,
                        (mantido, novo) -> novo.getDataEnvio().isAfter(mantido.getDataEnvio()) ? novo : mantido));

        return pagina.map(assinatura -> AssinaturaDtos.LinhaInadimplencia.de(
                assinatura, cobrancasPendentes.get(assinatura.getId()),
                ultimoLembretePorAssinatura.get(assinatura.getId()), hoje));
    }

    /**
     * Cabecalho do alerta de inatividade: quantas matriculas ativas tem
     * aluno parado, e quantas dessas nunca fizeram check-in nenhum.
     */
    @Transactional(readOnly = true)
    public AssinaturaDtos.ResumoAlunosInativos resumoAlunosInativos(int diasSemCheckin) {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasSemCheckin);
        return new AssinaturaDtos.ResumoAlunosInativos(
                repository.countInativasDesde(limite),
                repository.countAtivasSemCheckinNunca());
    }

    /**
     * O alerta de inatividade em si: matriculas ativas cujo aluno nunca
     * fez check-in liberado, ou parou ha mais dias do que a janela
     * permite — indicador antecedente, ao contrario do painel de
     * retencao, que so ve o churn depois que a assinatura ja foi
     * cancelada.
     */
    @Transactional(readOnly = true)
    public Page<AssinaturaDtos.LinhaAlunoInativo> alunosInativos(Pageable pageable, int diasSemCheckin) {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime limite = agora.minusDays(diasSemCheckin);
        return repository.buscarInativasDesde(limite, pageable)
                .map(bruta -> AssinaturaDtos.LinhaAlunoInativo.de(bruta, agora));
    }

    /**
     * O job diario da regua de cobranca: quem esta ativo mas vencido ha
     * mais dias do que a tolerancia permite vira INADIMPLENTE sozinho, sem
     * a secretaria precisar clicar em cada um.
     */
    @Transactional
    public int autoBloquearVencidas(int diasTolerancia) {
        LocalDate limite = LocalDate.now().minusDays(diasTolerancia);
        List<Assinatura> vencidas = repository.buscarAtivasVencidasAntesDe(limite);
        vencidas.forEach(Assinatura::marcarInadimplente);
        return vencidas.size();
    }

    /**
     * O job diario de lembretes: um por estagio da regua (vence em breve,
     * vencida, inadimplente), nunca repetido — a secretaria nao precisa
     * mais avisar cada aluno na mao.
     *
     * Sem WhatsApp nem SMTP integrado no projeto (mesmo caso da cobranca):
     * o envio e simulado, so um registro do que teria saido e por qual
     * canal.
     */
    @Transactional
    public int gerarLembretes(int diasVenceEmBreve) {
        LocalDate hoje = LocalDate.now();
        LocalDate limiteDaJanela = hoje.plusDays(diasVenceEmBreve);

        int venceEmBreve = registrarLembretes(
                repository.buscarAtivasVencendoEntre(hoje, limiteDaJanela), EstagioLembrete.VENCE_EM_BREVE);
        int vencidas = registrarLembretes(
                repository.buscarAtivasVencidasAntesDe(hoje), EstagioLembrete.VENCIDA);
        int inadimplentes = registrarLembretes(
                repository.findByStatus(StatusAssinatura.INADIMPLENTE), EstagioLembrete.INADIMPLENTE);

        return venceEmBreve + vencidas + inadimplentes;
    }

    /**
     * Grava um lembrete por assinatura da lista, pulando quem ja tem um
     * deste estagio — e o que garante "um por estagio, nunca repetido"
     * mesmo o job rodando todo santo dia.
     *
     * Canal por telefone quando ha um cadastrado, senao e-mail: e o
     * contato mais direto de academia, e todo aluno tem pelo menos um
     * dos dois (cadastro exige e-mail sempre).
     */
    private int registrarLembretes(List<Assinatura> candidatas, EstagioLembrete estagio) {
        int criados = 0;
        for (Assinatura assinatura : candidatas) {
            if (lembreteRepository.existsByAssinaturaIdAndEstagio(assinatura.getId(), estagio)) {
                continue;
            }

            Usuario aluno = assinatura.getAluno();
            String telefone = aluno.getTelefone();
            boolean temTelefone = telefone != null && !telefone.isBlank();

            LembreteEnviado lembrete = new LembreteEnviado();
            lembrete.setAssinatura(assinatura);
            lembrete.setEstagio(estagio);
            lembrete.setCanal(temTelefone ? CanalLembrete.WHATSAPP : CanalLembrete.EMAIL);
            lembrete.setDestinatario(temTelefone ? telefone : aluno.getEmail());
            lembreteRepository.save(lembrete);
            criados++;
        }
        return criados;
    }

    /** Extrato de lembretes (simulados) da assinatura, mais recente primeiro. */
    @Transactional(readOnly = true)
    public List<LembreteDtos.Response> historicoLembretes(Long assinaturaId) {
        carregar(assinaturaId); // 404 antes de devolver historico vazio de id inexistente
        return lembreteRepository.findByAssinaturaIdOrderByDataEnvioDesc(assinaturaId).stream()
                .map(LembreteDtos.Response::de)
                .toList();
    }

    /**
     * Gera a cobranca do ciclo corrente da assinatura, com o codigo
     * simulado que a tela mostra no lugar de um boleto/PIX de verdade.
     */
    private void criarCobranca(Assinatura assinatura) {
        Cobranca cobranca = new Cobranca();
        cobranca.setAssinatura(assinatura);
        cobranca.setValor(assinatura.getPlano().getValorMensal());
        cobranca.setFormaPagamento(assinatura.getFormaPagamento());
        cobranca.setDataVencimento(assinatura.getDataVencimento());
        cobranca.setCodigoSimulado(gerarCodigoSimulado(assinatura.getFormaPagamento()));
        cobrancaRepository.save(cobranca);
    }

    /** Cartao nao tem "copia e cola" — so boleto e PIX mostram um codigo na tela. */
    private String gerarCodigoSimulado(FormaPagamento formaPagamento) {
        if (formaPagamento == FormaPagamento.CARTAO) {
            return null;
        }
        String sufixo = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return "%s-SIMULADO-%s".formatted(formaPagamento.name(), sufixo);
    }

    /**
     * Este aluno pode treinar nesta unidade hoje?
     *
     * E a pergunta que a recepcao faz na catraca, e a razao de as tres
     * tabelas existirem. O veredito devolve o motivo: barrar sem dizer
     * por que obriga a abrir a ficha do aluno para descobrir se e atraso,
     * vencimento ou plano que nao cobre a unidade — tres encaminhamentos
     * diferentes.
     *
     * A ordem das verificacoes e deliberada: primeiro o que o aluno deve
     * (atraso, vencimento), que a recepcao resolve no balcao, e so depois
     * a cobertura do plano, que e conversa de troca de plano.
     */
    @Transactional
    public AssinaturaDtos.Acesso conferirAcesso(Long alunoId, Long unidadeId) {
        Usuario aluno = carregarAluno(alunoId);
        Unidade unidade = unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Unidade", unidadeId));
        Assinatura assinatura = repository.buscarVigentePorAluno(alunoId).orElse(null);

        AssinaturaDtos.Acesso veredito = montarVeredito(aluno, unidade, assinatura, unidadeId);
        registrarCheckin(aluno, unidade, assinatura, veredito);
        return veredito;
    }

    private AssinaturaDtos.Acesso montarVeredito(Usuario aluno, Unidade unidade, Assinatura assinatura, Long unidadeId) {
        if (assinatura == null) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.SEM_MATRICULA,
                    "%s nao tem matricula vigente.".formatted(aluno.getNome()), null);
        }

        LocalDate hoje = LocalDate.now();
        AssinaturaDtos.Response resumo = AssinaturaDtos.Response.de(assinatura, hoje);

        if (assinatura.getStatus() == StatusAssinatura.INADIMPLENTE) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.INADIMPLENTE,
                    "Matricula em atraso de pagamento.", resumo);
        }
        if (assinatura.estaVencidaEm(hoje)) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.VENCIDA,
                    "Matricula vencida em %s.".formatted(assinatura.getDataVencimento().format(DATA_BR)), resumo);
        }
        if (!assinatura.getPlano().daAcessoA(unidadeId)) {
            return new AssinaturaDtos.Acesso(false, MotivoAcesso.UNIDADE_NAO_COBERTA,
                    "O plano \"%s\" nao cobre a unidade %s."
                            .formatted(assinatura.getPlano().getNome(), unidade.getNome()), resumo);
        }

        return new AssinaturaDtos.Acesso(true, MotivoAcesso.LIBERADO,
                "Acesso liberado ate %s.".formatted(assinatura.getDataVencimento().format(DATA_BR)), resumo);
    }

    /**
     * O historico de frequencia nasce aqui: cada pergunta da catraca vira
     * um registro, liberado ou barrado. Barrado tambem e frequencia — e o
     * que explica pra secretaria por que o aluno reclamou na porta.
     */
    private void registrarCheckin(Usuario aluno, Unidade unidade, Assinatura assinatura, AssinaturaDtos.Acesso veredito) {
        Checkin checkin = new Checkin();
        checkin.setAluno(aluno);
        checkin.setUnidade(unidade);
        checkin.setAssinatura(assinatura);
        checkin.setLiberado(veredito.liberado());
        checkin.setMotivo(veredito.motivo());
        checkinRepository.save(checkin);
    }

    /** Historico de frequencia do aluno, mais recente primeiro. */
    @Transactional(readOnly = true)
    public Page<CheckinDtos.Response> historicoCheckins(Long alunoId, Pageable pageable) {
        carregarAluno(alunoId); // 404 antes de devolver historico vazio de id inexistente
        return checkinRepository.findByAlunoId(alunoId, pageable)
                .map(CheckinDtos.Response::de);
    }

    /**
     * Espelha no servico a coerencia que o banco exige por CHECK: parceiro
     * sem token nao da para conferir, e token em matricula direta nao
     * significa nada. A mensagem sai como 409 legivel, em vez de a
     * violacao de constraint virar 500.
     */
    private String normalizarToken(OrigemAssinatura origem, String informado) {
        String token = (informado == null || informado.isBlank()) ? null : informado.trim();

        if (origem.exigeTokenParceiro() && token == null) {
            throw new RegraNegocioException(
                    "Matricula via %s exige o codigo do aluno no parceiro.".formatted(origem));
        }
        if (!origem.exigeTokenParceiro() && token != null) {
            throw new RegraNegocioException("Matricula direta nao tem codigo de parceiro.");
        }
        if (token != null && repository.existsByTokenParceiroAndStatusNot(token, StatusAssinatura.CANCELADA)) {
            throw new RegraNegocioException(
                    "Este codigo de parceiro ja esta em uso por outra matricula vigente.");
        }
        return token;
    }

    /**
     * Resolve quem indicou, quando a origem e INDICACAO.
     *
     * A coerencia origem/indicador ja e checada no DTO (`isIndicadorCoerente`)
     * — o que falta aqui e o que so o banco sabe: o indicador existe, e um
     * aluno de verdade, e nao e o proprio aluno se indicando.
     */
    private Usuario normalizarIndicador(OrigemAssinatura origem, Long indicadoPorAlunoId, Usuario aluno) {
        if (!origem.exigeIndicador()) {
            return null;
        }

        if (indicadoPorAlunoId.equals(aluno.getId())) {
            throw new RegraNegocioException("Um aluno nao pode se indicar a si mesmo.");
        }

        Usuario indicador = usuarioRepository.findById(indicadoPorAlunoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno que indicou", indicadoPorAlunoId));
        if (indicador.getTipoPerfil() != TipoPerfil.ALUNO) {
            throw new RegraNegocioException(
                    "\"%s\" nao esta cadastrado como aluno e nao pode ter indicado ninguem."
                            .formatted(indicador.getNome()));
        }
        return indicador;
    }

    private Assinatura carregar(Long id) {
        return repository.findWithAlunoAndPlanoById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Assinatura", id));
    }

    private Usuario carregarAluno(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aluno", id));
    }

    /** "" e null contam a mesma coisa: nenhum comentario foi deixado. */
    private String vazioComoNulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }
}
