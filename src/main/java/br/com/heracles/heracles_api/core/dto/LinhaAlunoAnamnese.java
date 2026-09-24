package br.com.heracles.heracles_api.core.dto;

/** Projecao de um aluno com matricula vigente e se ele tem anamnese preenchida. */
public record LinhaAlunoAnamnese(Long alunoId, boolean temAnamnese) {
}
