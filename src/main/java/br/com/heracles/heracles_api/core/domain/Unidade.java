package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "unidades", schema = "core")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoUnidade tipo;

    private String endereco;
    private String telefone;
}
