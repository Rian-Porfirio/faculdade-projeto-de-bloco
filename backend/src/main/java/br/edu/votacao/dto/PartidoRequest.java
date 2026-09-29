package br.edu.votacao.dto;

import jakarta.validation.constraints.*;

public record PartidoRequest(
        @NotBlank @Size(max = 10) String sigla,
        @NotBlank String nome,
        @NotNull @Min(1) @Max(99) Integer numero) {
}
