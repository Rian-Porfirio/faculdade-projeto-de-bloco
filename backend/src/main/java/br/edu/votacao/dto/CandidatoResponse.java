package br.edu.votacao.dto;

import br.edu.votacao.domain.Cargo;
import br.edu.votacao.domain.Regiao;

public record CandidatoResponse(Long id, String nome, Integer numero, Cargo cargo, Long partidoId,
                                String partidoSigla, Long eleicaoId, String estado, String cidade, Regiao regiao) {
}
