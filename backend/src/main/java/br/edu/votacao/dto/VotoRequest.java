package br.edu.votacao.dto;

import jakarta.validation.constraints.NotNull;

/** localVotacaoId é opcional: se ausente, usa o local de votação cadastrado do eleitor. */
public record VotoRequest(
        @NotNull Long eleitorId,
        @NotNull Long candidatoId,
        @NotNull Long eleicaoId,
        Long localVotacaoId) {
}
