package br.edu.biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Table(name = "membro")
@Audited
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Membro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro;

    @Embedded
    private Endereco endereco;

    @NotAudited
    @OneToMany(mappedBy = "membro", fetch = FetchType.LAZY)
    private List<Emprestimo> emprestimos = new ArrayList<>();

    public Membro(String nome, String email, LocalDate dataCadastro, Endereco endereco) {
        this.nome = nome;
        this.email = email;
        this.dataCadastro = dataCadastro;
        this.endereco = endereco;
    }
}
