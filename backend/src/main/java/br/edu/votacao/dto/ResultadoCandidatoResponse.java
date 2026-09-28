package br.edu.votacao.dto;

import br.edu.votacao.domain.Cargo;

public record ResultadoCandidatoResponse(Long candidatoId, String nome, Integer numero, String partidoSigla,
                                         Cargo cargo, long votos, double percentual) {
}
