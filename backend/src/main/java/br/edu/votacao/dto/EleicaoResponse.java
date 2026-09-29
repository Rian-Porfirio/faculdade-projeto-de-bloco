package br.edu.votacao.dto;

import br.edu.votacao.domain.StatusEleicao;
import java.time.LocalDateTime;

public record EleicaoResponse(Long id, String nome, String descricao, LocalDateTime dataInicio,
                              LocalDateTime dataTermino, StatusEleicao status) {
}
