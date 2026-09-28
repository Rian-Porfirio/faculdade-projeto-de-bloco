package br.edu.votacao.dto;

import br.edu.votacao.domain.Cargo;
import jakarta.validation.constraints.*;

public record CandidatoRequest(
        @NotBlank String nome,
        @NotNull @Min(1) @Max(99999) Integer numero,
        @NotNull Cargo cargo,
        @NotNull Long partidoId,
        @NotNull Long eleicaoId,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ter 2 letras (UF)") String estado,
        @NotBlank String cidade) {
}
