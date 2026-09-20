package br.com.heracles.heracles_api.matriculas.domain;

/**
 * De onde vem a matricula.
 *
 * Parceiro (Gympass/TotalPass) traz um identificador do aluno la, que e o
 * que permite conferir o acesso; matricula direta nao tem token nenhum.
 * Indicacao traz outro aluno, nao um token — quem trouxe o novo aluno.
 */
public enum OrigemAssinatura {

    DIRETO,
    GYMPASS,
    TOTALPASS,
    INDICACAO;

    public boolean exigeTokenParceiro() {
        return this == GYMPASS || this == TOTALPASS;
    }

    public boolean exigeIndicador() {
        return this == INDICACAO;
    }
}
