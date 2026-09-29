package br.edu.votacao.dto;

import br.edu.votacao.domain.StatusEleicao;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record EleicaoRequest(
        @NotBlank String nome,
        @Size(max = 500) String descricao,
        @NotNull LocalDateTime dataInicio,
        @NotNull LocalDateTime dataTermino,
        @NotNull StatusEleicao status) {
}
