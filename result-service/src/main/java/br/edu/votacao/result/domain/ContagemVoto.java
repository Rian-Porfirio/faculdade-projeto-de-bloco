package br.edu.votacao.result.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Votos de um candidato vindos de eleitores de um estado. A soma por candidato dá o total. */
@Entity
@Table(name = "contagem_voto", uniqueConstraints = @UniqueConstraint(name = "uk_contagem_candidato_estado",
        columnNames = {"candidato_id", "estado"}))
@Getter
@Setter
@NoArgsConstructor
public class ContagemVoto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidato_id", nullable = false)
    private Long candidatoId;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(nullable = false)
    private long total;
}
