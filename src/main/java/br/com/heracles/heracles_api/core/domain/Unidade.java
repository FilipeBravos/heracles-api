package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "unidades", schema = "core")
@Getter
@Setter
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoUnidade tipo;

    private String endereco;
    private String telefone;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Unidade outra)) return false;
        return id != null && id.equals(outra.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
