package br.com.heracles.heracles_api.operacoes.dto;

import java.time.LocalDateTime;

/** Projecao interna: a data da venda mais recente de um produto, para o relatorio de produtos parados. */
public record LinhaUltimaVendaProduto(Long produtoId, LocalDateTime ultimaVenda) {
}
