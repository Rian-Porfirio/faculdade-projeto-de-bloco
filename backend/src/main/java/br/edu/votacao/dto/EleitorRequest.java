package br.edu.votacao.dto;

import jakarta.validation.constraints.*;

public record EleitorRequest(
        @NotBlank String nome,
        @NotBlank @Size(max = 20) String identificador,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ter 2 letras (UF)") String estado,
        @NotBlank String cidade,
        @NotNull Long localVotacaoId) {
}
