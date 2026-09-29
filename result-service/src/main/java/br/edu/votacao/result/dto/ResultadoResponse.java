package br.edu.votacao.result.dto;

import java.util.List;

/** Mesmo formato do voting-service, com "fonte" indicando que veio da projeção baseada em eventos. */
public record ResultadoResponse(Long eleicaoId, String eleicaoNome, String cargo, String estado, long totalVotos,
                                List<ResultadoCandidatoResponse> candidatos, String fonte) {
}
