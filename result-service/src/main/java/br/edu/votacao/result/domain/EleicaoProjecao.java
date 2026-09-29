package br.edu.votacao.result.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cópia local (read model) mínima de uma eleição, alimentada por eventos. */
@Entity
@Table(name = "eleicao_projecao")
@Getter
@Setter
@NoArgsConstructor
public class EleicaoProjecao {
    @Id
    private Long id;

    @Column(nullable = false)
    private String nome;

    /** ATIVA, ENCERRADA ou DESCONHECIDA (quando só conhecemos a eleição por eventos de candidato/voto). */
    @Column(nullable = false, length = 20)
    private String status;
}
