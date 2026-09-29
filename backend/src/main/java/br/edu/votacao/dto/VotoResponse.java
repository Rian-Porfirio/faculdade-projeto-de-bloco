package br.edu.votacao.dto;

import java.time.Instant;

public record VotoResponse(Long id, Long eleitorId, String eleitorNome, Long candidatoId, String candidatoNome,
                           Integer candidatoNumero, Long eleicaoId, Long localVotacaoId, Instant dataHora) {
}
