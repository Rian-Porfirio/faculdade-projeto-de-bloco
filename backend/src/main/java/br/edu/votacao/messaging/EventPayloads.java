package br.edu.votacao.messaging;

import java.time.Instant;

/** Payloads (campo "data") dos eventos publicados. Dados denormalizados evitam que consumidores chamem a API. */
public final class EventPayloads {
    private EventPayloads() {
    }

    public record VotoRegistrado(Long votoId, Long eleitorId, String estadoEleitor, Long candidatoId,
                                 String candidatoNome, Integer candidatoNumero, String cargo, String partidoSigla,
                                 Long eleicaoId, String eleicaoNome, Long localVotacaoId, Instant dataHora) {
    }

    /** Usado por CandidatoCadastrado e CandidatoAtualizado. */
    public record Candidato(Long candidatoId, String nome, Integer numero, String cargo, String partidoSigla,
                            Long eleicaoId, String eleicaoNome, String estado, String cidade) {
    }

    public record CandidatoRemovido(Long candidatoId, Long eleicaoId) {
    }

    /** Usado por EleicaoIniciada e EleicaoFinalizada. */
    public record Eleicao(Long eleicaoId, String nome, String status) {
    }
}
