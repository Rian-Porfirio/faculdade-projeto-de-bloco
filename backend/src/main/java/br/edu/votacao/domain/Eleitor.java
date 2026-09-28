package br.edu.votacao.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eleitor")
@Getter
@Setter
@NoArgsConstructor
public class Eleitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** Identificador único do eleitor (simulação do título de eleitor). */
    @Column(nullable = false, unique = true, length = 20)
    private String identificador;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(nullable = false)
    private String cidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_votacao_id")
    private LocalVotacao localVotacao;
}
