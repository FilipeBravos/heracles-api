package br.com.heracles.heracles_api.core.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios", schema = "core")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private String nome;
    private String cpf;
    private String email;
    private String telefone;

    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil")
    private TipoPerfil tipoPerfil;

    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro;

    @Enumerated(EnumType.STRING)
    private StatusUsuario status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_treinos",
            schema = "core",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "treino_id")
    )
    private List<Treino> treinos = new ArrayList<>();

    // Essa mágica preenche a data e o status automaticamente antes de salvar no banco
    @PrePersist
    public void prePersist() {
        if (this.dataCadastro == null) this.dataCadastro = LocalDateTime.now();
        if (this.status == null) this.status = StatusUsuario.ATIVO;
    }


}
