package br.edu.votacao.result.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Apenas os campos de cada payload que este serviço usa. */
public final class EventData {
    private EventData() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VotoRegistrado(Long candidatoId, String candidatoNome, Integer candidatoNumero, String cargo,
                                 String partidoSigla, Long eleicaoId, String eleicaoNome, String estadoEleitor) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidato(Long candidatoId, String nome, Integer numero, String cargo, String partidoSigla,
                            Long eleicaoId, String eleicaoNome, String estado, String cidade) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidatoRemovido(Long candidatoId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Eleicao(Long eleicaoId, String nome, String status) {
    }
}
