package br.edu.votacao.result.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidato_projecao")
@Getter
@Setter
@NoArgsConstructor
public class CandidatoProjecao {
    /** Mesmo id do candidato no voting-service (não é gerado aqui). */
    @Id
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 30)
    private String cargo;

    @Column(nullable = false, length = 10)
    private String partidoSigla;

    @Column(nullable = false)
    private Long eleicaoId;

    private String estado;
    private String cidade;
}
