package br.edu.votacao.dto;

import br.edu.votacao.domain.Cargo;
import java.util.List;

/** cargo e estado são os filtros aplicados (null = sem filtro). */
public record ResultadoResponse(Long eleicaoId, String eleicaoNome, Cargo cargo, String estado,
                                long totalVotos, List<ResultadoCandidatoResponse> candidatos) {
}
