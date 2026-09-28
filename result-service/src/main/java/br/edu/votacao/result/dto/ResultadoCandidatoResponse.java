package br.edu.votacao.result.dto;

public record ResultadoCandidatoResponse(Long candidatoId, String nome, Integer numero, String partidoSigla,
                                         String cargo, long votos, double percentual) {
}
