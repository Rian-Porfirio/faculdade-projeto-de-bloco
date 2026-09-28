package br.edu.votacao.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eleicao")
@Getter
@Setter
@NoArgsConstructor
public class Eleicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataInicio;

    @Column(nullable = false)
    private LocalDateTime dataTermino;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEleicao status;

    public boolean estaAtiva() {
        return status == StatusEleicao.ATIVA;
    }
}
