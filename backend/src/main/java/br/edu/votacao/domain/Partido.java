package br.edu.votacao.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "partido")
@Getter
@Setter
@NoArgsConstructor
public class Partido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String sigla;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer numero;
}
