package br.edu.votacao.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Voto de um eleitor em um candidato numa eleição.
 * A constraint única (eleitor, eleição) garante no banco a regra "um voto por eleição",
 * mesmo em caso de requisições concorrentes.
 */
@Entity
@Table(name = "voto", uniqueConstraints = @UniqueConstraint(name = "uk_voto_eleitor_eleicao",
        columnNames = {"eleitor_id", "eleicao_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Voto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleitor_id")
    private Eleitor eleitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidato_id")
    private Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "eleicao_id")
    private Eleicao eleicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "local_votacao_id")
    private LocalVotacao localVotacao;

    @Column(nullable = false)
    private Instant dataHora;
}
