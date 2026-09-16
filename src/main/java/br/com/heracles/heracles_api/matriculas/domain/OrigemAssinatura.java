package br.com.heracles.heracles_api.matriculas.domain;

/**
 * De onde vem a matricula.
 *
 * Parceiro (Gympass/TotalPass) traz um identificador do aluno la, que e o
 * que permite conferir o acesso; matricula direta nao tem token nenhum.
 */
public enum OrigemAssinatura {

    DIRETO,
    GYMPASS,
    TOTALPASS;

    public boolean exigeTokenParceiro() {
        return this != DIRETO;
    }
}
