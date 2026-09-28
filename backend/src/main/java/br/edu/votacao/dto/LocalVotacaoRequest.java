package br.edu.votacao.dto;

import jakarta.validation.constraints.*;

public record LocalVotacaoRequest(
        @NotBlank String nome,
        @NotBlank String cidade,
        @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ter 2 letras (UF)") String estado,
        @NotBlank @Size(max = 10) String zona) {
}
