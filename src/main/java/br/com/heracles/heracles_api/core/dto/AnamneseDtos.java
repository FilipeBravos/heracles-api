package br.com.heracles.heracles_api.core.dto;

import br.com.heracles.heracles_api.core.domain.Anamnese;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AnamneseDtos {

    private AnamneseDtos() {
    }

    public record Salvar(
            @NotBlank(message = "Informe o objetivo do aluno")
            @Size(max = 500, message = "O objetivo deve ter no maximo 500 caracteres")
            String objetivo,

            String condicoesSaude,
            String lesoesCirurgias,
            String medicamentosUso,
            String restricoesMedicas,

            @NotBlank(message = "Informe o nome do contato de emergencia")
            @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres")
            String contatoEmergenciaNome,

            @NotBlank(message = "Informe o telefone do contato de emergencia")
            @Size(max = 20, message = "O telefone deve ter no maximo 20 caracteres")
            String contatoEmergenciaTelefone
    ) {
    }

    /**
     * `preenchida` sai sempre true aqui: so existe Response quando ha uma
     * Anamnese salva. Quem nao tem anamnese recebe Ausente, nao um Response
     * com campos nulos.
     */
    public record Response(
            boolean preenchida,
            String objetivo,
            String condicoesSaude,
            String lesoesCirurgias,
            String medicamentosUso,
            String restricoesMedicas,
            String contatoEmergenciaNome,
            String contatoEmergenciaTelefone,
            LocalDateTime preenchidaEm
    ) {
        public static Response de(Anamnese anamnese) {
            return new Response(
                    true,
                    anamnese.getObjetivo(),
                    anamnese.getCondicoesSaude(),
                    anamnese.getLesoesCirurgias(),
                    anamnese.getMedicamentosUso(),
                    anamnese.getRestricoesMedicas(),
                    anamnese.getContatoEmergenciaNome(),
                    anamnese.getContatoEmergenciaTelefone(),
                    anamnese.getPreenchidaEm()
            );
        }

        /** Estado normal para aluno recem-cadastrado: nao e erro, e "ainda nao". */
        public static Response ausente() {
            return new Response(false, null, null, null, null, null, null, null, null);
        }
    }
}
