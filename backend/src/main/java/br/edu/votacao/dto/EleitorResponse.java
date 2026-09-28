package br.edu.votacao.dto;

public record EleitorResponse(Long id, String nome, String identificador, String estado, String cidade,
                              Long localVotacaoId, String localVotacaoNome) {
}
