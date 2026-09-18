package br.com.heracles.heracles_api.core.domain;

import br.com.heracles.heracles_api.matriculas.domain.Plano;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios", schema = "core")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cpf;
    private String email;
    private String telefone;

    // --- Daqui pra baixo, so o aluno usa de fato. Professor, secretaria e
    //     admin precisam so do que ja esta acima para uma conta de acesso. ---

    private String endereco;
    private String cep;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    /**
     * Guardada como bytes no proprio banco — ver a migracao V12 para o porque.
     * Sem @Lob de proposito: no Hibernate 7, byte[] com @Lob mapeia para
     * "oid" (large object via referencia), e a coluna e "bytea" comum —
     * o mapeamento sem @Lob e o que bate com o tipo da coluna.
     */
    private byte[] foto;

    @Column(name = "foto_content_type")
    private String fotoContentType;

    /**
     * So a escolha do aluno na recepcao, sem virar assinatura. O plano
     * efetivo (o que da acesso de fato) nasce em matriculas.assinaturas
     * quando a secretaria confirma pela tela de Matriculas — as duas
     * coisas podem divergir por um tempo, de proposito.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_escolhido_id")
    private Plano planoEscolhido;

    // @JsonIgnore e redundante agora que os controllers so falam em DTOs,
    // mas fica como segunda barreira caso alguem volte a serializar a entidade.
    @JsonIgnore
    @Column(name = "senha_hash")
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil")
    private TipoPerfil tipoPerfil;

    @Column(name = "data_cadastro", updatable = false)
    private LocalDateTime dataCadastro;

    @Enumerated(EnumType.STRING)
    private StatusUsuario status;

    // LAZY: a listagem de alunos nao precisa dos treinos, e quando precisa
    // o repositorio usa @EntityGraph para trazer tudo numa consulta so.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_treinos",
            schema = "core",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "treino_id")
    )
    private List<Treino> treinos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.dataCadastro == null) this.dataCadastro = LocalDateTime.now();
        if (this.status == null) this.status = StatusUsuario.ATIVO;
    }

    public void alternarStatus() {
        this.status = (this.status == StatusUsuario.ATIVO)
                ? StatusUsuario.INATIVO
                : StatusUsuario.ATIVO;
    }

    public boolean isAtivo() {
        return this.status == StatusUsuario.ATIVO;
    }

    // equals/hashCode por identidade persistente: entidades novas (id nulo)
    // nunca sao "iguais", o que evita colisoes dentro de colecoes.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario outro)) return false;
        return id != null && id.equals(outro.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
